from urllib.parse import urlparse
from .config import PLATFORMS

GAME_TEXT_FIELDS = {
    'title': 200, 'cover_url': 500, 'release_date': 50, 'developer': 1000,
    'publisher': 1000, 'game_engine': 1000, 'available_platforms': 2000,
    'igdb_url': 500, 'age_rating': 50,
}
GAME_ALLOWED_FIELDS = set(GAME_TEXT_FIELDS) | {'igdb_id', 'owned_platform', 'format', 'rating', 'status', 'version'}

def normalize_username(username):
    return username.strip().casefold()

def validate_text(value, field, maximum, required=False):
    if value is None:
        if required:
            raise ValueError(f'"{field}" ist erforderlich.')
        return None
    if not isinstance(value, str):
        raise ValueError(f'"{field}" muss Text sein.')
    value = value.strip()
    if required and not value:
        raise ValueError(f'"{field}" darf nicht leer sein.')
    if len(value) > maximum:
        raise ValueError(f'"{field}" darf höchstens {maximum} Zeichen enthalten.')
    return value or None

def validate_https_url(value, field, allowed_hosts):
    if value is None:
        return None
    parsed = urlparse(value)
    if parsed.scheme != 'https' or parsed.hostname not in allowed_hosts:
        raise ValueError(f'"{field}" enthält keine erlaubte HTTPS-Adresse.')
    return value

def validate_game_payload(data, creating=False):
    unexpected = set(data) - GAME_ALLOWED_FIELDS
    if unexpected:
        raise ValueError(f'Unbekannte Felder: {", ".join(sorted(unexpected))}.')
    if creating and 'title' not in data:
        raise ValueError('"title" ist erforderlich.')
    cleaned = {}
    for field, maximum in GAME_TEXT_FIELDS.items():
        if field in data:
            cleaned[field] = validate_text(data[field], field, maximum, field == 'title')
    if 'cover_url' in cleaned:
        cleaned['cover_url'] = validate_https_url(cleaned['cover_url'], 'cover_url', {'images.igdb.com'})
    if 'igdb_url' in cleaned:
        cleaned['igdb_url'] = validate_https_url(cleaned['igdb_url'], 'igdb_url', {'igdb.com', 'www.igdb.com'})
    if 'format' in data:
        if data['format'] not in {'Physisch', 'Digital'}:
            raise ValueError('"format" muss "Physisch" oder "Digital" sein.')
        cleaned['format'] = data['format']
    if 'status' in data:
        if data['status'] not in {'Nicht gespielt', 'Gespielt', 'Abgebrochen'}:
            raise ValueError('Ungültiger Spielstatus.')
        cleaned['status'] = data['status']
    if 'owned_platform' in data:
        platforms = data['owned_platform']
        if not isinstance(platforms, str):
            raise ValueError('"owned_platform" muss Text sein.')
        selected = [platform.strip() for platform in platforms.split(',') if platform.strip()]
        if not selected:
            raise ValueError('Mindestens eine Besitzplattform ist erforderlich.')
        invalid = set(selected) - PLATFORMS
        if invalid:
            raise ValueError(f'Ungültige Besitzplattform: {", ".join(sorted(invalid))}.')
        cleaned['owned_platform'] = list(dict.fromkeys(selected))
    elif creating:
        raise ValueError('"owned_platform" ist erforderlich.')
    if 'rating' in data:
        rating = data['rating']
        if rating in (None, ''):
            cleaned['rating'] = None
        elif isinstance(rating, bool) or not str(rating).isdigit() or not 0 <= int(rating) <= 10:
            raise ValueError('"rating" muss eine ganze Zahl zwischen 0 und 10 sein.')
        else:
            cleaned['rating'] = int(rating)
    if 'igdb_id' in data:
        igdb_id = data['igdb_id']
        if igdb_id in (None, ''):
            cleaned['igdb_id'] = None
        elif isinstance(igdb_id, bool) or not str(igdb_id).isdigit() or int(igdb_id) <= 0:
            raise ValueError('"igdb_id" muss eine positive ganze Zahl sein.')
        else:
            cleaned['igdb_id'] = int(igdb_id)
    if not creating and 'version' in data:
        version = data['version']
        if isinstance(version, bool) or not str(version).isdigit() or int(version) < 1:
            raise ValueError('"version" muss eine positive ganze Zahl sein.')
        cleaned['version'] = int(version)
    return cleaned
