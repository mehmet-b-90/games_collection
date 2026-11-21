import time
import threading
from collections import OrderedDict

import requests
from flask import current_app
from .database import request_id
from .validation import VALID_AGE_RATINGS

_token = None
_token_expires = 0
_token_owner = None
_token_lock = threading.Lock()
_playtime_lock = threading.Lock()
_playtime_fields = ('hastily', 'normally', 'completely')

# Current IGDB game_types (live verified on 2026-10-30), shared by search validation and UI.
IGDB_GAME_TYPES = {
    0: 'Spielevollversion', 1: 'DLC', 2: 'Erweiterung', 3: 'Bundle',
    4: 'Eigenständige Erweiterung', 5: 'Mod', 6: 'Episode', 7: 'Season',
    8: 'Remake', 9: 'Remaster', 10: 'Erweiterte Spielversion', 11: 'Portierung',
    12: 'Abspaltung (Fork)', 13: 'Paket / Add-on', 14: 'Update',
}


def log_upstream_failure(operation, error=None):
    # Requests exceptions can contain token-bearing URLs or response bodies.
    status = getattr(getattr(error, 'response', None), 'status_code', None)
    current_app.logger.warning('%s [Fehler-ID: %s; Typ: %s; HTTP: %s]', operation,
                               request_id(), type(error).__name__ if error else 'Antwortformat',
                               status if type(status) is int else 'unbekannt')

def get_preferred_age_rating(age_ratings):
    """Liefert bevorzugt USK, andernfalls PEGI; andere Systeme werden ignoriert."""
    if not isinstance(age_ratings, list):
        return ''
    for preferred_organization in ('USK', 'PEGI'):
        for age_rating in age_ratings or []:
            if not isinstance(age_rating, dict):
                continue
            organization = age_rating.get('organization')
            if isinstance(organization, dict):
                organization_name = organization.get('name')
            elif type(organization) is int:
                organization_name = {4: 'USK', 2: 'PEGI'}.get(organization)
            else:
                continue
            if organization_name != preferred_organization:
                continue
            category = age_rating.get('rating_category')
            rating = category.get('rating') if isinstance(category, dict) else None
            mapped = f'{preferred_organization} {str(rating).strip()}'
            if mapped in VALID_AGE_RATINGS:
                return mapped
    return ''

def get_igdb_credentials(verify=False):
    """Return a shared app token and its remaining lifetime, never the client secret."""
    global _token, _token_expires, _token_owner
    client_id = current_app.config['TWITCH_CLIENT_ID']
    client_secret = current_app.config['TWITCH_CLIENT_SECRET']
    if not client_id or not client_secret:
        return None
    with _token_lock:
        identity = (client_id, client_secret)
        if verify and _token and _token_owner == identity and time.time() < _token_expires - 60:
            try:
                validation = requests.get('https://id.twitch.tv/oauth2/validate',
                                          headers={'Authorization': f'OAuth {_token}'}, timeout=15)
                if validation.status_code == 401:
                    _token_expires = 0
                else:
                    validation.raise_for_status()
                    remaining = validation.json().get('expires_in')
                    if type(remaining) is not int or remaining <= 60:
                        _token_expires = 0
                    else:
                        _token_expires = min(_token_expires, time.time() + remaining)
            except (requests.RequestException, ValueError, AttributeError) as error:
                log_upstream_failure('Twitch-Tokenprüfung fehlgeschlagen', error)
                return None
        if _token_owner != identity or time.time() >= _token_expires - 60:
            try:
                response = requests.post('https://id.twitch.tv/oauth2/token', data={
                    'client_id': client_id, 'client_secret': client_secret,
                    'grant_type': 'client_credentials'}, timeout=15)
                response.raise_for_status()
                payload = response.json()
                token = payload.get('access_token')
                lifetime = payload.get('expires_in')
                if not isinstance(token, str) or not token or type(lifetime) is not int or lifetime <= 60:
                    log_upstream_failure('Twitch-Tokenanforderung: ungültige Antwortstruktur')
                    return None
            except (requests.RequestException, ValueError, AttributeError) as error:
                log_upstream_failure('Twitch-Tokenanforderung fehlgeschlagen', error)
                return None
            _token, _token_expires, _token_owner = token, time.time() + lifetime, identity
        return {'client_id': client_id, 'access_token': _token,
                'expires_in': max(0, int(_token_expires - time.time()))}


def get_igdb_token():
    credentials = get_igdb_credentials()
    return credentials['access_token'] if credentials else None


def game_time_to_beat(game_id):
    """Load one catalog record on demand, including cached misses."""
    with _playtime_lock:
        cache = current_app.extensions.setdefault('igdb_playtime', OrderedDict())
        if game_id in cache:
            expires_at, times = cache[game_id]
            if time.monotonic() < expires_at:
                cache.move_to_end(game_id)
                return times

    credentials = get_igdb_credentials()
    if not credentials:
        raise ValueError('IGDB credentials unavailable')
    headers = {'Client-ID': credentials['client_id'], 'Authorization': 'Bearer ' + credentials['access_token']}
    body = f'fields {",".join(_playtime_fields)}; where game_id = {game_id}; limit 1;'
    response = requests.post('https://api.igdb.com/v4/game_time_to_beats', headers=headers, data=body, timeout=8)
    response.raise_for_status()
    results = response.json()
    if not isinstance(results, list) or (results and not isinstance(results[0], dict)):
        raise ValueError('Invalid IGDB playtime record')
    record = results[0] if results else {}
    times = {field: record[field] for field in _playtime_fields
             if type(record.get(field)) is int and 0 < record[field] <= 2**31 - 1}

    with _playtime_lock:
        cache[game_id] = (time.monotonic() + 86_400, times)
        cache.move_to_end(game_id)
        if len(cache) > 512:
            cache.popitem(last=False)
    return times
