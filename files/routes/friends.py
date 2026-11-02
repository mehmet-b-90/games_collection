"""Mutual collection sharing. No public user directory or write delegation."""
from flask import Blueprint, request
from flask_login import current_user, login_required
from sqlalchemy import or_
from ..database import api_error, commit_changes
from ..extensions import db
from ..models import Friendship, Game, User
from ..validation import normalize_username

friends_bp = Blueprint('friends', __name__, url_prefix='/api/friends')


def pair_with(user_id):
    low, high = sorted((current_user.id, user_id))
    return Friendship.query.filter_by(user_low=low, user_high=high).first()


def public_user(user):
    return {'id': user.id, 'username': user.username}


@friends_bp.route('', methods=['GET', 'POST'])
@login_required
def friends():
    if request.method == 'GET':
        if request.args:
            return api_error('Es werden keine Suchparameter erwartet.')
        pairs = Friendship.query.filter(or_(Friendship.user_low == current_user.id,
                                            Friendship.user_high == current_user.id)).all()
        ids = [p.user_high if p.user_low == current_user.id else p.user_low for p in pairs]
        users = {u.id: u for u in User.query.filter(User.id.in_(ids)).all()}
        return {'friends': sorted([
            {**public_user(users[other]), 'status': p.status,
             'incoming': p.requested_by != current_user.id}
            for p, other in zip(pairs, ids) if other in users
        ], key=lambda item: item['username'].casefold())}
    data = request.get_json(silent=True)
    if not isinstance(data, dict) or set(data) != {'username'} or not isinstance(data['username'], str):
        return api_error('Es wird ausschließlich ein Benutzername erwartet.')
    username = data['username'].strip()
    if not 3 <= len(username) <= 150:
        return api_error('Der Benutzername muss 3 bis 150 Zeichen lang sein.')
    user = User.query.filter_by(username_key=normalize_username(username)).first()
    if user is None:
        return api_error('Kein Konto mit diesem Benutzernamen gefunden.', 404)
    if user.id == current_user.id:
        return api_error('Du kannst dir selbst keine Freundschaftsanfrage senden.')
    existing = pair_with(user.id)
    if existing:
        return api_error('Ihr seid bereits befreundet.' if existing.status == 'accepted'
                         else 'Zwischen euch besteht bereits eine Anfrage. Prüfe deine Anfragen.', 409)
    low, high = sorted((current_user.id, user.id))
    db.session.add(Friendship(user_low=low, user_high=high, requested_by=current_user.id))
    return commit_changes('Zwischen euch besteht bereits eine Anfrage. Bitte aktualisiere die Liste.') or ({'status': 'pending'}, 201)


@friends_bp.route('/<int:user_id>', methods=['PUT', 'DELETE'])
@login_required
def manage_friend(user_id):
    if user_id > 2**63 - 1:
        return api_error('Die Freundschaft oder Anfrage wurde nicht gefunden.', 404)
    data = request.get_json(silent=True)
    if not isinstance(data, dict) or data:
        return api_error('Es wird ein leeres JSON-Objekt erwartet.')
    pair = pair_with(user_id)
    if pair is None:
        return api_error('Die Freundschaft oder Anfrage wurde nicht gefunden.', 404)
    matching = Friendship.query.filter_by(id=pair.id, user_low=pair.user_low,
        user_high=pair.user_high, requested_by=pair.requested_by, status=pair.status)
    if request.method == 'DELETE':
        if not matching.delete():
            db.session.rollback()
            return api_error('Die Anfrage wurde zwischenzeitlich geändert. Bitte aktualisiere die Liste.', 409)
        return commit_changes() or {'status': 'deleted'}
    if pair.requested_by == current_user.id:
        return api_error('Nur der Empfänger kann die Anfrage annehmen.', 403)
    # Conditional update cannot revive an invitation concurrently removed by its sender.
    changed = matching.update({'status': 'accepted'})
    if not changed:
        db.session.rollback()
        return api_error('Die Anfrage wurde inzwischen entfernt. Bitte aktualisiere die Liste.', 409)
    return commit_changes() or {'status': 'accepted'}


@friends_bp.route('/<int:user_id>/games')
@login_required
def friend_collection(user_id):
    if user_id > 2**63 - 1:
        return api_error('Diese Sammlung ist nicht freigegeben.', 403)
    pair = pair_with(user_id)
    if pair is None or pair.status != 'accepted':
        return api_error('Diese Sammlung ist nicht freigegeben. Eine bestätigte Freundschaft ist erforderlich.', 403)
    user = db.session.get(User, user_id)
    if user is None:
        return api_error('Diese Sammlung ist nicht freigegeben.', 403)
    games = Game.query.filter_by(user_id=user_id).order_by(Game.title, Game.id).all()
    return {'user': public_user(user), 'games': [game.to_dict() for game in games]}
