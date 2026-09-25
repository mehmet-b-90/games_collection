from sqlalchemy import inspect, text

from .extensions import db


def ensure_compatible_schema():
    """Ergänzt kleine, rückwärtskompatible Schemaänderungen ohne Datenverlust."""
    inspector = inspect(db.engine)
    if 'user' not in inspector.get_table_names():
        return
    columns = {column['name'] for column in inspector.get_columns('user')}
    if 'ui_theme' not in columns:
        db.session.execute(text(
            "ALTER TABLE user ADD COLUMN ui_theme VARCHAR(20) NOT NULL DEFAULT 'cinematic'"
        ))
        db.session.commit()
