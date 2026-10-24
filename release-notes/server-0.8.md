# Games Collection Server 0.8

Server-Release vom 24.10.2026 aus `feature/popscore`. Tag `docker-v0.8`; Docker-Image `ghcr.io/mehmet-b-90/games_collection:0.8` für AMD64 und ARM64. `latest` folgt weiterhin `main` und wird durch diesen Tag nicht aktualisiert.

## Änderungen

- Neuer Bereich **Entdecken / Aktuell beliebt** im Web mit bis zu 100 IGDB-Titeln nach Seitenaufrufen, responsiven Coverkarten in Cinematic Dark und Editorial Shelf und Besitzmarkierung. Keine beliebten Titel in der Sammlung.
- „+ Hinzufügen“ öffnet den bestehenden vorausgefüllten Editor. IGDB-Bewertungen und verfügbare Plattformen werden nicht als persönliche Bewertung oder Besitz übernommen.
- Authentifiziertes `GET /api/discover` für Web und den neuen nativen Android-Quellstand. Gemeinsamer Prozesscache: 24 Stunden frisch, bei Fehlern gekennzeichnete Ersatzdaten bis sieben Tage, 60 Sekunden Fehler-Backoff.
- Neues verbindliches Projekt-Icon und aktualisierte PWA-Ressourcen. Der Commit enthält außerdem die dokumentierten Android-UI-/Verbindungsverbesserungen; Android bleibt 0.7 / Code 4, ohne neue APK-/Play-Veröffentlichung.

## Voraussetzungen und Update

- Vorhandene Twitch-/IGDB-Konfiguration, eigenes `SECRET_KEY`, passende Datenverzeichnisrechte und HTTPS im regulären Betrieb weiterverwenden.
- Vor einem Serverupdate konsistentes SQLite-Backup erstellen. Daten liegen weiterhin unter `/app/data/database.db`; keine Schemaänderung gegenüber Server 0.7. `create_all()` ist keine allgemeine Migration; ältere Datenbanken benötigen gegebenenfalls separate Anpassungen.
- Nach erfolgreichem GitHub-Workflow in `docker-compose.yaml` den `image:`-Eintrag ausdrücklich auf `ghcr.io/mehmet-b-90/games_collection:0.8` setzen, dann `docker compose pull` und `docker compose up -d` ausführen. `GAMES_COLLECTION_IMAGE` wird von dieser Compose-Datei nicht ausgewertet.
- Die Veröffentlichung aktualisiert keinen laufenden Produktivserver. Für den neuen Android-Bereich wird dieser Serverendpunkt benötigt; die bestehende veröffentlichte Android-APK bleibt unverändert.

## Prüfungen

- Lokales Docker-Format-Image mit Podman gebaut; alle 44 Backendtests darin mit Netzwerkabschaltung und ausschließlich temporären Testdaten bestanden.
- Gunicorn im isolierten temporären Container gestartet; `/health` liefert HTTP 200 und `status: ok`.
- GitHub Actions wiederholt Backendtests und Healthcheck vor dem AMD64-/ARM64-Upload; GitHub Release Notes werden anschließend automatisch veröffentlicht.
- Bereits bei der Featureumsetzung: Live-IGDB lieferte 100 Titel; Web in beiden Designs auf Desktop und bei 390 px einschließlich Hinzufügen/Speichern geprüft. Einzelheiten in `SERVER_REVIEW.md`.

## Bekannte Einschränkungen

- Rangfolge entspricht IGDB-Seitenaufrufen und ist nicht als identisch mit der IGDB-Startseite garantiert. Cache nur im Prozessspeicher, nach Neustart leer.
- Kein erneuter Live-IGDB-/Web-Sichttest bei dieser Übergabe. Android-Geräte-/Lifecycle-/Offline-UI-Prüfung für Entdecken bleibt offen. ARM64 wird in CI gebaut, lokal wurde AMD64 geprüft.
- Keine allgemeine Datenbankmigration oder automatische Backupstrategie. Bestehende Flask-Login-DeprecationWarning unter Python 3.14.
