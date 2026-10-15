from .auth import auth_bp
from .games import games_bp
from .main import main_bp
from .users import users_bp
from .session import session_bp

def register_blueprints(app):
    for blueprint in (auth_bp, main_bp, games_bp, users_bp, session_bp):
        app.register_blueprint(blueprint)
