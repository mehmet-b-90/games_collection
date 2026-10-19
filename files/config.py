import os

PLATFORMS = {
    'Amazon Games', 'Battle.net', 'EA app', 'Epic Games Store', 'GOG.com',
    'Nintendo 3DS', 'Nintendo DS', 'Nintendo Entertainment System (NES)',
    'Super Nintendo Entertainment System (SNES)', 'Nintendo 64 (N64)',
    'GameCube', 'Game Boy', 'Game Boy Color', 'Game Boy Advance',
    'Sega Game Gear', 'Sega Mega Drive', 'Sega Dreamcast', 'Sega Mega CD',
    'Sega Master System', 'Wii', 'Wii U', 'PlayStation 4', 'PlayStation 5',
    'Rockstar Games Launcher', 'Steam', 'Switch', 'Switch 2',
    'Ubisoft Connect', 'XBOX One', 'XBOX PC', 'XBOX Series'
}

def configure_app(app):
    secret_key = os.environ.get('SECRET_KEY')
    if not secret_key:
        raise RuntimeError('SECRET_KEY muss als Umgebungsvariable gesetzt sein.')
    database_path = os.path.abspath(os.environ.get('DATABASE_PATH', '/app/data/database.db'))
    os.makedirs(os.path.dirname(database_path), exist_ok=True)
    app.config.update(
        SECRET_KEY=secret_key,
        SQLALCHEMY_DATABASE_URI=f'sqlite:///{database_path}',
        SQLALCHEMY_TRACK_MODIFICATIONS=False,
        SQLALCHEMY_ENGINE_OPTIONS={'connect_args': {'timeout': 15}, 'hide_parameters': True},
        MAX_CONTENT_LENGTH=64 * 1024,
        SESSION_COOKIE_HTTPONLY=True,
        SESSION_COOKIE_SAMESITE='Lax',
        SESSION_COOKIE_SECURE=os.environ.get('SESSION_COOKIE_SECURE', '').lower() in {'1', 'true', 'yes'},
        TWITCH_CLIENT_ID=os.environ.get('TWITCH_CLIENT_ID', ''),
        TWITCH_CLIENT_SECRET=os.environ.get('TWITCH_CLIENT_SECRET', ''),
    )
