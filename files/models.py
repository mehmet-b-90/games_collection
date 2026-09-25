from flask_login import UserMixin
from .extensions import db

class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(150), unique=True, nullable=False)
    username_key = db.Column(db.String(150), unique=True, nullable=False)
    password = db.Column(db.String(255), nullable=False)
    is_admin = db.Column(db.Boolean, nullable=False, default=False)
    ui_theme = db.Column(db.String(20), nullable=False, default='cinematic', server_default='cinematic')
    games = db.relationship('Game', backref='owner', lazy='selectin', cascade='all, delete-orphan', passive_deletes=True)

    def to_dict(self):
        return {'id': self.id, 'username': self.username, 'is_admin': self.is_admin}

class Game(db.Model):
    __table_args__ = (
        db.CheckConstraint('rating IS NULL OR (rating >= 0 AND rating <= 10)', name='rating_range'),
        db.CheckConstraint("format IN ('Physisch', 'Digital')", name='valid_format'),
        db.CheckConstraint("status IN ('Nicht gespielt', 'Gespielt', 'Abgebrochen')", name='valid_status'),
        db.UniqueConstraint('user_id', 'igdb_id', name='unique_user_igdb_game'),
    )
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id', ondelete='CASCADE'), nullable=False)
    igdb_id = db.Column(db.Integer)
    igdb_url = db.Column(db.String(500))
    title = db.Column(db.String(200), nullable=False)
    cover_url = db.Column(db.String(500))
    release_date = db.Column(db.String(50))
    developer = db.Column(db.Text)
    publisher = db.Column(db.Text)
    game_engine = db.Column(db.Text)
    available_platforms = db.Column(db.Text)
    format = db.Column(db.String(50), nullable=False, default='Digital')
    rating = db.Column(db.Integer)
    status = db.Column(db.String(50), nullable=False, default='Nicht gespielt')
    age_rating = db.Column(db.String(50))
    version = db.Column(db.Integer, nullable=False, default=1)
    __mapper_args__ = {'version_id_col': version}
    owned_platforms = db.relationship('GamePlatformOwnership', backref='game', lazy='selectin', cascade='all, delete-orphan', passive_deletes=True)

    def to_dict(self):
        data = {column.name: getattr(self, column.name) for column in self.__table__.columns}
        data['owned_platform'] = ', '.join(item.platform for item in self.owned_platforms)
        return data

class GamePlatformOwnership(db.Model):
    __table_args__ = (db.UniqueConstraint('game_id', 'platform', name='unique_game_platform'),)
    id = db.Column(db.Integer, primary_key=True)
    game_id = db.Column(db.Integer, db.ForeignKey('game.id', ondelete='CASCADE'), nullable=False)
    platform = db.Column(db.String(50), nullable=False)
