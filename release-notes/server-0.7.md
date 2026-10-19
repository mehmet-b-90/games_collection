# Games Collection Server 0.7

Server-Veröffentlichung vom 19.10.2026 für Web/PWA und die native Android-App. Docker-Images für AMD64 und ARM64:

```bash
docker pull ghcr.io/mehmet-b-90/games_collection:0.7
docker pull ghcr.io/mehmet-b-90/games_collection:latest
```

## Änderungen

- Spielbearbeitungen versionieren jetzt auch reine Besitzplattformänderungen. Gleichzeitige Änderungen überschreiben sich nicht still; veraltete Bearbeitungen liefern HTTP 409. Idempotente Wiederholungen bleiben erhalten.
- Parallele Erstregistrierungen vergeben genau einem Konto Administratorrechte.
- Verständliche deutsche API-/HTML-Fehlermeldungen mit Fehler-ID, passenden HTTP-Statuscodes und zugehörigen Betreiberlogs. Datenbankfehler führen zum Rollback und HTTP 503. SQL-Parameter werden in Datenbank-Tracebacks verborgen.
- Strengere Typ-, Ganzzahl-, URL- und Kontofeldvalidierung; ungültige Eingaben werden ohne Mutation abgewiesen. Robuste Verarbeitung fehlerhafter IGDB-/Twitch-Antworten und Diagnose ohne externe Antwortkörper oder Tokenwerte.
- `no-store` für APIs, Fehler und persönliche HTML-Seiten; Protokollheader wie `Allow` bei HTTP 405 bleiben erhalten. Relative Datenbankpfade werden korrekt aufgelöst.
- Enthält die Server-API für Android 0.7 einschließlich `/api/igdb-token` und idempotenter Spieländerungen. Die vorhandene Android-Release-APK bleibt unverändert.

## Voraussetzungen und Update

- Docker Engine, optional Docker Compose; bestehende `.env` mit eigenem `SECRET_KEY` und Twitch-/IGDB-Zugangsdaten weiterverwenden. HTTPS im regulären Betrieb und passende Schreibrechte für UID/GID 1000 im Datenverzeichnis sicherstellen.
- Vor dem Update ein konsistentes SQLite-Backup erstellen, bei laufender Datenbank einschließlich WAL über ein geeignetes Backupverfahren.
- Die SQLite-Datenbank bleibt unter `/app/data/database.db` mit dem Bind-Mount `./data:/app/data` erhalten. Keine bestehenden Nutzdaten werden durch die Veröffentlichung verändert.
- **Alt-Datenbanken:** Die bisherige automatische Ergänzung von `user.ui_theme` ist entfernt. `create_all()` legt fehlende Tabellen an, migriert aber keine vorhandenen Spalten. Alte Datenbanken ohne die benötigten Modellspalten vor dem Update gesondert migrieren. `/health` prüft die Verbindung, nicht die Vollständigkeit des Schemas.

Mit der aktuellen Compose-Datei:

```bash
docker compose pull
docker compose up -d
```

Für eine feste Version `GAMES_COLLECTION_IMAGE=ghcr.io/mehmet-b-90/games_collection:0.7` in `.env` setzen; ohne Überschreibung verwendet Compose `latest`.

## Prüfungen

- 36 Backend-Tests im gebauten Container mit Python 3.14 und ausschließlich temporärer Datenbank bestanden, einschließlich paralleler SQLite-Schreibtests, Rollback-/Idempotenzfällen und fehlerhafter Eingaben.
- Containerstart mit Gunicorn und erfolgreicher `/health`-Prüfung; die Veröffentlichung führt diese Prüfungen vor dem Image-Upload erneut in GitHub Actions aus.
- Docker-Images für `linux/amd64` und `linux/arm64`; GitHub Release und Tag werden erst nach erfolgreicher Image-Veröffentlichung erzeugt.

## Bekannte Einschränkungen

- Keine allgemeine Datenbankmigration oder automatische Backupstrategie. DELETE und Kontobearbeitungen besitzen weiterhin keine optimistische Versionsprüfung.
- Live-Twitch-/IGDB-, Geräte-, Last-/Dauer- und visuelle Prüfungen beider Designs stehen aus. ARM64 wird gebaut; der Container-Laufzeittest erfolgt auf AMD64.
- Flask-Login meldet unter Python 3.14 eine bestehende `datetime.utcnow()`-DeprecationWarning. Weitere Details stehen in `SERVER_REVIEW.md`.
