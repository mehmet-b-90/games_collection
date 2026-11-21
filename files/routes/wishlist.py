from datetime import date, datetime, timezone
from zoneinfo import ZoneInfo

import requests
from flask import Blueprint, jsonify, request
from flask_login import current_user, login_required
from sqlalchemy.dialects.sqlite import insert

from ..database import api_error
from ..extensions import db
from ..igdb import get_igdb_credentials, get_preferred_age_rating
from ..models import Wish
from ..popscore import IGDB_GAME_FIELDS, _post

wishlist_bp = Blueprint('wishlist', __name__, url_prefix='/api/wishlist')


def today():
    return datetime.now(ZoneInfo('Europe/Berlin')).date()


def catalog(ids):
    if not ids:
        return {}
    credentials = get_igdb_credentials()
    if not credentials:
        raise ValueError('credentials')
    result = {}
    fields = ('release_dates.platform.name,release_dates.release_region.region,'
              'release_dates.date_format.format,release_dates.y,release_dates.m,release_dates.date,release_dates.status.name')
    for offset in range(0, len(ids), 100):
        group = ','.join(map(str, ids[offset:offset + 100]))
        for game in _post('games', f'fields {IGDB_GAME_FIELDS},{fields}; where id = ({group}); limit 100;', credentials):
            game['mapped_age_rating'] = get_preferred_age_rating(game.get('age_ratings'))
            result[game['id']] = game
    return result


def options(game):
    choices = {}
    for release in game.get('release_dates', []):
        platform = release.get('platform', {})
        region = release.get('release_region', {}).get('region', '')
        key = (platform.get('id', 0), region)
        precision = release.get('date_format', {}).get('format', '')
        year, month = release.get('y'), release.get('m')
        day = None
        label = 'Termin offen'
        if type(year) is int and precision.startswith('YYYY'):
            label = f'{year} · Tag offen'
            if precision == 'YYYYMM' and type(month) is int:
                label = f'{month:02d}/{year} · Tag offen'
            if precision == 'YYYYMMDD' and type(release.get('date')) is int:
                day = datetime.fromtimestamp(release['date'], timezone.utc).date()
                label = day.strftime('%d.%m.%Y')
        status = release.get('status', {}).get('name', '').lower()
        if status and status != 'full release':
            continue
        option = dict(platform=key[0], region=region, name=platform.get('name', 'Plattform offen'),
                      label=label, day=day.isoformat() if day else None)
        if key not in choices or (option['day'] or '9999') < (choices[key]['day'] or '9999'):
            choices[key] = option
    for platform in game.get('platforms', []):
        if not any(key[0] == platform['id'] for key in choices):
            choices[(platform['id'], '')] = dict(platform=platform['id'], region='', name=platform['name'],
                                                  day=None, label='Termin offen')
    return sorted(choices.values(), key=lambda o: (o['region'].lower() not in ('europe', 'worldwide'), o['name'], o['region'])) or [
        dict(platform=0, region='', name='Plattform offen', label='Termin offen', day=None)]


def lookup(game_id):
    if not 0 < game_id <= 2147483647:
        return None
    return catalog([game_id]).get(game_id)


@wishlist_bp.errorhandler(requests.RequestException)
@wishlist_bp.errorhandler(ValueError)
def upstream_error(_error):
    return api_error('Die IGDB-Termine konnten nicht geladen werden.', 502)


@wishlist_bp.get('/<int:game_id>/options')
@login_required
def choices(game_id):
    game = lookup(game_id)
    return jsonify(options=options(game)) if game else api_error('Titel nicht gefunden.', 404)


@wishlist_bp.put('/<int:game_id>')
@login_required
def add(game_id):
    data = request.get_json(silent=True)
    if (not isinstance(data, dict) or set(data) != {'platform', 'region'} or type(data['platform']) is not int
            or not isinstance(data['region'], str)):
        return api_error('Ungültige Terminauswahl.', 400)
    game = lookup(game_id)
    if not game:
        return api_error('Titel nicht gefunden.', 404)
    choice = next((o for o in options(game) if all(o[k] == v for k, v in data.items())), None)
    if not choice:
        return api_error('Der Termin hat sich geändert. Bitte erneut auswählen.', 409)
    created = today()
    past = bool(choice['day'] and date.fromisoformat(choice['day']) <= created)
    db.session.execute(insert(Wish).values(user_id=current_user.id, game_id=game_id, **data, snapshot=game,
                                           created=created, web_seen=past, android_seen=past).on_conflict_do_nothing())
    saved = db.session.get(Wish, (current_user.id, game_id))
    if saved.platform != data['platform'] or saved.region != data['region']:
        db.session.rollback()
        return api_error('Der Titel ist bereits für einen anderen Termin vorgemerkt.', 409)
    db.session.commit()
    return jsonify(ok=True)


@wishlist_bp.delete('/<int:game_id>')
@login_required
def remove(game_id):
    if request.data and request.get_json(silent=True) != {}:
        return api_error('Beim Entfernen werden keine zusätzlichen Felder erwartet.', 400)
    Wish.query.filter_by(user_id=current_user.id, game_id=game_id).delete()
    db.session.commit()
    return jsonify(ok=True)


@wishlist_bp.get('')
@login_required
def wishes():
    channel = request.args.get('client', 'web')
    if channel not in ('web', 'android'):
        return api_error('Unbekannter Client.', 400)
    rows = Wish.query.filter_by(user_id=current_user.id).order_by(Wish.game_id).all()
    try:
        fresh = catalog([w.game_id for w in rows])
    except (requests.RequestException, ValueError):
        fresh = {}
    items = []
    for wish in rows:
        game = fresh.get(wish.game_id, wish.snapshot)
        candidates = options(game)
        choice = next((o for o in candidates if o['platform'] == wish.platform and o['region'] == wish.region), None)
        if not choice and not wish.region:
            choice = next((o for o in candidates if not wish.platform or o['platform'] == wish.platform), None)
        if not choice:
            choice = dict(next(o for o in options(wish.snapshot) if o['platform'] == wish.platform and o['region'] == wish.region),
                          label='Termin offen', day=None)
        confirmed = wish.game_id in fresh
        due = confirmed and choice['day'] and wish.created <= date.fromisoformat(choice['day']) <= today()
        released = confirmed and choice['day'] and date.fromisoformat(choice['day']) <= today()
        items.append(dict(game=game, release=choice, released=bool(released), notify=bool(due and not getattr(wish, channel + '_seen')),
                          stale=not confirmed))
    return jsonify(items=items)


@wishlist_bp.post('/<int:game_id>/seen')
@login_required
def seen(game_id):
    data = request.get_json(silent=True)
    if not isinstance(data, dict) or set(data) != {'client'} or data['client'] not in ('android', 'web'):
        return api_error('Unbekannter Client.', 400)
    Wish.query.filter_by(user_id=current_user.id, game_id=game_id).update({data['client'] + '_seen': True})
    db.session.commit()
    return jsonify(ok=True)
