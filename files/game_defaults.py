"""Account defaults are suggestions for new games, never mutations of saved games."""
from .config import PLATFORMS
from .extensions import db
from .igdb import IGDB_GAME_TYPES
from .models import UserGameDefaults


def initial_defaults():
    return {'game_type': None, 'format': 'Digital', 'status': 'Nicht gespielt', 'platforms': []}


def get_defaults(user_id):
    record = db.session.get(UserGameDefaults, user_id)
    return record.values if record else initial_defaults()


def validate_defaults(data):
    if not isinstance(data, dict) or set(data) != set(initial_defaults()):
        raise ValueError('Titeltyp, Format, Status und Plattformen werden erwartet.')
    kind = data['game_type']
    if kind is not None and (type(kind) is not int or kind not in IGDB_GAME_TYPES):
        raise ValueError('Ungültiger Titeltyp.')
    if not isinstance(data['format'], str) or data['format'] not in ('Digital', 'Physisch'):
        raise ValueError('Ungültiges Format.')
    if not isinstance(data['status'], str) or data['status'] not in ('Nicht gespielt', 'Gespielt', 'Abgebrochen'):
        raise ValueError('Ungültiger Status.')
    platforms = data['platforms']
    if not isinstance(platforms, list) or len(platforms) > len(PLATFORMS) or any(
            not isinstance(item, str) or item not in PLATFORMS for item in platforms):
        raise ValueError('Ungültige Besitzplattformen.')
    return {**data, 'platforms': sorted(set(platforms))}
