import time
import requests
from flask import current_app

_token = None
_token_expires = 0

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

def get_igdb_token():
    global _token, _token_expires
    if time.time() < _token_expires:
        return _token
    client_id = current_app.config['TWITCH_CLIENT_ID']
    client_secret = current_app.config['TWITCH_CLIENT_SECRET']
    auth_url = f'https://id.twitch.tv/oauth2/token?client_id={client_id}&client_secret={client_secret}&grant_type=client_credentials'
    try:
        response = requests.post(auth_url, timeout=15)
        response.raise_for_status()
        payload = response.json()
    except (requests.RequestException, ValueError):
        return None
    _token = payload.get('access_token')
    _token_expires = time.time() + payload.get('expires_in', 0) - 60
    return _token
