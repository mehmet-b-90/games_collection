"""JSON session API for native clients; shares Flask-Login and CSRF with the web UI."""
from flask import Blueprint, jsonify, request, session
from flask_login import current_user, login_user, logout_user
from flask_wtf.csrf import generate_csrf
from werkzeug.security import check_password_hash
from ..database import api_error
from ..models import User
from ..validation import normalize_username

session_bp = Blueprint('session', __name__, url_prefix='/api')


def session_response():
    user = None
    if current_user.is_authenticated:
        user = {**current_user.to_dict(), 'ui_theme': current_user.ui_theme}
    response = jsonify({
        'api_version': 1,
        'capabilities': {'idempotent_game_writes': True},
        'authenticated': user is not None,
        'user': user,
        'csrf_token': generate_csrf(),
    })
    response.headers['Cache-Control'] = 'no-store'
    return response


@session_bp.route('/session', methods=['GET', 'POST', 'DELETE'])
def manage_session():
    if request.method == 'GET':
        # An anonymous client also needs a CSRF token before submitting its login.
        return session_response()
    if request.method == 'DELETE':
        session.clear()
        logout_user()
        return session_response()
    data = request.get_json(silent=True)
    if not isinstance(data, dict) or set(data) - {'username', 'password', 'remember'}:
        return api_error('Es werden Benutzername, Passwort und optional remember erwartet.')
    username, password = data.get('username'), data.get('password')
    remember = data.get('remember', False)
    if not isinstance(username, str) or not isinstance(password, str) or not isinstance(remember, bool):
        return api_error('Ungültige Anmeldedaten.')
    if not 1 <= len(username.strip()) <= 150 or not 1 <= len(password) <= 4096:
        return api_error('Ungültige Anmeldedaten.')
    user = User.query.filter_by(username_key=normalize_username(username)).first()
    if user is None or not check_password_hash(user.password, password):
        return api_error('Falscher Benutzername oder Passwort.', 401)
    # Rotate the previous session and its CSRF token on account changes.
    session.clear()
    logout_user()
    login_user(user, remember=remember)
    return session_response()
