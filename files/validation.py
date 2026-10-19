from urllib.parse import urlparse
from .config import PLATFORMS

GAME_TEXT_FIELDS = {
    'title': 200, 'cover_url': 500, 'release_date': 50, 'developer': 1000,
    'publisher': 1000, 'game_engine': 1000, 'available_platforms': 2000,
    'igdb_url': 500, 'age_rating': 50,
}
GAME_ALLOWED_FIELDS = set(GAME_TEXT_FIELDS) | {'igdb_id', 'owned_platform', 'format', 'rating', 'status', 'version'}
VALID_AGE_RATINGS = {
    'USK 0', 'USK 6', 'USK 12', 'USK 16', 'USK 18',
    'PEGI 3', 'PEGI 7', 'PEGI 12', 'PEGI 16', 'PEGI 18',
}

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
    try:
        parsed = urlparse(value)
        hostname = parsed.hostname
        port = parsed.port
    except ValueError:
        raise ValueError(f'"{field}" enthält keine gültige HTTPS-Adresse.') from None
    if parsed.scheme != 'https' or hostname not in allowed_hosts or parsed.username is not None or parsed.password is not None or port not in (None, 443):
        raise ValueError(f'"{field}" enthält keine erlaubte HTTPS-Adresse.')
    return value


def validate_integer(value, field, minimum, maximum):
    # Keep numeric strings compatible with existing web clients; reject floats,
    # booleans, Unicode digits and values SQLite cannot bind as INTEGER.
    if type(value) is int:
        number = value
    elif isinstance(value, str) and value.isascii() and value.isdigit() and len(value) <= 19:
        number = int(value)
    else:
        raise ValueError(f'"{field}" muss eine ganze Zahl zwischen {minimum} und {maximum} sein.')
    if not minimum <= number <= maximum:
        raise ValueError(f'"{field}" muss eine ganze Zahl zwischen {minimum} und {maximum} sein.')
    return number

def validate_game_payload(data, creating=False):
    allowed_fields = GAME_ALLOWED_FIELDS - {'version'} if creating else GAME_ALLOWED_FIELDS
    unexpected = set(data) - allowed_fields
    if unexpected:
        raise ValueError(f'Unbekannte Felder: {", ".join(sorted(unexpected))}.')
    if creating and 'title' not in data:
        raise ValueError('"title" ist erforderlich.')
    cleaned = {}
    for field, maximum in GAME_TEXT_FIELDS.items():
        if field in data:
            cleaned[field] = validate_text(data[field], field, maximum, field == 'title')
    release_year = cleaned.get('release_date')
    if release_year is not None:
        if not release_year.isascii() or not release_year.isdigit() or len(release_year) != 4 or not 1900 <= int(release_year) <= 2100:
            raise ValueError('"release_date" muss ein Jahr zwischen 1900 und 2100 sein.')
    age_rating = cleaned.get('age_rating')
    if age_rating is not None and age_rating not in VALID_AGE_RATINGS:
        raise ValueError('"age_rating" enthält keine unterstützte USK- oder PEGI-Freigabe.')
    if 'cover_url' in cleaned:
        cleaned['cover_url'] = validate_https_url(cleaned['cover_url'], 'cover_url', {'images.igdb.com'})
    if 'igdb_url' in cleaned:
        cleaned['igdb_url'] = validate_https_url(cleaned['igdb_url'], 'igdb_url', {'igdb.com', 'www.igdb.com'})
    if 'format' in data:
        if not isinstance(data['format'], str) or data['format'] not in {'Physisch', 'Digital'}:
            raise ValueError('"format" muss "Physisch" oder "Digital" sein.')
        cleaned['format'] = data['format']
    if 'status' in data:
        if not isinstance(data['status'], str) or data['status'] not in {'Nicht gespielt', 'Gespielt', 'Abgebrochen'}:
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
        else:
            cleaned['rating'] = validate_integer(rating, 'rating', 0, 10)
    if 'igdb_id' in data:
        igdb_id = data['igdb_id']
        if igdb_id in (None, ''):
            cleaned['igdb_id'] = None
        else:
            cleaned['igdb_id'] = validate_integer(igdb_id, 'igdb_id', 1, 2**63 - 1)
    if not creating and 'version' in data:
        version = data['version']
        cleaned['version'] = validate_integer(version, 'version', 1, 2**63 - 2)
    return cleaned
