from uuid import uuid4

from flask import current_app, g, jsonify, request
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from sqlalchemy.orm.exc import StaleDataError
from .extensions import db

def request_id():
    if 'request_id' not in g:
        g.request_id = uuid4().hex
    return g.request_id


def log_exception(message):
    # Log the route template, never query strings, payloads or authentication headers.
    current_app.logger.exception('%s [Fehler-ID: %s; %s %s]', message, request_id(),
                                 request.method, request.url_rule.rule if request.url_rule else '(unbekannte Route)')


def api_error(message, status=400):
    if status >= 500:
        message = f'{message} Fehler-ID: {request_id()}'
    return jsonify({'error': message, 'request_id': request_id()}), status

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
        log_exception('Datenbankoperation fehlgeschlagen')
        return api_error('Die Datenbank ist derzeit nicht verfügbar. Bitte versuche es später erneut.', 503)
