import unittest
from unittest.mock import Mock, patch
from concurrent.futures import ThreadPoolExecutor

import requests
from flask import Flask
from files.popscore import popular_games, PopScoreError, CACHE_SECONDS, STALE_SECONDS


class PopScoreTest(unittest.TestCase):
    def setUp(self):
        self.app = Flask(__name__)
        self.app.config.update(TWITCH_CLIENT_ID='test-client', TWITCH_CLIENT_SECRET='test-secret')

    def fetch(self):
        with self.app.test_request_context():
            return popular_games()

    def responses(self):
        return [Mock(json=lambda: [{'game_id': 8}, {'game_id': 4}, {'game_id': 8}]),
                Mock(json=lambda: [{'id': 4, 'name': 'Second', 'rating': 95},
                                   {'id': 8, 'name': 'First', 'age_ratings': [
                                       {'organization': {'name': 'USK'}, 'rating_category': {'rating': '12'}}]}])]

    @patch('files.popscore.get_igdb_credentials', return_value={'client_id': 'test', 'access_token': 'test'})
    @patch('files.popscore.requests.post')
    def test_ranking_details_cache_and_independent_return_values(self, post, credentials):
        post.side_effect = self.responses()
        result = self.fetch()
        self.assertEqual([g['id'] for g in result['games']], [8, 4])
        self.assertEqual([g['popularity_rank'] for g in result['games']], [1, 2])
        self.assertEqual(result['games'][0]['mapped_age_rating'], 'USK 12')
        self.assertEqual(result['games'][1]['rating'], 95)
        self.assertFalse(result['stale'])
        result['games'].clear()
        self.assertEqual(len(self.fetch()['games']), 2)
        self.assertEqual(post.call_count, 2)
        self.assertIn('where popularity_type = 1;', post.call_args_list[0].kwargs['data'])
        self.assertIn('where id = (8,4);', post.call_args_list[1].kwargs['data'])

    @patch('files.popscore._fetch', return_value=[])
    def test_empty_feed_is_cached_and_parallel_requests_share_fetch(self, fetch):
        with ThreadPoolExecutor(max_workers=4) as pool:
            results = list(pool.map(lambda _: self.fetch(), range(4)))
        self.assertTrue(all(r['games'] == [] for r in results))
        self.assertEqual(fetch.call_count, 1)

    @patch('files.popscore._fetch', return_value=[{'id': 8}])
    def test_stale_fallback_backoff_and_expiry(self, fetch):
        self.fetch()
        cache = self.app.extensions['popscore']
        cache['updated'] -= CACHE_SECONDS + 1
        fetch.side_effect = requests.Timeout()
        self.assertTrue(self.fetch()['stale'])
        self.assertTrue(self.fetch()['stale'])
        self.assertEqual(fetch.call_count, 2)
        cache['updated'] -= STALE_SECONDS
        with self.assertRaises(PopScoreError):
            self.fetch()

    @patch('files.popscore._fetch', side_effect=requests.Timeout())
    def test_initial_failure_and_backoff(self, fetch):
        for _ in range(2):
            with self.assertRaises(PopScoreError):
                self.fetch()
        self.assertEqual(fetch.call_count, 1)

    @patch('files.popscore._fetch', return_value=[])
    def test_config_change_invalidates_cache_and_missing_config_is_503(self, fetch):
        self.fetch()
        self.app.config['TWITCH_CLIENT_ID'] = 'another-client'
        self.fetch()
        self.assertEqual(fetch.call_count, 2)
        self.app.config['TWITCH_CLIENT_SECRET'] = ''
        with self.assertRaises(PopScoreError) as error:
            self.fetch()
        self.assertEqual(error.exception.status, 503)

    @patch('files.popscore.get_igdb_credentials', return_value={'client_id': 'test', 'access_token': 'test'})
    @patch('files.popscore.requests.post')
    def test_malformed_upstream_is_not_cached_as_success(self, post, credentials):
        post.return_value = Mock(json=lambda: [{'game_id': '8; malicious'}])
        with self.assertRaises(PopScoreError):
            self.fetch()
        self.assertNotIn('games', self.app.extensions['popscore'])
        self.assertEqual(post.call_count, 1)
