from flask import Flask, request
from flask_wtf.csrf import CSRFError
from .config import configure_app
from .database import api_error
from .extensions import csrf, db, login_manager
from .models import User
from .routes import register_blueprints
from .schema import ensure_compatible_schema

def create_app():
    application = Flask(__name__)
    configure_app(application)
    db.init_app(application)
    csrf.init_app(application)
    login_manager.init_app(application)
    login_manager.login_view = 'auth.login'
    register_blueprints(application)

    @login_manager.user_loader
    def load_user(user_id):
        return db.session.get(User, int(user_id))

    @application.errorhandler(CSRFError)
    def handle_csrf_error(error):
        if request.path.startswith('/api/'):
            response, status = api_error('Ungültiges oder fehlendes CSRF-Token.', 400)
            response.headers['X-CSRF-Error'] = '1'
            return response, status
        return str(error.description), 400

    with application.app_context():
        db.create_all()
        ensure_compatible_schema()
    return application

app = create_app()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

# Rückwärtskompatible Exporte für Tests und bestehende Importe.
from .igdb import get_preferred_age_rating  # noqa: E402,F401
from .models import Game, GamePlatformOwnership  # noqa: E402,F401
