import os
import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor
from threading import Barrier
from uuid import uuid4
from unittest.mock import Mock, patch


TEST_DIRECTORY = tempfile.TemporaryDirectory()
os.environ['SECRET_KEY'] = 'test-secret-key'
os.environ['DATABASE_PATH'] = os.path.join(TEST_DIRECTORY.name, 'test.db')

from files.app import (  # noqa: E402
    Game,
    GamePlatformOwnership,
    User,
    app,
    db,
    get_preferred_age_rating,
)
from sqlalchemy.exc import SQLAlchemyError
from files.models import GameWriteReceipt
from files.database import commit_changes as real_commit_changes
from files.config import PLATFORMS  # noqa: E402


class GamesCollectionTestCase(unittest.TestCase):
    @classmethod
    def tearDownClass(cls):
        with app.app_context():
            db.session.remove()
            db.drop_all()
        TEST_DIRECTORY.cleanup()

    def setUp(self):
        app.config.update(TESTING=True, WTF_CSRF_ENABLED=False)
        with app.app_context():
            db.drop_all()
            db.create_all()
        self.client = app.test_client()

    def register(self, username='Tester', password='password1'):
        return self.client.post('/register', data={
            'username': username,
            'password': password,
            'password_confirm': password,
        })

    def add_game(self, title='Testspiel', igdb_id=123):
        return self.client.post('/api/games', json={
            'title': title,
            'owned_platform': 'Steam',
            'format': 'Digital',
            'status': 'Nicht gespielt',
            'rating': '8',
            'igdb_id': igdb_id,
            'igdb_url': f'https://www.igdb.com/games/{title.lower()}',
            'cover_url': 'https://images.igdb.com/igdb/image/upload/test.jpg',
        })

    def test_igdb_token_requires_login_csrf_and_is_not_cached(self):
        self.assertEqual(self.client.post('/api/igdb-token', json={}).status_code, 401)
        self.register()
        app.config['WTF_CSRF_ENABLED'] = True
        self.assertEqual(self.client.post('/api/igdb-token', json={}).status_code, 400)
        csrf = self.client.get('/api/session').json['csrf_token']
        with patch('files.routes.session.get_igdb_credentials', return_value={
                'client_id': 'test-client', 'access_token': 'test-token', 'expires_in': 3600}):
            response = self.client.post('/api/igdb-token', json={}, headers={'X-CSRFToken': csrf})
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.headers['Cache-Control'], 'no-store')
            self.assertEqual(set(response.json), {'client_id', 'access_token', 'expires_in'})
            self.assertEqual(self.client.post('/api/igdb-token', json={'unexpected': True},
                headers={'X-CSRFToken': csrf}).status_code, 400)

    def test_igdb_credentials_cache_lifetime_and_secret_transport(self):
        import files.igdb as igdb
        with app.app_context(), patch.dict(app.config, TWITCH_CLIENT_ID='test-client', TWITCH_CLIENT_SECRET='test-secret'), \
                patch.object(igdb, '_token', None), patch.object(igdb, '_token_expires', 0), \
                patch.object(igdb, '_token_owner', None), patch('files.igdb.time.time', return_value=1000), \
                patch('files.igdb.requests.post') as post:
            post.return_value.json.return_value = {'access_token': 'test-token', 'expires_in': 3600}
            first = igdb.get_igdb_credentials()
            self.assertEqual(first['expires_in'], 3600)
            self.assertEqual(first, igdb.get_igdb_credentials())
            self.assertEqual(post.call_count, 1)
            self.assertNotIn('test-secret', post.call_args.args[0])
            self.assertEqual(post.call_args.kwargs['data']['client_secret'], 'test-secret')
            with patch('files.igdb.requests.get') as get:
                get.return_value.status_code = 401
                post.return_value.json.return_value = {'access_token': 'replacement', 'expires_in': 7200}
                self.assertEqual(igdb.get_igdb_credentials(verify=True)['access_token'], 'replacement')
                self.assertEqual(post.call_count, 2)

    def test_registration_is_case_insensitive_and_first_user_is_admin(self):
        self.assertEqual(self.register('Tester').status_code, 302)
        with app.app_context():
            self.assertTrue(User.query.one().is_admin)
        self.client.post('/logout')
        response = self.register('tester', 'password2')
        self.assertEqual(response.status_code, 200)
        self.assertIn('Benutzername existiert bereits', response.get_data(as_text=True))

    def test_login_remember_me_is_a_checkbox(self):
        response = self.client.get('/login')
        self.assertEqual(response.status_code, 200)
        self.assertIn('type="checkbox"', response.get_data(as_text=True))
        self.assertIn('for="rememberCheck">Eingeloggt bleiben', response.get_data(as_text=True))

        stylesheet = self.client.get('/static/style.css')
        css = stylesheet.get_data(as_text=True)
        self.assertIn(':not([type="checkbox"])', css)
        self.assertIn('.form-check-input:checked', css)
        stylesheet.close()

    def test_native_session_login_logout_and_csrf_rotation(self):
        self.register()
        self.client.post('/logout')
        app.config['WTF_CSRF_ENABLED'] = True
        anonymous = self.client.get('/api/session')
        self.assertEqual(anonymous.headers['Cache-Control'], 'no-store')
        self.assertFalse(anonymous.json['authenticated'])
        token = anonymous.json['csrf_token']
        self.assertEqual(self.client.post('/api/session', json={
            'username': 'Tester', 'password': 'password1',
        }).status_code, 400)
        logged_in = self.client.post('/api/session', json={
            'username': 'tester', 'password': 'password1', 'remember': True,
        }, headers={'X-CSRFToken': token})
        self.assertEqual(logged_in.status_code, 200)
        self.assertTrue(logged_in.json['authenticated'])
        self.assertEqual(logged_in.json['user']['username'], 'Tester')
        self.assertNotIn('password', logged_in.json['user'])
        self.assertNotEqual(logged_in.json['csrf_token'], token)
        self.assertTrue(any('remember_token=' in cookie for cookie in logged_in.headers.getlist('Set-Cookie')))
        self.assertEqual(self.client.get('/api/games').status_code, 200)
        logged_out = self.client.delete('/api/session', headers={'X-CSRFToken': logged_in.json['csrf_token']})
        self.assertEqual(logged_out.status_code, 200)
        self.assertFalse(logged_out.json['authenticated'])
        self.assertIsNone(logged_out.json['user'])
        self.assertTrue(any('remember_token=;' in cookie for cookie in logged_out.headers.getlist('Set-Cookie')))
        self.assertEqual(self.client.get('/api/games').status_code, 401)

    def test_native_api_requires_authentication_and_isolates_collections(self):
        unauthorized = self.client.get('/api/games')
        self.assertEqual(unauthorized.status_code, 401)
        self.assertIsNotNone(unauthorized.json['error'])
        self.register('First')
        self.add_game('Private First', 11)
        self.client.post('/logout')
        self.register('Second')
        self.add_game('Private Second', 22)
        response = self.client.get('/api/games')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.headers['Cache-Control'], 'no-store')
        self.assertEqual([game['title'] for game in response.json['games']], ['Private Second'])
        self.assertEqual(response.json['platforms'], sorted(PLATFORMS))
        self.assertIn('version', response.json['games'][0])

    def test_native_https_login_keeps_strict_csrf_referrer_check(self):
        self.register()
        self.client.post('/logout')
        app.config['WTF_CSRF_ENABLED'] = True
        token = self.client.get('/api/session', base_url='https://localhost').json['csrf_token']
        payload = {'username': 'Tester', 'password': 'password1'}
        self.assertEqual(self.client.post('/api/session', base_url='https://localhost', json=payload,
                         headers={'X-CSRFToken': token}).status_code, 400)
        response = self.client.post('/api/session', base_url='https://localhost', json=payload,
                                    headers={'X-CSRFToken': token, 'Referer': 'https://localhost/'})
        self.assertEqual(response.status_code, 200)
        self.assertTrue(response.json['authenticated'])

    def test_native_login_rejects_invalid_json_and_bad_credentials(self):
        self.register()
        self.client.post('/logout')
        for payload in ([], {'username': ['Tester'], 'password': 'password1'},
                        {'username': 'Tester', 'password': 'password1', 'remember': 'false'},
                        {'username': 'Tester', 'password': 'password1', 'extra': True}):
            self.assertEqual(self.client.post('/api/session', json=payload).status_code, 400)
        for username, password in [('Missing', 'password1'), ('Tester', 'wrong')]:
            self.assertEqual(self.client.post('/api/session', json={
                'username': username, 'password': password,
            }).status_code, 401)
        self.assertFalse(self.client.get('/api/session').json['authenticated'])

    def test_csrf_rejects_state_change_without_token(self):
        app.config['WTF_CSRF_ENABLED'] = True
        response = self.client.post('/register', data={
            'username': 'Tester',
            'password': 'password1',
            'password_confirm': 'password1',
        })
        self.assertEqual(response.status_code, 400)

    def test_csrf_token_endpoint_and_api_marker(self):
        self.register()
        app.config['WTF_CSRF_ENABLED'] = True

        rejected = self.client.post('/api/games', json={})
        self.assertEqual(rejected.status_code, 400)
        self.assertEqual(rejected.headers.get('X-CSRF-Error'), '1')

        token_response = self.client.get('/api/csrf-token')
        self.assertEqual(token_response.status_code, 200)
        self.assertEqual(token_response.headers.get('Cache-Control'), 'no-store')
        token = token_response.get_json()['csrf_token']

        accepted = self.client.post('/api/games', json={
            'title': 'CSRF-Testspiel',
            'owned_platform': 'Steam',
            'format': 'Digital',
            'status': 'Gespielt',
            'rating': '7',
        }, headers={'X-CSRFToken': token})
        self.assertEqual(accepted.status_code, 201, accepted.get_json())

    def test_game_duplicate_stale_update_and_account_cascade_delete(self):
        self.register()
        first = self.add_game()
        self.assertEqual(first.status_code, 201, first.get_json())
        self.assertEqual(self.add_game().status_code, 409)

        with app.app_context():
            game = Game.query.one()
            game_id = game.id
            version = game.version

        payload = {
            'title': 'Geändert',
            'owned_platform': 'Steam',
            'format': 'Digital',
            'status': 'Gespielt',
            'rating': '9',
            'version': version,
        }
        updated = self.client.put(f'/api/games/{game_id}', json=payload)
        self.assertEqual(updated.status_code, 200, updated.get_json())
        self.assertEqual(self.client.put(f'/api/games/{game_id}', json=payload).status_code, 409)

        deleted = self.client.delete('/api/account', json={'current_password': 'password1'})
        self.assertEqual(deleted.status_code, 200, deleted.get_json())
        with app.app_context():
            self.assertEqual(User.query.count(), 0)
            self.assertEqual(Game.query.count(), 0)
            self.assertEqual(GamePlatformOwnership.query.count(), 0)

    def test_url_allowlist(self):
        self.register()
        response = self.client.post('/api/games', json={
            'title': 'Unsicher',
            'owned_platform': 'Steam',
            'format': 'Digital',
            'status': 'Nicht gespielt',
            'cover_url': 'javascript:alert(1)',
        })
        self.assertEqual(response.status_code, 400)

    def test_game_metadata_and_create_version_are_validated(self):
        self.register()
        base = {
            'title': 'Metadaten-Test',
            'owned_platform': 'Steam',
            'format': 'Digital',
            'status': 'Nicht gespielt',
        }
        for field, value in (
            ('release_date', 'kein-jahr'),
            ('release_date', '1899'),
            ('release_date', '2101'),
            ('age_rating', 'frei erfunden'),
            ('version', 99),
        ):
            response = self.client.post('/api/games', json={**base, field: value})
            self.assertEqual(response.status_code, 400, (field, value, response.get_json()))

        valid = self.client.post('/api/games', json={
            **base, 'release_date': '2026', 'age_rating': 'PEGI 18',
        })
        self.assertEqual(valid.status_code, 201, valid.get_json())

    def test_api_not_found_is_json(self):
        self.register()
        response = self.client.put('/api/games/999999', json={'title': 'Nicht vorhanden', 'version': 1})
        self.assertEqual(response.status_code, 404)
        self.assertEqual(response.content_type, 'application/json')
        self.assertIn('error', response.get_json())

    def test_age_rating_prefers_usk_then_pegi(self):
        ratings = [
            {'organization': {'name': 'PEGI'}, 'rating_category': {'rating': '16'}},
            {'organization': {'name': 'USK'}, 'rating_category': {'rating': '12'}},
        ]
        self.assertEqual(get_preferred_age_rating(ratings), 'USK 12')
        self.assertEqual(get_preferred_age_rating(ratings[:1]), 'PEGI 16')

    def test_classic_platforms_are_available(self):
        expected = {
            'Nintendo Entertainment System (NES)',
            'Super Nintendo Entertainment System (SNES)',
            'Nintendo 64 (N64)',
            'GameCube', 'Game Boy', 'Game Boy Color', 'Game Boy Advance',
            'Sega Game Gear', 'Sega Mega Drive', 'Sega Dreamcast',
            'Sega Mega CD', 'Sega Master System', 'Wii', 'Wii U',
        }
        self.assertTrue(expected.issubset(PLATFORMS))

    @patch('files.routes.games.requests.post')
    @patch('files.routes.games.get_igdb_token', return_value='test-token')
    def test_igdb_search_can_be_limited_by_release_year(self, _, post):
        self.register()
        app.config.update(TWITCH_CLIENT_ID='test-client', TWITCH_CLIENT_SECRET='test-secret')
        igdb_response = Mock()
        igdb_response.json.return_value = []
        post.return_value = igdb_response

        response = self.client.post('/api/search', json={'query': 'Zelda', 'release_year': '1998'})

        self.assertEqual(response.status_code, 200, response.get_json())
        request_body = post.call_args.kwargs['data']
        self.assertIn('search "Zelda";', request_body)
        self.assertIn('where first_release_date >= 883612800', request_body)
        self.assertIn('first_release_date < 915148800', request_body)

        invalid = self.client.post('/api/search', json={'query': 'Zelda', 'release_year': '1899'})
        self.assertEqual(invalid.status_code, 400)

    def test_index_loads_modular_frontend(self):
        response = self.client.post('/register', data={
            'username': 'Tester',
            'password': 'password1',
            'password_confirm': 'password1',
        }, follow_redirects=True)
        self.assertEqual(response.status_code, 200)
        self.assertIn('/static/js/main.js', response.get_data(as_text=True))
        self.assertIn('id="dekuDealsBtn"', response.get_data(as_text=True))
        self.assertIn('id="filterMinRating"', response.get_data(as_text=True))
        self.assertIn('id="releaseYearSearch"', response.get_data(as_text=True))
        self.assertIn('Nintendo Entertainment System (NES)', response.get_data(as_text=True))
        self.assertIn('rel="manifest"', response.get_data(as_text=True))
        self.assertIn('id="installAppBtn"', response.get_data(as_text=True))
        self.assertIn('account-logout-action', response.get_data(as_text=True))
        self.assertIn('id="logoutForm" class="d-none"', response.get_data(as_text=True))
        self.assertIn('aria-current="page"', response.get_data(as_text=True))
        self.assertIn('class="filter-panel-content"', response.get_data(as_text=True))
        static_response = self.client.get('/static/js/main.js')
        self.assertEqual(static_response.status_code, 200)
        static_response.close()
        games_script = self.client.get('/static/js/games.js')
        self.assertIn('https://www.dekudeals.com/search?q=', games_script.get_data(as_text=True))
        self.assertIn('if (id) data.version =', games_script.get_data(as_text=True))
        games_script.close()
        ui_script = self.client.get('/static/js/ui.js')
        self.assertIn("rating >= minimumRating", ui_script.get_data(as_text=True))
        self.assertIn("platform-icon-label", ui_script.get_data(as_text=True))
        self.assertIn("'Wii U': 'Wii U'", ui_script.get_data(as_text=True))
        self.assertIn('ownedPlatforms.includes(platform)', ui_script.get_data(as_text=True))
        ui_script.close()

        igdb_response = self.client.get('/static/js/igdb-search.js')
        igdb_script = igdb_response.get_data(as_text=True)
        self.assertIn('requestSequence', igdb_script)
        self.assertIn('activeController.signal.aborted', igdb_script)
        igdb_response.close()
        account_response = self.client.get('/static/js/account.js')
        account_script = account_response.get_data(as_text=True)
        self.assertIn('themeSaveQueue', account_script)
        self.assertIn('latestThemeRequest', account_script)
        account_response.close()

    def test_pwa_manifest_icons_and_service_worker(self):
        manifest_response = self.client.get('/static/manifest.webmanifest')
        self.assertEqual(manifest_response.status_code, 200)
        manifest = manifest_response.get_json()
        self.assertEqual(manifest['start_url'], '/')
        self.assertEqual(manifest['display'], 'standalone')
        self.assertEqual({icon['sizes'] for icon in manifest['icons']}, {'192x192', '512x512'})
        manifest_response.close()

        for path in (
            '/static/icons/app-icon-192.png',
            '/static/icons/app-icon-512.png',
            '/static/icons/app-icon-maskable-512.png',
            '/static/offline.html',
        ):
            asset = self.client.get(path)
            self.assertEqual(asset.status_code, 200, path)
            asset.close()

        worker_response = self.client.get('/service-worker.js')
        self.assertEqual(worker_response.status_code, 200)
        self.assertEqual(worker_response.headers.get('Service-Worker-Allowed'), '/')
        self.assertEqual(worker_response.headers.get('Cache-Control'), 'no-cache')
        worker = worker_response.get_data(as_text=True)
        self.assertIn("url.pathname.startsWith('/api/')", worker)
        self.assertNotIn("'/api/", worker.split('STATIC_ASSETS', 1)[1].split('];', 1)[0])
        worker_response.close()

    def test_theme_can_be_changed_and_is_rendered(self):
        self.register()
        response = self.client.put('/api/account/theme', json={'theme': 'editorial'})
        self.assertEqual(response.status_code, 200, response.get_json())
        with app.app_context():
            self.assertEqual(User.query.one().ui_theme, 'editorial')
        page = self.client.get('/')
        self.assertIn('data-ui-theme="editorial"', page.get_data(as_text=True))
        invalid = self.client.put('/api/account/theme', json={'theme': 'unknown'})
        self.assertEqual(invalid.status_code, 400)

    def manual_payload(self, title='Manuell'):
        return {'title': title, 'owned_platform': 'Steam', 'format': 'Digital',
                'status': 'Nicht gespielt', 'igdb_id': None}

    def test_idempotent_create_replays_after_lost_response_and_detects_key_reuse(self):
        self.register()
        key = {'Idempotency-Key': str(uuid4())}
        first = self.client.post('/api/games', json=self.manual_payload(), headers=key)
        retry = self.client.post('/api/games', json=self.manual_payload(), headers=key)
        self.assertEqual(first.status_code, 201)
        self.assertEqual(retry.status_code, 201)
        self.assertEqual(retry.headers.get('Idempotency-Replayed'), '1')
        self.assertEqual(first.json, retry.json)
        mismatch = self.client.post('/api/games', json=self.manual_payload('Anders'), headers=key)
        self.assertEqual(mismatch.status_code, 409)
        with app.app_context():
            self.assertEqual(Game.query.count(), 1)
            self.assertEqual(GameWriteReceipt.query.count(), 1)

    def test_idempotent_updates_and_deletes_replay_without_mutating_twice(self):
        self.register()
        self.add_game()
        game = self.client.get('/api/games').json['games'][0]
        update = {'version': game['version'], 'title': 'Geändert'}
        key = {'Idempotency-Key': str(uuid4())}
        for _ in range(2):
            self.assertEqual(self.client.put(f"/api/games/{game['id']}", json=update, headers=key).status_code, 200)
        with app.app_context():
            self.assertEqual(Game.query.one().version, game['version'] + 1)
        # A different operation must still honor optimistic version checks.
        self.assertEqual(self.client.put(f"/api/games/{game['id']}", json=update,
                         headers={'Idempotency-Key': str(uuid4())}).status_code, 409)
        key = {'Idempotency-Key': str(uuid4())}
        for _ in range(2):
            self.assertEqual(self.client.delete(f"/api/games/{game['id']}", json={}, headers=key).status_code, 200)
        with app.app_context():
            self.assertEqual(Game.query.count(), 0)
            self.assertEqual(GameWriteReceipt.query.count(), 2)

    def test_failed_idempotent_write_rolls_back_receipt_and_can_be_corrected(self):
        self.register()
        key = {'Idempotency-Key': str(uuid4())}
        self.assertEqual(self.client.post('/api/games', json={'title': 'Invalid'}, headers=key).status_code, 400)
        with app.app_context():
            self.assertEqual(GameWriteReceipt.query.count(), 0)
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload(), headers=key).status_code, 201)
        key = {'Idempotency-Key': str(uuid4())}
        with self.assertLogs(app.logger, level='ERROR'):
            with patch('sqlalchemy.orm.Session.commit', side_effect=SQLAlchemyError('test')):
                response = self.client.post('/api/games', json=self.manual_payload('Rollback'), headers=key)
        self.assertEqual(response.status_code, 503)
        with app.app_context():
            self.assertEqual(Game.query.count(), 1)
            self.assertEqual(GameWriteReceipt.query.count(), 1)
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload('Rollback'), headers=key).status_code, 201)

    def test_idempotency_is_user_scoped_and_receipts_follow_account_deletion(self):
        self.register('First')
        key = {'Idempotency-Key': str(uuid4())}
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload(), headers=key).status_code, 201)
        with app.app_context():
            first_id = User.query.filter_by(username='First').one().id
        self.client.post('/logout')
        self.register('Second')
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload(), headers=key).status_code, 201)
        games = self.client.get('/api/games').json['games']
        self.assertEqual(len(games), 1)
        with app.app_context():
            self.assertEqual(GameWriteReceipt.query.count(), 2)
            db.session.delete(db.session.get(User, first_id))
            db.session.commit()
            self.assertEqual(GameWriteReceipt.query.count(), 1)
            self.assertEqual(Game.query.count(), 1)

    def test_concurrent_idempotent_creates_commit_only_one_game_and_receipt(self):
        self.register()
        cookie = self.client.get_cookie('session').value
        key = {'Idempotency-Key': str(uuid4())}
        barrier = Barrier(2)

        def synchronized_commit(*args, **kwargs):
            barrier.wait(timeout=10)
            return real_commit_changes(*args, **kwargs)

        def create(_):
            client = app.test_client()
            client.set_cookie('session', cookie)
            return client.post('/api/games', json=self.manual_payload(), headers=key).status_code

        with patch('files.routes.games.commit_changes', side_effect=synchronized_commit):
            with ThreadPoolExecutor(max_workers=2) as pool:
                self.assertEqual(list(pool.map(create, range(2))), [201, 201])
        with app.app_context():
            self.assertEqual(Game.query.count(), 1)
            self.assertEqual(GameWriteReceipt.query.count(), 1)

    def test_idempotency_header_does_not_bypass_authentication_csrf_or_validation(self):
        key = {'Idempotency-Key': str(uuid4())}
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload(), headers=key).status_code, 401)
        self.register()
        for invalid in ['', 'not-a-uuid', str(uuid4()).upper()]:
            self.assertEqual(self.client.post('/api/games', json=self.manual_payload(),
                             headers={'Idempotency-Key': invalid}).status_code, 400)
        app.config['WTF_CSRF_ENABLED'] = True
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload(), headers=key).status_code, 400)
        token = self.client.get('/api/session').json['csrf_token']
        self.assertTrue(self.client.get('/api/session').json['capabilities']['idempotent_game_writes'])
        self.assertEqual(self.client.post('/api/games', json=self.manual_payload(),
                         headers={**key, 'X-CSRFToken': token}).status_code, 201)

    def test_adding_receipt_table_preserves_existing_collection(self):
        self.register()
        self.add_game()
        with app.app_context():
            GameWriteReceipt.__table__.drop(db.engine)
            db.create_all()
            self.assertEqual(Game.query.one().title, 'Testspiel')
            self.assertEqual(GameWriteReceipt.query.count(), 0)
        response = self.client.post('/api/games', json=self.manual_payload(),
                                    headers={'Idempotency-Key': str(uuid4())})
        self.assertEqual(response.status_code, 201)


if __name__ == '__main__':
    unittest.main()
