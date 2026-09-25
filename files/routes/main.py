from flask import Blueprint, current_app, jsonify, render_template, send_from_directory
from flask_login import current_user, login_required
from flask_wtf.csrf import generate_csrf
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from ..config import PLATFORMS
from ..extensions import db
from ..models import Game

main_bp = Blueprint('main', __name__)

@main_bp.route('/health')
def health():
    try:
        db.session.execute(text('SELECT 1'))
        return jsonify({'status': 'ok'})
    except SQLAlchemyError:
        db.session.rollback()
        return jsonify({'status': 'unhealthy'}), 503

@main_bp.route('/api/csrf-token')
@login_required
def csrf_token():
    response = jsonify({'csrf_token': generate_csrf()})
    response.headers['Cache-Control'] = 'no-store'
    return response

@main_bp.route('/service-worker.js')
def service_worker():
    response = send_from_directory(current_app.static_folder, 'service-worker.js')
    response.headers['Cache-Control'] = 'no-cache'
    response.headers['Service-Worker-Allowed'] = '/'
    return response

@main_bp.route('/')
@login_required
def index():
    games = Game.query.filter_by(user_id=current_user.id).all()
    engines = sorted({engine.strip() for game in games if game.game_engine for engine in game.game_engine.split(',') if engine.strip()})
    ratings = [game.rating for game in games if game.rating is not None]
    owned_platforms = {item.platform for game in games for item in game.owned_platforms}
    summary = {
        'games': len(games),
        'average_rating': round(sum(ratings) / len(ratings), 1) if ratings else None,
        'platforms': len(owned_platforms),
    }
    return render_template(
        'index.html', games=[game.to_dict() for game in games], engines=engines,
        platforms=sorted(PLATFORMS), summary=summary,
    )
