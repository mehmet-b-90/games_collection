from flask import Flask, redirect, render_template, request, url_for
from flask_wtf.csrf import CSRFError
from werkzeug.exceptions import HTTPException
from sqlalchemy.exc import SQLAlchemyError
from .config import configure_app
from .database import api_error, log_exception, request_id
from .extensions import csrf, db, login_manager
from .models import User
from .routes import register_blueprints

def create_app():
    application = Flask(__name__)
    configure_app(application)
    db.init_app(application)
    csrf.init_app(application)
    login_manager.init_app(application)
    login_manager.login_view = 'auth.login'
    register_blueprints(application)

    @login_manager.unauthorized_handler
    def unauthorized():
        if request.path.startswith('/api/'):
            return api_error('Bitte melde dich erneut an.', 401)
        return redirect(url_for('auth.login'))

    @login_manager.user_loader
    def load_user(user_id):
        try:
            numeric_id = int(user_id)
        except (ValueError, TypeError):
            return None
        return db.session.get(User, numeric_id) if 0 < numeric_id <= 2**63 - 1 else None

    @application.after_request
    def response_headers(response):
        response.headers['X-Request-ID'] = request_id()
        if request.path.startswith('/api/') or response.status_code >= 400 or request.endpoint in {'main.index', 'auth.login', 'auth.register'}:
            response.headers['Cache-Control'] = 'no-store'
        return response

    def server_error(message, status):
        if request.path.startswith('/api/'):
            return api_error(message, status)
        return render_template('error.html', message=f'{message} Fehler-ID: {request_id()}'), status

    @application.errorhandler(SQLAlchemyError)
    def handle_database_error(error):
        db.session.rollback()
        log_exception('Datenbankzugriff fehlgeschlagen')
        return server_error('Die Datenbank ist derzeit nicht verfügbar. Bitte versuche es später erneut.', 503)

    @application.errorhandler(Exception)
    def handle_unexpected_error(error):
        db.session.rollback()
        log_exception('Unerwarteter Serverfehler')
        return server_error('Ein unerwarteter Serverfehler ist aufgetreten. Bitte lade die Daten vor einem erneuten Versuch neu.', 500)

    @application.errorhandler(CSRFError)
    def handle_csrf_error(error):
        if request.path.startswith('/api/'):
            response, status = api_error('Ungültiges oder fehlendes CSRF-Token.', 400)
            response.headers['X-CSRF-Error'] = '1'
            return response, status
        return render_template('error.html', message='Das Sicherheitstoken ist abgelaufen oder ungültig. Bitte lade die Seite neu.'), 400

    @application.errorhandler(HTTPException)
    def handle_http_error(error):
        if request.path.startswith('/api/'):
            messages = {400: 'Die Anfrage ist ungültig.', 404: 'Der angeforderte Datensatz oder die Seite wurde nicht gefunden.',
                        405: 'Diese Aktion ist für die Adresse nicht erlaubt.', 413: 'Die Anfrage ist zu groß (maximal 64 KiB).'}
            response = error.get_response()
            body, status = api_error(messages.get(error.code, 'Die Anfrage konnte nicht verarbeitet werden.'), error.code or 500)
            # Preserve protocol headers such as Allow on HTTP 405.
            response.set_data(body.get_data())
            response.content_type = 'application/json'
            response.status_code = status
            return response
        return error

    with application.app_context():
        db.create_all()
    return application

app = create_app()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

# Rückwärtskompatible Exporte für Tests und bestehende Importe.
from .igdb import get_preferred_age_rating  # noqa: E402,F401
from .models import Game, GamePlatformOwnership  # noqa: E402,F401
