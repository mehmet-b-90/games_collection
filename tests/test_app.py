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

    def test_discovery_requires_login_and_never_changes_collection(self):
        self.assertEqual(self.client.get('/api/discover').status_code, 401)
        self.register()
        self.add_game()
        before = self.client.get('/api/games').json
        feed = {'games': [{'id': 123, 'name': 'Popular', 'popularity_rank': 1}],
                'updated_at': '2026-10-24T12:00:00+00:00', 'stale': False, 'source': 'igdb_visits'}
        with patch('files.routes.games.popular_games', return_value=feed) as upstream:
            response = self.client.get('/api/discover')
            self.assertEqual(response.status_code, 200)
            self.assertEqual(response.json, feed)
            self.assertEqual(response.headers['Cache-Control'], 'no-store')
            self.assertEqual(self.client.get('/api/discover?unexpected=1').status_code, 400)
            self.assertEqual(upstream.call_count, 1)
        self.assertEqual(self.client.get('/api/games').json, before)
        page = self.client.get('/').text
        self.assertIn('id="discover" hidden', page)
        self.assertIn('data-view="discover"', page)

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

    def test_concurrent_first_registrations_create_exactly_one_admin(self):
        from werkzeug.security import generate_password_hash
        barrier = Barrier(2)

        def synchronized_hash(password):
            result = generate_password_hash(password)
            barrier.wait(timeout=10)
            return result

        def register(username):
            return app.test_client().post('/register', data={
                'username': username, 'password': 'password1', 'password_confirm': 'password1',
            }).status_code

        with patch('files.routes.auth.generate_password_hash', side_effect=synchronized_hash):
            with ThreadPoolExecutor(max_workers=2) as pool:
                self.assertEqual(list(pool.map(register, ['First', 'Second'])), [302, 302])
        with app.app_context():
            self.assertEqual(User.query.count(), 2)
            self.assertEqual(User.query.filter_by(is_admin=True).count(), 1)

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

    def test_login_ignores_username_case_for_web_and_native_clients(self):
        self.assertEqual(self.register('StraßeÄ', 'CaseSensitive1').status_code, 302)
        self.client.post('/logout')
        for username in ('StraßeÄ', 'straßeä', 'STRASSEÄ', ' sTrAsSeä '):
            for native in (False, True):
                with self.subTest(username=username, native=native):
                    if native:
                        response = self.client.post('/api/session', json={
                            'username': username, 'password': 'CaseSensitive1',
                        })
                        self.assertEqual(response.status_code, 200)
                    else:
                        response = self.client.post('/login', data={
                            'username': username, 'password': 'CaseSensitive1',
                        })
                        self.assertEqual(response.status_code, 302)
                    session = self.client.get('/api/session').json
                    self.assertTrue(session['authenticated'])
                    self.assertEqual(session['user']['username'], 'StraßeÄ')
                    self.client.post('/logout')

        for native in (False, True):
            with self.subTest(wrong_password_case=True, native=native):
                credentials = {'username': 'STRASSEÄ', 'password': 'casesensitive1'}
                if native:
                    self.assertEqual(self.client.post('/api/session', json=credentials).status_code, 401)
                else:
                    response = self.client.post('/login', data=credentials)
                    self.assertIn('Falscher Benutzername oder Passwort.', response.get_data(as_text=True))
                self.assertFalse(self.client.get('/api/session').json['authenticated'])

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

    @patch('files.routes.games.requests.post')
    @patch('files.routes.games.get_igdb_token', return_value='test-token')
    def test_igdb_search_combines_type_year_and_id(self, _, post):
        self.register()
        app.config.update(TWITCH_CLIENT_ID='test-client', TWITCH_CLIENT_SECRET='test-secret')
        post.return_value.json.return_value = []
        response = self.client.post('/api/search', json={
            'query': 'Battlefield 6', 'release_year': '2025', 'game_type': 0,
        })
        self.assertEqual(response.status_code, 200, response.json)
        body = post.call_args.kwargs['data']
        self.assertIn('where game_type = 0 & first_release_date >= 1735689600 & first_release_date < 1767225600;', body)
        self.assertEqual(body.count('where '), 1)
        self.assertTrue(body.endswith('limit 10;'))
        for game_type in range(15):
            with self.subTest(game_type=game_type):
                response = self.client.post('/api/search', json={'query': 'Spiel', 'game_type': game_type})
                self.assertEqual(response.status_code, 200)
                self.assertIn(f'where game_type = {game_type};', post.call_args.kwargs['data'])
        response = self.client.post('/api/search', json={'query': '317407', 'game_type': 1})
        self.assertEqual(response.status_code, 200)
        self.assertTrue(post.call_args.kwargs['data'].startswith('where id = 317407 & game_type = 1;'))
        for extra in ({}, {'game_type': None}):
            response = self.client.post('/api/search', json={'query': 'Spiel', **extra})
            self.assertEqual(response.status_code, 200)
            self.assertNotIn('game_type', post.call_args.kwargs['data'])

    @patch('files.routes.games.requests.post')
    @patch('files.routes.games.get_igdb_token')
    def test_igdb_search_rejects_invalid_types_before_upstream_call(self, token, post):
        self.register()
        for value in (-1, 15, True, False, 1.5, 0.0, '0', '', [], {}, '0; limit 500;'):
            with self.subTest(value=value):
                response = self.client.post('/api/search', json={'query': 'Spiel', 'game_type': value})
                self.assertEqual(response.status_code, 400, response.json)
        token.assert_not_called()
        post.assert_not_called()

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
        self.assertIn('id="gameTypeSearch"', response.get_data(as_text=True))
        self.assertIn('<option value="0">Spielevollversion</option>', response.get_data(as_text=True))
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

    def test_invalid_json_values_are_rejected_without_mutation(self):
        self.register()
        invalid_fields = [('format', []), ('status', {}), ('igdb_id', 2**63),
                          ('igdb_id', '²'), ('igdb_id', '9' * 5000),
                          ('rating', '²'), ('rating', False), ('rating', 2.5),
                          ('cover_url', 'https://[broken'),
                          ('cover_url', 'https://images.igdb.com:broken/a')]
        for field, value in invalid_fields:
            with self.subTest(field=field, value=str(value)[:60]):
                response = self.client.post('/api/games', json={**self.manual_payload(), field: value})
                self.assertEqual(response.status_code, 400, response.get_json())
        for theme in ([], {}, None, True):
            self.assertEqual(self.client.put('/api/account/theme', json={'theme': theme}).status_code, 400)
        with app.app_context():
            user_id = User.query.one().id
            self.assertEqual(Game.query.count(), 0)
        for username in ([], {}, False, None, ''):
            self.assertEqual(self.client.put(f'/api/users/{user_id}', json={'username': username}).status_code, 400)
        self.assertEqual(self.client.put('/api/account', json={
            'username': 'Changed', 'current_password': 'password1', 'new_password': False,
        }).status_code, 400)
        self.assertEqual(self.client.get('/api/games').status_code, 200)

    def test_platform_only_update_advances_version_and_rejects_stale_write(self):
        self.register()
        self.add_game()
        game = self.client.get('/api/games').json['games'][0]
        url = f"/api/games/{game['id']}"
        payload = {'version': game['version'], 'owned_platform': 'GOG.com'}
        key = {'Idempotency-Key': str(uuid4())}
        self.assertEqual(self.client.put(url, json=payload, headers=key).status_code, 200)
        self.assertEqual(self.client.put(url, json=payload, headers=key).status_code, 200)
        self.assertEqual(self.client.put(url, json={**payload, 'owned_platform': 'Steam'}).status_code, 409)
        updated = self.client.get('/api/games').json['games'][0]
        self.assertEqual(updated['version'], game['version'] + 1)
        self.assertEqual(updated['owned_platform'], 'GOG.com')

    def test_concurrent_platform_only_updates_do_not_overwrite_each_other(self):
        self.register()
        self.add_game()
        game = self.client.get('/api/games').json['games'][0]
        cookie = self.client.get_cookie('session').value
        barrier = Barrier(2)

        def synchronized_commit(*args, **kwargs):
            barrier.wait(timeout=10)
            return real_commit_changes(*args, **kwargs)

        def update(platform):
            client = app.test_client()
            client.set_cookie('session', cookie)
            return client.put(f"/api/games/{game['id']}", json={
                'version': game['version'], 'owned_platform': platform,
            }).status_code

        with patch('files.routes.games.commit_changes', side_effect=synchronized_commit):
            with ThreadPoolExecutor(max_workers=2) as pool:
                self.assertEqual(sorted(pool.map(update, ['GOG.com', 'Switch'])), [200, 409])
        updated = self.client.get('/api/games').json['games'][0]
        self.assertEqual(updated['version'], game['version'] + 1)
        self.assertIn(updated['owned_platform'], ['GOG.com', 'Switch'])

    def test_database_and_unexpected_errors_are_safe_and_traceable(self):
        self.register()
        for error, status in [(SQLAlchemyError('internal-detail'), 503), (RuntimeError('internal-detail'), 500)]:
            for endpoint, url in [('games.list_games', '/api/games'), ('main.index', '/')]:
                with self.subTest(endpoint=endpoint, status=status):
                    with patch.dict(app.view_functions, {endpoint: Mock(side_effect=error)}):
                        with self.assertLogs(app.logger, level='ERROR') as logs:
                            response = self.client.get(url + '?private-query=hidden')
                    self.assertEqual(response.status_code, status)
                    request_id = response.headers['X-Request-ID']
                    self.assertIn(request_id, response.get_data(as_text=True))
                    self.assertIn(request_id, '\n'.join(logs.output))
                    self.assertNotIn('private-query', '\n'.join(logs.output))
                    self.assertNotIn('internal-detail', response.get_data(as_text=True))
                    self.assertEqual(response.headers['Cache-Control'], 'no-store')
                    if url.startswith('/api/'):
                        self.assertEqual(response.json['request_id'], request_id)
        self.assertEqual(self.client.get('/api/games').status_code, 200)

    def test_protocol_errors_and_oversized_resource_ids(self):
        self.register()
        response = self.client.patch('/api/games', json={})
        self.assertEqual(response.status_code, 405)
        self.assertIn('GET', response.headers['Allow'])
        response = self.client.post('/api/games', data='x' * (65 * 1024), content_type='application/json')
        self.assertEqual(response.status_code, 413)
        self.assertIn('64 KiB', response.json['error'])
        self.assertEqual(self.client.delete(f'/api/games/{2**100}').status_code, 404)
        self.assertEqual(self.client.delete(f'/api/users/{2**100}').status_code, 404)
        with self.client.session_transaction() as session:
            session['_user_id'] = 'invalid'
            session['_fresh'] = True
        self.assertEqual(self.client.get('/api/games').status_code, 401)

    def test_healthcheck_failure_is_logged_and_sql_parameters_are_hidden(self):
        from sqlalchemy import text
        with patch('sqlalchemy.orm.Session.execute', side_effect=SQLAlchemyError('test')):
            with self.assertLogs(app.logger, level='ERROR') as logs:
                response = self.client.get('/health')
        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json, {'status': 'unhealthy'})
        self.assertIn(response.headers['X-Request-ID'], '\n'.join(logs.output))
        with app.app_context():
            self.assertTrue(db.engine.hide_parameters)
            try:
                db.session.execute(text('SELECT missing_function(:value)'), {'value': 'sensitive-sentinel'})
            except SQLAlchemyError as error:
                self.assertNotIn('sensitive-sentinel', str(error))
                self.assertIn('parameters hidden', str(error))
            else:
                self.fail('Die ungültige SQL-Abfrage muss fehlschlagen.')
            finally:
                db.session.rollback()
        self.assertEqual(self.client.get('/health').status_code, 200)

    def test_password_write_limits_are_consistent(self):
        with patch('files.routes.auth.generate_password_hash') as generate:
            response = self.register(password='x' * 4097)
            self.assertIn('4096', response.get_data(as_text=True))
            generate.assert_not_called()
        self.register()
        with app.app_context():
            user_id = User.query.one().id
        self.assertEqual(self.client.put(f'/api/users/{user_id}', json={'password': 'x' * 4097}).status_code, 400)
        self.assertEqual(self.client.put('/api/account', json={
            'username': 'Tester', 'current_password': 'password1', 'new_password': 'x' * 4097,
        }).status_code, 400)
        self.assertEqual(self.client.post('/api/session', json={
            'username': 'Tester', 'password': 'password1',
        }).status_code, 200)

    @patch('files.routes.games.get_igdb_token', return_value='private-token')
    @patch('files.routes.games.requests.post')
    def test_igdb_failures_and_malformed_results(self, post, _):
        import requests
        self.register()
        app.config.update(TWITCH_CLIENT_ID='test-client', TWITCH_CLIENT_SECRET='test-secret')
        for payload in ({}, [None], ['invalid']):
            post.return_value.json.return_value = payload
            with self.assertLogs(app.logger, level='WARNING'):
                response = self.client.post('/api/search', json={'query': 'Zelda'})
            self.assertEqual(response.status_code, 502)
        post.side_effect = requests.Timeout('private-token')
        with self.assertLogs(app.logger, level='WARNING') as logs:
            response = self.client.post('/api/search', json={'query': 'Zelda'})
        self.assertEqual(response.status_code, 502)
        self.assertNotIn('private-token', '\n'.join(logs.output))
        self.assertIn('Timeout', '\n'.join(logs.output))
        for malformed in (True, 5, {'organization': []}, [{'organization': [], 'rating_category': {}}]):
            self.assertEqual(get_preferred_age_rating(malformed), '')


if __name__ == '__main__':
    unittest.main()
