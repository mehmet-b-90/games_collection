"""Shared, account-independent IGDB discovery feed (one cache per app/worker)."""
import copy
import math
import threading
import time
from datetime import datetime, timezone

import requests
from flask import current_app

from .igdb import get_igdb_credentials, get_preferred_age_rating, log_upstream_failure

IGDB_GAME_FIELDS = ('id,name,cover.image_id,first_release_date,platforms.name,'
                    'involved_companies.company.name,involved_companies.developer,'
                    'involved_companies.publisher,url,game_engines.name,'
                    'age_ratings.organization.name,age_ratings.rating_category.rating')
CACHE_SECONDS = 86400
RETRY_SECONDS = 60
STALE_SECONDS = 7 * CACHE_SECONDS
_init_lock = threading.Lock()


class PopScoreError(Exception):
    def __init__(self, message, status=502):
        super().__init__(message)
        self.status = status


def _post(endpoint, body, credentials):
    response = requests.post('https://api.igdb.com/v4/' + endpoint,
                             headers={'Client-ID': credentials['client_id'],
                                      'Authorization': 'Bearer ' + credentials['access_token']},
                             data=body, timeout=10)
    response.raise_for_status()
    result = response.json()
    if not isinstance(result, list) or any(not isinstance(item, dict) for item in result):
        raise ValueError('Invalid IGDB list')
    return result


def _fetch():
    credentials = get_igdb_credentials()
    if not credentials:
        raise PopScoreError('IGDB-Authentifizierung fehlgeschlagen. Bitte später erneut versuchen.')
    primitives = _post('popularity_primitives',
                       'fields game_id,value; where popularity_type = 1; sort value desc; limit 100;', credentials)
    ids = []
    for primitive in primitives:
        game_id = primitive.get('game_id')
        if type(game_id) is not int or not 0 < game_id <= 2147483647:
            raise ValueError('Invalid IGDB game ID')
        if game_id not in ids:
            ids.append(game_id)
    if not ids:
        return []
    games = _post('games', f'fields {IGDB_GAME_FIELDS},genres.name,rating; '
                  f'where id = ({",".join(map(str, ids))}); limit 100;', credentials)
    by_id = {}
    for game in games:
        game_id = game.get('id')
        if type(game_id) is not int or not isinstance(game.get('name'), str) or not game['name'].strip():
            raise ValueError('Invalid IGDB game')
        # Optional fields can be absent; reject malformed nested values before clients see them.
        for field in ('platforms', 'genres', 'game_engines', 'involved_companies', 'age_ratings'):
            if game.get(field) is None:
                game[field] = []
            if not isinstance(game[field], list) or any(not isinstance(item, dict) for item in game[field]):
                raise ValueError('Invalid IGDB detail')
        if game.get('cover') is not None and not isinstance(game['cover'], dict):
            raise ValueError('Invalid cover')
        rating = game.get('rating')
        if type(rating) not in (int, float) or not math.isfinite(rating) or not 0 <= rating <= 100:
            game['rating'] = None
        game['mapped_age_rating'] = get_preferred_age_rating(game['age_ratings'])
        by_id[game_id] = game
    return [dict(by_id[game_id], popularity_rank=rank) for rank, game_id in enumerate(ids, 1) if game_id in by_id]


def popular_games():
    identity = (current_app.config['TWITCH_CLIENT_ID'], current_app.config['TWITCH_CLIENT_SECRET'])
    if not all(identity):
        raise PopScoreError('Die IGDB-Zugangsdaten sind nicht konfiguriert.', 503)
    with _init_lock:
        cache = current_app.extensions.setdefault('popscore', {'lock': threading.Lock()})
    with cache['lock']:
        if cache.get('identity') != identity:
            lock = cache['lock']
            cache.clear()
            cache.update(lock=lock, identity=identity)
        now = time.time()
        age = now - cache.get('updated', 0)
        fresh = 'games' in cache and age < CACHE_SECONDS
        if not fresh and now >= cache.get('retry_at', 0):
            try:
                games = _fetch()
                cache.update(games=games, updated=time.time(), retry_at=0)
                fresh = True
            except (requests.RequestException, ValueError, TypeError, PopScoreError) as error:
                log_upstream_failure('IGDB-PopScore fehlgeschlagen', error)
                cache['retry_at'] = time.time() + RETRY_SECONDS
        if 'games' not in cache or time.time() - cache['updated'] > STALE_SECONDS:
            raise PopScoreError('Aktuell beliebte Spiele sind gerade nicht verfügbar. Bitte später erneut versuchen.')
        return {'games': copy.deepcopy(cache['games']), 'stale': not fresh,
                'updated_at': datetime.fromtimestamp(cache['updated'], timezone.utc).isoformat(),
                'source': 'igdb_visits'}
