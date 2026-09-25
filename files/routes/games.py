from datetime import datetime, timezone

import requests
from flask import Blueprint, current_app, jsonify, request
from flask_login import current_user, login_required
from ..database import api_error, commit_changes
from ..extensions import db
from ..igdb import get_igdb_token, get_preferred_age_rating
from ..models import Game, GamePlatformOwnership
from ..validation import validate_game_payload

games_bp = Blueprint('games', __name__, url_prefix='/api')

def json_object():
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else None

@games_bp.route('/search', methods=['POST'])
@login_required
def search_igdb():
    data = json_object()
    if data is None or not isinstance(data.get('query'), str):
        return api_error('"query" muss Text sein.')
    unexpected = set(data) - {'query', 'release_year'}
    if unexpected:
        return api_error(f'Unbekannte Felder: {", ".join(sorted(unexpected))}.')
    query = data['query'].strip()
    if not query or len(query) > 200:
        return api_error('"query" muss 1 bis 200 Zeichen lang sein.')
    release_year = data.get('release_year')
    if release_year in (None, ''):
        release_year = None
    elif isinstance(release_year, bool) or not str(release_year).isdigit():
        return api_error('"release_year" muss ein gültiges Jahr sein.')
    else:
        release_year = int(release_year)
        if not 1900 <= release_year <= 2100:
            return api_error('"release_year" muss zwischen 1900 und 2100 liegen.')
    client_id = current_app.config['TWITCH_CLIENT_ID']
    if not client_id or not current_app.config['TWITCH_CLIENT_SECRET']:
        return api_error('Die IGDB-Zugangsdaten sind nicht konfiguriert.', 503)
    token = get_igdb_token()
    if not token:
        return api_error('IGDB-Authentifizierung fehlgeschlagen.', 502)
    headers = {'Client-ID': client_id, 'Authorization': f'Bearer {token}'}
    fields = 'fields id, name, cover.image_id, first_release_date, platforms.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, url, game_engines.name, age_ratings.organization.name, age_ratings.rating_category.rating;'
    if query.isdigit() and release_year is None:
        body = f'where id = {query}; {fields} limit 1;'
    else:
        escaped_query = query.replace('\\', '\\\\').replace('"', '\\"')
        year_filter = ''
        if release_year is not None:
            start = int(datetime(release_year, 1, 1, tzinfo=timezone.utc).timestamp())
            end = int(datetime(release_year + 1, 1, 1, tzinfo=timezone.utc).timestamp())
            year_filter = f' where first_release_date >= {start} & first_release_date < {end};'
        body = f'search "{escaped_query}";{year_filter} {fields} limit 10;'
    try:
        response = requests.post('https://api.igdb.com/v4/games', headers=headers, data=body, timeout=15)
        response.raise_for_status()
        results = response.json()
    except (requests.RequestException, ValueError):
        return api_error('IGDB-Suche ist derzeit nicht verfügbar.', 502)
    if not isinstance(results, list):
        return api_error('IGDB hat eine ungültige Antwort geliefert.', 502)
    for game in results:
        game['mapped_age_rating'] = get_preferred_age_rating(game.get('age_ratings', []))
    return jsonify(results)

@games_bp.route('/games', methods=['POST'])
@login_required
def add_game():
    data = json_object()
    if data is None:
        return api_error('Es wird ein JSON-Objekt erwartet.')
    try:
        game_data = validate_game_payload(data, creating=True)
    except ValueError as error:
        return api_error(str(error))
    platforms = game_data.pop('owned_platform')
    game = Game(user_id=current_user.id, **game_data)
    game.owned_platforms = [GamePlatformOwnership(platform=platform) for platform in platforms]
    db.session.add(game)
    error = commit_changes('Dieses IGDB-Spiel ist bereits in deiner Sammlung.')
    return error or ({'status': 'success'}, 201)

@games_bp.route('/games/<int:game_id>', methods=['PUT', 'DELETE'])
@login_required
def manage_game(game_id):
    game = db.get_or_404(Game, game_id)
    if game.user_id != current_user.id:
        return api_error('Unauthorized', 403)
    if request.method == 'DELETE':
        db.session.delete(game)
        return commit_changes() or {'status': 'deleted'}
    data = json_object()
    if data is None:
        return api_error('Es wird ein JSON-Objekt erwartet.')
    try:
        game_data = validate_game_payload(data)
    except ValueError as error:
        return api_error(str(error))
    expected_version = game_data.pop('version', None)
    if expected_version is None:
        return api_error('Die Versionsnummer des Spiels fehlt.')
    if game.version != expected_version:
        return api_error('Das Spiel wurde zwischenzeitlich geändert. Bitte lade die Seite neu.', 409)
    if not game_data:
        return api_error('Keine Änderungen übermittelt.')
    platforms = game_data.pop('owned_platform', None)
    for key, value in game_data.items():
        setattr(game, key, value)
    if platforms is not None:
        requested = set(platforms)
        for ownership in list(game.owned_platforms):
            if ownership.platform not in requested:
                game.owned_platforms.remove(ownership)
        existing = {ownership.platform for ownership in game.owned_platforms}
        game.owned_platforms.extend(GamePlatformOwnership(platform=p) for p in platforms if p not in existing)
    return commit_changes('Dieses IGDB-Spiel ist bereits in deiner Sammlung.') or {'status': 'updated'}
