from flask import current_app, jsonify
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from sqlalchemy.orm.exc import StaleDataError
from .extensions import db

def api_error(message, status=400):
    return jsonify({'error': message}), status

def commit_changes(integrity_message='Die Daten konnten wegen eines Konflikts nicht gespeichert werden.'):
    try:
        db.session.commit()
        return None
    except StaleDataError:
        db.session.rollback()
        return api_error('Der Datensatz wurde zwischenzeitlich geändert. Bitte lade die Seite neu.', 409)
    except IntegrityError:
        db.session.rollback()
        return api_error(integrity_message, 409)
    except SQLAlchemyError:
        db.session.rollback()
        current_app.logger.exception('Datenbankoperation fehlgeschlagen')
        return api_error('Die Datenbankoperation ist fehlgeschlagen.', 503)
