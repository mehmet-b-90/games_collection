import time
import threading
import requests
from flask import current_app

_token = None
_token_expires = 0
_token_owner = None
_token_lock = threading.Lock()

def get_preferred_age_rating(age_ratings):
    """Liefert bevorzugt USK, andernfalls PEGI; andere Systeme werden ignoriert."""
    for preferred_organization in ('USK', 'PEGI'):
        for age_rating in age_ratings or []:
            if not isinstance(age_rating, dict):
                continue
            organization = age_rating.get('organization')
            organization_name = organization.get('name') if isinstance(organization, dict) else {4: 'USK', 2: 'PEGI'}.get(organization)
            if organization_name != preferred_organization:
                continue
            category = age_rating.get('rating_category')
            rating = category.get('rating') if isinstance(category, dict) else None
            if rating is not None and str(rating).strip():
                return f'{preferred_organization} {str(rating).strip()}'
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
            except (requests.RequestException, ValueError, AttributeError):
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
                    return None
            except (requests.RequestException, ValueError, AttributeError):
                return None
            _token, _token_expires, _token_owner = token, time.time() + lifetime, identity
        return {'client_id': client_id, 'access_token': _token,
                'expires_in': max(0, int(_token_expires - time.time()))}


def get_igdb_token():
    credentials = get_igdb_credentials()
    return credentials['access_token'] if credentials else None
