from flask import Blueprint, redirect, render_template, request, url_for
from flask_login import login_required, login_user, logout_user
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from werkzeug.security import check_password_hash, generate_password_hash
from ..extensions import db
from ..database import log_exception, request_id
from ..models import User
from ..validation import normalize_username

auth_bp = Blueprint('auth', __name__)

@auth_bp.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        username = (request.form.get('username') or '').strip()
        password = request.form.get('password') or ''
        password_confirm = request.form.get('password_confirm')
        if not 3 <= len(username) <= 150:
            return render_template('auth.html', action='register', error='Der Benutzername muss 3 bis 150 Zeichen lang sein.')
        if not 8 <= len(password) <= 4096:
            return render_template('auth.html', action='register', error='Das Passwort muss 8 bis 4096 Zeichen lang sein.')
        if password != password_confirm:
            return render_template('auth.html', action='register', error='Die Passwörter stimmen nicht überein.')
        if User.query.filter_by(username_key=normalize_username(username)).first():
            return render_template('auth.html', action='register', error='Benutzername existiert bereits.')
        # Evaluate administrator bootstrap inside the INSERT, so concurrent
        # first registrations cannot both observe an empty user table.
        new_user = User(username=username, username_key=normalize_username(username), password=generate_password_hash(password), is_admin=~db.select(User.id).exists())
        db.session.add(new_user)
        try:
            db.session.commit()
        except IntegrityError:
            db.session.rollback()
            return render_template('auth.html', action='register', error='Benutzername existiert bereits.')
        except SQLAlchemyError:
            db.session.rollback()
            log_exception('Registrierung fehlgeschlagen')
            return render_template('auth.html', action='register', error=f'Die Registrierung konnte nicht gespeichert werden. Bitte versuche es später erneut. Fehler-ID: {request_id()}'), 503
        login_user(new_user)
        return redirect(url_for('main.index'))
    return render_template('auth.html', action='register')

@auth_bp.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = normalize_username(request.form.get('username') or '')
        password = request.form.get('password') or ''
        # Existing accounts may have passwords longer than the new write limit.
        if not 1 <= len(username) <= 150 or not password:
            return render_template('auth.html', action='login', error='Falscher Benutzername oder Passwort.')
        user = User.query.filter_by(username_key=username).first()
        if user and check_password_hash(user.password, password):
            login_user(user, remember=bool(request.form.get('remember')))
            return redirect(url_for('main.index'))
        return render_template('auth.html', action='login', error='Falscher Benutzername oder Passwort.')
    return render_template('auth.html', action='login')

@auth_bp.route('/logout', methods=['POST'])
@login_required
def logout():
    logout_user()
    return redirect(url_for('auth.login'))
