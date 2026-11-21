"""Administration actions for server operators."""
from contextlib import closing
from datetime import datetime, timezone
import os
import shutil
import sqlite3
import tempfile
import threading
import time

import requests
from flask import Blueprint, current_app, request, send_file
from flask_login import current_user, login_required
from sqlalchemy.dialects.sqlite import insert

from ..database import api_error, log_exception
from ..extensions import db
from ..igdb import fetch_game_durations, log_upstream_failure
from ..igdb_playtime import store_game_durations
from ..models import Game, IgdbPlaytime, SchemaMigration

admin_bp = Blueprint('admin', __name__, url_prefix='/api/admin')
PLAYTIME_IMPORT_VERSION = '0.12'
PLAYTIME_IMPORT_MARKER = '2026_11_igdb_playtime_import'
PLAYTIME_BATCH_SIZE = 50
_playtime_import_lock = threading.Lock()


def playtime_import_available():
    return (current_app.config['APP_VERSION'] == PLAYTIME_IMPORT_VERSION
            and db.session.get(SchemaMigration, PLAYTIME_IMPORT_MARKER) is None)


@admin_bp.post('/igdb-playtime/import')
@login_required
def import_igdb_playtime():
    if not current_user.is_admin:
        return api_error('Du hast keine Berechtigung für diese Aktion.', 403)
    if current_app.config['APP_VERSION'] != PLAYTIME_IMPORT_VERSION:
        return api_error('Der einmalige IGDB-Spielzeitimport ist in dieser Version nicht verfügbar.', 404)
    if request.args or (request.data and request.get_json(silent=True) != {}):
        return api_error('Für den Import werden keine zusätzlichen Angaben erwartet.')
    if not _playtime_import_lock.acquire(blocking=False):
        return api_error('Ein Spielzeitimport läuft bereits. Bitte warte kurz.', 409)
    try:
        if not playtime_import_available():
            return {'processed': 0, 'done': True}
        pending = db.session.scalars(
            db.select(Game.igdb_id).distinct().outerjoin(IgdbPlaytime, Game.igdb_id == IgdbPlaytime.igdb_id)
            .where(Game.igdb_id > 0, IgdbPlaytime.igdb_id.is_(None))
            .order_by(Game.igdb_id).limit(PLAYTIME_BATCH_SIZE + 1)
        ).all()
        batch = pending[:PLAYTIME_BATCH_SIZE]
        if batch:
            store_game_durations(fetch_game_durations(batch))
        done = len(pending) <= PLAYTIME_BATCH_SIZE
        if done:
            db.session.execute(insert(SchemaMigration).values(name=PLAYTIME_IMPORT_MARKER).on_conflict_do_nothing())
        db.session.commit()
        return {'processed': len(batch), 'done': done}
    except (requests.RequestException, ValueError) as error:
        log_upstream_failure('IGDB-Spielzeitimport fehlgeschlagen', error)
        return api_error('Der IGDB-Abruf ist fehlgeschlagen. Starte den Import erneut, um ihn fortzusetzen.', 502)
    finally:
        _playtime_import_lock.release()


@admin_bp.post('/backup')
@login_required
def export_database():
    if not current_user.is_admin:
        return api_error('Du hast keine Berechtigung für diese Aktion.', 403)

    download = None
    try:
        # SQLite's backup API includes committed WAL data and produces a coherent
        # standalone database while other connections continue using the source.
        with tempfile.TemporaryDirectory(prefix='games-collection-backup-') as directory:
            path = os.path.join(directory, 'database.db')
            deadline = time.monotonic() + 20

            def progress(status, remaining, total):
                if time.monotonic() > deadline:
                    raise TimeoutError('SQLite backup deadline exceeded')

            source = db.engine.raw_connection()
            try:
                with closing(sqlite3.connect(path)) as destination:
                    source.driver_connection.backup(destination, pages=256, progress=progress, sleep=0.05)
            finally:
                source.close()
            # Anonymous temporary file stays open for streaming, and is removed
            # automatically when the WSGI server closes the response iterable.
            download = tempfile.TemporaryFile()
            with open(path, 'rb') as snapshot:
                shutil.copyfileobj(snapshot, download)
            size = download.tell()
            download.seek(0)
        stamp = datetime.now(timezone.utc).strftime('%Y-%m-%d_%H-%M-%S_UTC')
        response = send_file(download, mimetype='application/vnd.sqlite3', as_attachment=True,
                             download_name=f'Games-Collection-Backup-{stamp}.db', conditional=False, etag=False)
        response.content_length = size
        response.headers['X-Content-Type-Options'] = 'nosniff'
        return response
    except (OSError, sqlite3.Error, TimeoutError):
        if download is not None:
            download.close()
        log_exception('Datenbank-Backup fehlgeschlagen')
        return api_error('Das Backup konnte nicht erstellt werden. Bitte versuche es später erneut.', 503)
