from flask import Blueprint, request
from flask_login import current_user, login_required, logout_user
from werkzeug.security import check_password_hash, generate_password_hash
from ..database import api_error, commit_changes
from ..extensions import db
from ..models import User
from ..validation import normalize_username

users_bp = Blueprint('users', __name__, url_prefix='/api')
UI_THEMES = {'cinematic', 'editorial'}

def json_object():
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else None

@users_bp.route('/users', methods=['GET'])
@login_required
def get_users():
    if not current_user.is_admin:
        return api_error('Unauthorized', 403)
    return [user.to_dict() for user in User.query.all()]

@users_bp.route('/users/<int:user_id>', methods=['PUT', 'DELETE'])
@login_required
def manage_user(user_id):
    if not current_user.is_admin:
        return api_error('Unauthorized', 403)
    user = db.get_or_404(User, user_id)
    if request.method == 'DELETE':
        if user.id == current_user.id:
            return api_error('Admin kann sich nicht selbst löschen')
        db.session.delete(user)
        return commit_changes() or {'status': 'deleted'}
    data = json_object()
    if data is None:
        return api_error('Es wird ein JSON-Objekt erwartet.')
    unexpected = set(data) - {'username', 'password'}
    if unexpected:
        return api_error(f'Unbekannte Felder: {", ".join(sorted(unexpected))}.')
    if data.get('username'):
        username = data['username'].strip() if isinstance(data['username'], str) else ''
        if not 3 <= len(username) <= 150:
            return api_error('Der Benutzername muss 3 bis 150 Zeichen lang sein.')
        if User.query.filter(User.username_key == normalize_username(username), User.id != user_id).first():
            return api_error('Benutzername existiert bereits')
        user.username = username
        user.username_key = normalize_username(username)
    if data.get('password') != '' and 'password' in data:
        if not isinstance(data['password'], str) or len(data['password']) < 8:
            return api_error('Das Passwort muss mindestens 8 Zeichen lang sein.')
        user.password = generate_password_hash(data['password'])
    return commit_changes('Benutzername existiert bereits.') or {'status': 'updated'}

@users_bp.route('/account', methods=['PUT', 'DELETE'])
@login_required
def manage_account():
    data = json_object()
    if data is None:
        return api_error('Es wird ein JSON-Objekt erwartet.')
    unexpected = set(data) - {'username', 'current_password', 'new_password', 'new_password_confirm'}
    if unexpected:
        return api_error(f'Unbekannte Felder: {", ".join(sorted(unexpected))}.')
    current_password = data.get('current_password')
    if not isinstance(current_password, str) or not check_password_hash(current_user.password, current_password):
        return api_error('Das aktuelle Passwort ist nicht korrekt.', 403)
    user = current_user._get_current_object()
    if request.method == 'DELETE':
        if user.is_admin and not User.query.filter(User.id != user.id, User.is_admin.is_(True)).first():
            successor = User.query.filter(User.id != user.id).order_by(User.id).first()
            if successor:
                successor.is_admin = True
        db.session.delete(user)
        error = commit_changes()
        if error:
            return error
        logout_user()
        return {'status': 'deleted'}
    username = data.get('username')
    if not isinstance(username, str):
        return api_error('Der Benutzername ist erforderlich.')
    username = username.strip()
    if not 3 <= len(username) <= 150:
        return api_error('Der Benutzername muss 3 bis 150 Zeichen lang sein.')
    username_key = normalize_username(username)
    if User.query.filter(User.username_key == username_key, User.id != user.id).first():
        return api_error('Benutzername existiert bereits.')
    new_password = data.get('new_password') or ''
    confirmation = data.get('new_password_confirm') or ''
    if not isinstance(new_password, str) or not isinstance(confirmation, str):
        return api_error('Die Passwortfelder müssen Text enthalten.')
    if new_password and len(new_password) < 8:
        return api_error('Das neue Passwort muss mindestens 8 Zeichen lang sein.')
    if new_password and new_password != confirmation:
        return api_error('Die neuen Passwörter stimmen nicht überein.')
    user.username = username
    user.username_key = username_key
    if new_password:
        user.password = generate_password_hash(new_password)
    error = commit_changes('Benutzername existiert bereits.')
    return error or {'status': 'updated', 'username': user.username}


@users_bp.route('/account/theme', methods=['PUT'])
@login_required
def update_theme():
    data = json_object()
    if data is None or set(data) != {'theme'}:
        return api_error('Es wird ausschließlich das Feld "theme" erwartet.')
    theme = data['theme']
    if theme not in UI_THEMES:
        return api_error('Unbekannter Darstellungsmodus.')
    current_user.ui_theme = theme
    error = commit_changes()
    return error or {'status': 'updated', 'theme': theme}
