"""Server administration without modifications to the production schema."""
from contextlib import closing
from datetime import datetime, timezone
import os
import shutil
import sqlite3
import tempfile
import time

from flask import Blueprint, send_file
from flask_login import current_user, login_required

from ..database import api_error, log_exception
from ..extensions import db

admin_bp = Blueprint('admin', __name__, url_prefix='/api/admin')


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
