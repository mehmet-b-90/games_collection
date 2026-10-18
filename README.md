# Games Collection

Games Collection ist eine selbst gehostete Webanwendung zur Verwaltung einer persönlichen Videospielesammlung. Spieldaten und Cover können über IGDB gesucht werden. Die Anwendung bietet zwei Designs, Filter, Statistiken, Benutzerkonten und eine installierbare PWA-Oberfläche.

Dank Docker Container ist die Einrichtung einfach.

## Voraussetzungen

- Docker Engine
- Docker Compose (optional)
- Twitch-/IGDB-Zugangsdaten für die Spielesuche

## Einrichtung

1. Beispieldatei für die Konfiguration kopieren:

   ```bash
   cp .env.example .env
   ```

2. Die Werte in `.env` entsprechend dem Abschnitt [Konfiguration](#konfiguration) eintragen.

3. Fertiges Image aus der GitHub Container Registry laden und starten:

   ```bash
   docker compose pull
   docker compose up -d
   ```

4. `http://localhost:7000` öffnen. Der zuerst angelegte Benutzer erhält Administratorrechte.

Die SQLite-Datenbank wird im lokalen Verzeichnis `data/` abgelegt und nicht in das Repository übernommen.

## Docker-Image und Updates

Das Server-Image liegt unter `ghcr.io/mehmet-b-90/games_collection`. Es unterstützt `linux/amd64` (übliche PCs/Server) und `linux/arm64` (beispielsweise ARM-NAS und Raspberry Pi mit 64-Bit-System). Docker wählt die passende Architektur automatisch.

```bash
docker pull ghcr.io/mehmet-b-90/games_collection:latest
```

`latest` folgt dem zuletzt erfolgreich veröffentlichten Serverstand von `main`. Mit der mitgelieferten Compose-Datei aktualisierst du eine bestehende Installation so:

```bash
docker compose pull
docker compose up -d
```

Die vorhandene `.env` und das eingebundene Verzeichnis `data/` werden weiterverwendet. Vor Updates eine Sicherung der SQLite-Datenbank erstellen; eine laufende SQLite-Datenbank mit WAL nicht durch bloßes Kopieren nur der Datei `database.db` sichern.

Der Container läuft standardmäßig mit UID/GID `1000:1000`. Das eingebundene `data/` muss für diesen Benutzer schreibbar sein. Bei einer Neuinstallation das Verzeichnis vorher anlegen und passende Besitzrechte setzen. Auf SELinux-Systemen wie Fedora kann für den Bind-Mount zusätzlich `./data:/app/data:Z` erforderlich sein.

Für einen bestimmten Quellstand gibt es außerdem `sha-<vollständige-Commit-ID>`. Eine feste Version lässt sich über `GAMES_COLLECTION_IMAGE` auswählen, zum Beispiel mit einem tatsächlich veröffentlichten SHA-Tag:

```bash
GAMES_COLLECTION_IMAGE=ghcr.io/mehmet-b-90/games_collection:sha-DEINE_COMMIT_ID docker compose up -d
```

### Automatische Veröffentlichung

Der [GitHub-Actions-Workflow](https://github.com/mehmet-b-90/games_collection/actions/workflows/docker-publish.yml) baut bei relevanten Serveränderungen auf `main` zuerst ein Testimage. Darin laufen die Backend-Tests mit temporärer Datenbank und ein Starttest mit dem Docker-Healthcheck. Erst danach veröffentlicht er das Image für beide Architekturen. Pull Requests werden geprüft, veröffentlichen aber kein Image. Der Workflow lässt sich auf `main` auch manuell über **Actions → Build and publish Docker image → Run workflow** starten.

Tags mit dem Präfix `docker-v` erzeugen zusätzliche Docker-Versionstags: beispielsweise würde `docker-v1.2.3` das Image `:1.2.3` veröffentlichen. `latest` bleibt dem `main`-Branch vorbehalten. Android-Tags wie `android-v0.6` lösen keine Docker-Veröffentlichung aus.

Die Anmeldung an der Registry erfolgt mit dem automatisch bereitgestellten `GITHUB_TOKEN` und der Berechtigung `packages: write`; ein eigenes Registry-Passwort als Repository-Secret ist nicht erforderlich. GitHub legt neue Container-Pakete zunächst **privat** an. Nach der ersten Veröffentlichung muss der Besitzer einmalig unter **Packages → games_collection → Package settings → Change visibility → Public** die öffentliche Sichtbarkeit einstellen, damit `docker pull` ohne Anmeldung funktioniert. Siehe [GitHubs Registry-Dokumentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

### Lokal aus dem Quellcode bauen

Für eigene Änderungen kannst du weiterhin ein lokales Image bauen und es mit derselben Compose-Datei verwenden:

```bash
docker build -t games-collection:local .
GAMES_COLLECTION_IMAGE=games-collection:local docker compose up -d --pull never
```

Der Docker-Buildkontext enthält ausschließlich Dockerfile, `requirements.txt` und die Anwendung unter `files/`. Lokale Konfiguration, Datenbank, Git-Verlauf und Android-Werkzeuge werden nicht an den Builder übertragen.

## Konfiguration

Die Anwendung liest ihre Konfiguration aus der Datei `.env`. Die Datei enthält Geheimnisse und darf deshalb nicht in das Repository aufgenommen oder öffentlich weitergegeben werden.

```env
SECRET_KEY=ein-langer-zufaelliger-wert
TWITCH_CLIENT_ID=deine-twitch-client-id
TWITCH_CLIENT_SECRET=dein-twitch-client-secret
SESSION_COOKIE_SECURE=false
```

### Flask-Schlüssel

`SECRET_KEY` gehört zur lokalen Flask-Anwendung und **nicht** zur IGDB-API. Flask verwendet diesen Wert, um unter anderem Sitzungs-Cookies und CSRF-Tokens kryptografisch zu signieren. Dadurch kann der Server erkennen, ob diese Werte nachträglich verändert wurden.

Für jede Installation sollte ein eigener, langer und zufälliger Schlüssel verwendet werden. Mit Python lässt sich ein geeigneter Wert so erzeugen:

```bash
python3 -c "import secrets; print(secrets.token_hex(32))"
```

Alternativ kann OpenSSL verwendet werden:

```bash
openssl rand -hex 32
```

Beide Befehle erzeugen 32 kryptografisch sichere Zufallsbytes und geben sie als 64 Hexadezimalzeichen aus. Der ausgegebene Wert wird anschließend ohne Anführungszeichen hinter `SECRET_KEY=` in die `.env`-Datei eingetragen.

Wird `SECRET_KEY` bei einer laufenden Installation geändert, werden bestehende Benutzersitzungen ungültig und die Benutzer müssen sich erneut anmelden.

### IGDB-Authentifizierung

IGDB verwendet die Anwendungsverwaltung und OAuth-Authentifizierung von Twitch. Für die Spielesuche benötigt Games Collection daher genau zwei Zugangsdaten einer registrierten Twitch-Anwendung:

- `TWITCH_CLIENT_ID`: die öffentliche Kennung der registrierten Anwendung
- `TWITCH_CLIENT_SECRET`: das geheime Kennwort der Anwendung

Ein Twitch- oder IGDB-Benutzername und das persönliche Kontopasswort werden **nicht** benötigt und dürfen nicht in `.env` eingetragen werden.

Beim ersten Zugriff auf die Spielesuche sendet der Server Client-ID und Client-Secret an den OAuth-Dienst von Twitch. Mit dem OAuth-Verfahren `client_credentials` erhält er ein zeitlich begrenztes Access-Token. Anschließend sendet Games Collection dieses Token zusammen mit der Client-ID an IGDB. Das Token wird vorübergehend im Arbeitsspeicher zwischengespeichert und nach Ablauf automatisch neu angefordert.

```text
Client-ID + Client-Secret
          │
          ▼
    Twitch OAuth
          │
          ▼
zeitlich begrenztes Access-Token
          │
          ▼
       IGDB API
```

Die Client-ID und das Client-Secret können nach der Registrierung einer Anwendung im [Twitch Developer Portal](https://dev.twitch.tv/console/apps) erzeugt werden. Weitere Details beschreibt die [IGDB-API-Dokumentation](https://api-docs.igdb.com/).

### Sichere Cookies

`SESSION_COOKIE_SECURE` bestimmt, ob der Browser das Sitzungs-Cookie ausschließlich über HTTPS übertragen darf:

- `false`: für lokale Tests über `http://localhost:7000`
- `true`: für den produktiven Betrieb über HTTPS, beispielsweise hinter Caddy

Für eine öffentlich oder im Netzwerk erreichbare Installation mit HTTPS sollte der Wert auf `true` gesetzt werden.

## Android-App

[Android-App 0.7 direkt als APK herunterladen](https://github.com/mehmet-b-90/games_collection/releases/download/android-v0.7/Games-Collection-0.7-release.apk)

Die APK ist eine vorläufige Testversion für Android 8.0 oder neuer. Bei der Installation muss gegebenenfalls die Installation aus dieser Quelle zugelassen werden. Für die Verbindung benötigt der Server die JSON-API dieser Projektversion.

Im Ordner [`android/`](android/README.md) liegt eine eigenständige native Android-App mit Kotlin und Jetpack Compose. Sammlung, Filter, Statistik, Anmeldung und Spielverwaltung werden direkt in der App dargestellt. Die App kommuniziert per JSON-API mit dem Docker-Server; eine WebView wird nicht verwendet. Die beiden Designs Cinematic Dark und Editorial Shelf stehen auch nativ zur Verfügung. Vor der ersten Verbindung muss der Server auf das Image dieser Projektversion aktualisiert werden. Hinweise zur Installation, zum APK-Build und zu HTTPS stehen in der [Android-Anleitung](android/README.md).

Der aktuelle Android-Quellstand enthält zusätzliche Stabilitätskorrekturen für Entwürfe, HTTP-Abbruch und große Sammlungen. Für sicheres Wiederholen von Spieländerungen muss auch der Server aktualisiert werden; er ergänzt eine Tabelle für Vorgangsbelege. Version 0.7 enthält diese Korrekturen, die Offline-Sammlung und direkte IGDB-Suche. Für die Tokenbereitstellung den Server auf diesen Quellstand aktualisieren. Beim Wechsel von einer Debug-APK den Signaturhinweis in der Android-Anleitung beachten.

## Technischer Aufbau

- Flask und Gunicorn
- SQLAlchemy mit SQLite
- Bootstrap sowie modulares JavaScript
- Docker Compose
- PWA mit Service Worker und Web-App-Manifest
