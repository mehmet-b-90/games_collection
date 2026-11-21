"""Additive, transactional SQLite data migrations; never recreate existing tables."""
from .extensions import db
from .models import PLAYTIME_CONSTRAINT

COPIES_MIGRATION = '2026_11_game_copies'


def migrate_game_copies():
    # Serialize schema creation as well as the backfill. Explicit BEGIN also makes
    # SQLite DDL transactional (the driver's legacy transaction mode does not).
    with db.engine.connect() as connection:
        connection.exec_driver_sql('BEGIN IMMEDIATE')
        try:
            db.metadata.create_all(bind=connection)
            done = connection.exec_driver_sql(
                'SELECT name FROM schema_migration WHERE name = ?', (COPIES_MIGRATION,)
            ).first()
            if done is None:
                # No guessing if an unmarked, partially imported schema is present.
                if connection.exec_driver_sql('SELECT 1 FROM game_copy LIMIT 1').first():
                    raise RuntimeError('Exemplarmigration: Exemplare ohne Migrationsbeleg vorhanden. '
                                       'Datenbank vor dem Start prüfen; keine Daten geändert.')
                connection.exec_driver_sql('''
                    INSERT INTO game_copy (game_id, platform, format, edition)
                    SELECT p.game_id, p.platform, g.format, ''
                    FROM game_platform_ownership p JOIN game g ON g.id = p.game_id
                    ORDER BY p.id
                ''')
                connection.exec_driver_sql('INSERT INTO schema_migration (name) VALUES (?)', (COPIES_MIGRATION,))
            connection.commit()
        except Exception:
            connection.rollback()
            raise


def migrate_game_playtime():
    with db.engine.connect() as connection:
        connection.exec_driver_sql('BEGIN IMMEDIATE')
        try:
            columns = connection.exec_driver_sql('PRAGMA table_info(game)')
            if not any(column[1] == 'playtime_minutes' for column in columns):
                connection.exec_driver_sql(
                    'ALTER TABLE game ADD COLUMN playtime_minutes INTEGER '
                    f'CONSTRAINT playtime_range CHECK ({PLAYTIME_CONSTRAINT})'
                )
            connection.commit()
        except Exception:
            connection.rollback()
            raise
