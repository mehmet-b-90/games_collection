# Games Collection – Projektkontext

Stand: **16.10.2026**. Grundlage sind der lokal eingesehene Quellcode und die in dieser Sitzung zugängliche Konversation. Dies ist keine vollständige neue Sicherheitsprüfung. Frühere Assistentenantworten sind nur teilweise verfügbar; insbesondere lässt sich der Inhalt früherer nummerierter Optimierungslisten nicht vollständig rekonstruieren.

## Ziel und aktueller Arbeitsstand

Selbst gehostete Verwaltung persönlicher Videospielesammlungen mit mehreren Benutzerkonten, Weboberfläche/PWA und nativer Android-App. Produktname weiterhin Games Collection.

- Git-Remote lokal: `origin` → `https://github.com/mehmet-b-90/games_collection.git`.
- Ausgangscommit vor dieser Übergabe: `6bb0871` – `Add native Android app 0.6 and session API`. Den aktuellen Commit und Remote-Abgleich mit `git log -1` und `git status -sb` prüfen.
- Android: Version **0.6**, Code **3**; Compose-Image-Tag unabhängig davon: `mehmet_b_90/games_collection:v0.7`.
- Diese Übergabe umfasst die jüngsten Android-UI-Änderungen, `FormTextField.kt`, die lokale Covergrößenpersistenz, den Ausschluss des Werkzeugcaches und die Projektdokumentation. Der Benutzer hat Commit und Push dieses Quellstands beauftragt; keine neue App-Version oder Release-APK.
- Der README-Link verweist weiterhin auf die frühere Release-APK `android-v0.6`. Ein Push des Quellcodes ersetzt diese Datei nicht; sie enthält nicht automatisch die jüngsten UI-Änderungen.

## Architektur und Struktur

```text
Webbrowser / PWA ── HTML + JavaScript / JSON ─┐
                                            ├─ Flask + Gunicorn ─ SQLAlchemy ─ SQLite
Android (Kotlin/Compose) ── JSON + Cookies ───┘          │
                                                       └─ Twitch OAuth → IGDB
```

- `files/app.py`: App-Factory `create_app()`, Extensions, Blueprints, Fehlerbehandlung, Schema-Initialisierung; exportiertes `app` für Gunicorn.
- `files/config.py`: Umgebungsvariablen, Cookie-/DB-Konfiguration, kanonische Besitzplattformliste.
- `files/extensions.py`: SQLAlchemy, Flask-Login, Flask-WTF; SQLite-PRAGMAs.
- `files/models.py`: Benutzer, Spiele und Besitzplattformen.
- `files/validation.py`: Benutzernamennormalisierung, Payload-/URL-/Wertevalidierung.
- `files/database.py`: Commit-/Rollback-Helfer und JSON-Fehler.
- `files/schema.py`: kleine bestehende Kompatibilitätsergänzung für `user.ui_theme`, kein allgemeines Migrationstool.
- `files/igdb.py`: OAuth-Token-Cache und USK-/PEGI-Auswahl.
- `files/routes/`: `auth` (HTML-Anmeldung/Registrierung), `main` (Seite/Health/PWA/CSRF), `games`, `users`, `session` (native JSON-Anmeldung).
- `files/templates/`: Jinja-Templates `index.html`, `auth.html`.
- `files/static/`: gemeinsames CSS, Logos, PWA-Dateien und ES-Module in `js/`: `main`, `ui`, `api`, `games`, `igdb-search`, `account`, `admin`, `charts`, `pwa`.
- `tests/test_app.py`: Backend-/Frontend-Vertragstests mit `unittest`, temporärer DB und gemockten externen Aufrufen.
- `android/`: eigenständiges Gradle-Projekt; Anleitung in `android/README.md`.
- Android-Package: `android/app/src/main/java/de/gamescollection/viewer/`; `MainActivity`, `CollectionUi`, `FormTextField`, `CollectionViewModel`, `ApiClient`, `SessionStore`, `Models`, `NativeIcons`, `ServerAddress`.
- `Dockerfile`, `docker-compose.yaml`, `requirements.txt`, `.env.example`: Serverbetrieb; `data/` und `.env` sind nicht versioniert.

## Technologien (im Projekt festgelegt, keine Aussage über neueste Versionen)

- Docker-Basis `python:3.14.7-slim-trixie`; Flask 3.1.3, Flask-SQLAlchemy 3.1.1 / SQLAlchemy 2.0.54, Flask-Login 0.6.3, Flask-WTF 1.3.0, Gunicorn 26.2.0, Requests 2.34.2. Vollständige direkte und transitive Pins: `requirements.txt`.
- Jinja, CSS, JavaScript-ES-Module ohne Node-Buildpipeline; Bootstrap 5.3.0 und Chart.js 4.5.1 über CDN. Einige Plattformlogos ebenfalls extern, andere lokal.
- Android: Kotlin/Compose-Compiler 2.2.20, Android Gradle Plugin 8.13.2, Gradle-Wrapper 8.13 mit SHA-256-Prüfsumme, JDK 17.
- Android minSdk 26 (Android 8), compileSdk/targetSdk 36, Build-Tools laut Anleitung 35.0.0; Compose BOM 2025.09.00, Material 3, Lifecycle/ViewModel, Coroutines, OkHttp 4.12.0, Coil 2.7.0. Tests mit JUnit und MockWebServer.
- App-ID/Namespace `de.gamescollection.viewer`; kein Flutter, keine WebView, keine separate Android-Datenbank.

## Datenmodell und Konsistenzentscheidungen

- `User`: Name plus eindeutiger `username_key` (`strip().casefold()`), Passwort-Hash, Adminflag und `ui_theme`.
- `Game`: Besitzer, optionale numerische IGDB-ID/URL, Titel, Cover, Release-Jahr, Entwickler/Publisher/Engine/verfügbare Plattformen, Format, Status, optionale Bewertung, Altersfreigabe und `version`.
- `GamePlatformOwnership`: normalisierte Besitzplattformen mit eindeutigem Paar `(game_id, platform)`; API stellt sie aus Kompatibilitätsgründen als kommagetrenntes Feld `owned_platform` dar.
- Eindeutiges `(user_id, igdb_id)` verhindert doppelte IGDB-Spiele je Benutzer; mehrere manuelle Spiele ohne IGDB-ID sind möglich.
- Fremdschlüssel mit `ON DELETE CASCADE` und ORM-Cascades: Kontolöschung entfernt zugehörige Spiele und Plattformzuordnungen.
- Checks für Bewertung 0–10, Formate `Digital`/`Physisch`, Status `Nicht gespielt`/`Gespielt`/`Abgebrochen`, Release-Jahr 1900–2100 und unterstützte Altersfreigaben.
- Updates verlangen die gelesene `version`; SQLAlchemy-Versionierung und HTTP 409 schützen gegen veraltete Schreibzugriffe. Dies nicht pauschal als Versionsprüfung aller DELETE-/Benutzeroperationen verstehen.
- Zentraler Commit-Helfer rollt bei Konflikten/DB-Fehlern zurück; Fehlerstatus u. a. 409 und 503.
- SQLite: `foreign_keys=ON`, `journal_mode=WAL`, `synchronous=NORMAL`, `busy_timeout=15000`; Connect-Timeout 15 Sekunden. WAL verbessert Leser-/Schreiberparallelität, macht SQLite aber nicht zu einer Multi-Writer-Serverdatenbank.
- `db.create_all()` plus eng begrenzte `ui_theme`-Ergänzung beim Start. Keine vollständige Migration bestehender Alt-Schemas vorhanden.

## Authentifizierung, API und externe Dienste

- Flask-Login mit signierten clientseitigen Flask-Sitzungs-Cookies; keine eigene serverseitige Sitzungstabelle implementiert. Passwörter gehasht über Werkzeug.
- Erster registrierter Benutzer erhält Adminrechte. Eigenes Konto bearbeiten/löschen erfordert aktuelles Passwort. Bei Selbstlöschung des letzten Admins kann ein verbleibender Benutzer zum Nachfolger werden.
- CSRF auch für native Clients aktiv; Fehler tragen `X-CSRF-Error: 1`. Browser und Android erneuern dann das Token und wiederholen die abgewiesene Anfrage genau einmal.
- `GET/POST/DELETE /api/session`: Sitzungsstatus inkl. `api_version: 1` und CSRF, JSON-Login, Logout; GET auch anonym.
- `GET /api/games`: eigene Sammlung + Besitzplattformliste; `POST /api/games`, `PUT/DELETE /api/games/<id>`: CRUD.
- `POST /api/search`: `query` plus optional `release_year`; numerische Suche ohne Jahr wird als IGDB-ID behandelt.
- `/api/account` (PUT/DELETE), `/api/account/theme` (PUT), `/api/users` (GET), `/api/users/<id>` (PUT/DELETE): Kontoverwaltung/Admin.
- `GET /api/csrf-token`: angemeldeter Browser-Refresh; `GET /health`: DB-Erreichbarkeit. Geschützte APIs liefern bei fehlender Anmeldung JSON/401, nicht die Loginseite.
- Session- und Sammlungsantworten sind `no-store`. Ändernde native Anfragen senden `X-CSRFToken`, Cookie und Serveradresse als `Referer` für strenge HTTPS-CSRF-Prüfung.
- `SECRET_KEY` signiert Flask-Daten und ist ausschließlich serverseitig. IGDB benötigt **TWITCH_CLIENT_ID + TWITCH_CLIENT_SECRET**, kein Benutzerpasswort; Twitch `client_credentials` liefert ein im Prozess gecachtes Access-Token.
- IGDB-Altersfreigabe: USK vor PEGI. Cover nur erlaubte IGDB-HTTPS-Hosts; IGDB-Links auf erlaubte IGDB-Hosts begrenzt.
- Deku Deals ist ein externer Suchlink `https://www.dekudeals.com/search?q=…`, keine integrierte Preis-API und keine zugesicherte direkte Produktzuordnung.

## Umgesetzte Oberflächenfunktionen

### Web/PWA

- Registrierung, Anmeldung mit „Eingeloggt bleiben“, Benutzeradministration, eigenes Profil mit Namens-/Passwortänderung und Kontolöschung.
- Sammlung: IGDB-Suche mit Jahresfilter, manuelles Anlegen, Bearbeiten/Löschen, Mehrfachbesitzplattformen, Status/Format/Bewertung, Metadaten und Cover.
- Filter nach Text, Engine, Plattform, Status, Format, Altersfreigabe und Mindestbewertung; Sortierung und lokale Covergrößeneinstellung.
- Zwei Designs; mobil schwebende kompakte Bottom-Bar, Profil/Konto für Einstellungen/Abmelden; bereinigte redundante Navigationsknöpfe.
- Plattformlogos oder Text-Fallbacks; klassische Nintendo-/Sega-Plattformen sowie Wii/Wii U gehören zur kanonischen Liste in `config.py`.
- Statistiken mit Chart.js, Namen/Anzahlen/Prozentwerten; Beschriftungen in ausreichend großen Tortenstücken, Restlegende für kleine Anteile.
- Manifest, Icons, Service Worker, Offline-Hinweisseite. Service Worker nutzt Netzwerk zuerst, cached statische Dateien, nicht API-Antworten oder persönliche HTML-Seiten. Keine Offline-Bearbeitung/Synchronisation. Installation setzt einen geeigneten sicheren Browserkontext voraus (üblicherweise HTTPS; localhost-Sonderfall).

### Native Android-App

- Anmeldung/Abmeldung, optional dauerhaftes Login, Sammlung/Coverraster, Filter, Statistik, IGDB-Suche mit Jahr, manuelle Spiele und CRUD, externe Links.
- Top-Bar-Suchbutton öffnet schwebendes Suchfeld; Kennzahlen auf Statistik statt Sammlung. Kompakte Sortierung und schwebende Bottom-Bar.
- Änderungen dieser Übergabe: Coverclip nur auf Bild statt auf gesamte Karte; gruppierte Profilaktionen; gemeinsame `FormTextField`-Gestaltung; Profil → Einstellungen für Design, Covergrößen-Slider (100–240 dp Mindestbreite, Standard 150) mit Reset und Serverwechsel.
- Covergröße steuert `GridCells.Adaptive`, wird in lokalen Preferences gespeichert und überlebt Serverwechsel. Tatsächliche Coverbreiten richten sich nach verfügbarer Breite/Spaltenzahl.
- Kontoerstellung und weitergehende Kontoverwaltung bleiben im Browser. Keine Offline-Synchronisation, kein eigenes Android-Backend.
- Sitzungscookies und CSRF im Speicher; nur persistente Remember-Cookies verschlüsselt via Android Keystore im privaten Speicher. Kein gespeichertes Passwort; Backups deaktiviert.
- Cookies strikt an Serverorigin inkl. Protokoll/Port gebunden, keine automatischen Redirects oder TLS-Ausnahmen. HTTP nur mit ausdrücklicher Testfreigabe. Serverwechsel löscht Sitzung; Root-URL ohne Unterpfad verwenden.

## Betrieb und Fortsetzung auf einem anderen Computer

1. Richtiges Repository klonen bzw. aktualisieren. Mit `git log -1` und `git status -sb` prüfen, ob der übergebene Stand vorhanden ist. Nicht versionierte Secrets, Daten und Signaturschlüssel separat übertragen; sie werden durch Klonen nicht mitgenommen.
2. `.env.example` als `.env` übernehmen und eigene Werte setzen. Schlüssel z. B. mit `openssl rand -hex 32` oder `python3 -c "import secrets; print(secrets.token_hex(32))"` erzeugen. Reale Secrets nicht aus dieser Dokumentation erwarten.
3. Persistentes `data/` separat und sicher übertragen, wenn benötigt. Aktuelle Daten sind nicht als entbehrlich anzusehen. Für konsistente SQLite-Sicherung bei WAL nicht blind nur die laufende `.db` kopieren; SQLite-Backupverfahren oder sauber gestoppten Server verwenden.
4. Compose: `docker compose up -d --build`; Hostport 7000 → Container 5000. Gunicorn: ein Worker, vier Threads, Timeout 60; nicht-root Benutzer (Build-UID/GID standardmäßig 1000), Healthcheck `/health`.
5. Bind-Mount-Schreibrechte passend zum Containerbenutzer sicherstellen. Auf Fedora ggf. SELinux-Kontext prüfen. Caddy-/externes Netzwerk ist aktuell in Compose **auskommentiert**; historische manuelle Netzwerkerstellung ist keine portable Voraussetzung. HTTPS/Reverse Proxy auf dem Zielrechner klären.
6. Für lokales Backend ohne Docker: Abhängigkeiten aus `requirements.txt` in einer virtuellen Umgebung installieren, `SECRET_KEY` und absoluten `DATABASE_PATH` setzen. `.env` wird durch Compose injiziert; der Python-Code lädt sie nicht selbst via dotenv.
7. Android mit JDK 17, SDK 36 und akzeptierten SDK-Lizenzen einrichten, dann in `android/`: `./gradlew testDebugUnitTest lintDebug assembleDebug`. `android/local.properties`, `ANDROID_HOME`, `JAVA_HOME` sind rechnerlokal.
8. APK liegt unter `android/app/build/outputs/apk/debug/app-debug.apk`. Für bestehende Installationen gleichen Signaturschlüssel sicher separat übertragen; private Keystores nie committen. Standard-Debugschlüssel eines neuen Computers hat normalerweise eine andere Signatur.
9. Lokaler ignorierter Werkzeugcache `android/.toolchain/` wurde auf dem bisherigen Laptop genutzt; er ist kein Bestandteil eines frischen Klons und keine allgemeine Buildvoraussetzung. Keine alten absoluten Rechnerpfade voraussetzen.

## Verifikation und offene Punkte

Bestätigt aus den unmittelbar vorherigen Arbeiten: 9 Android-Unit-Tests, keine Lint-Befunde, APK-Signatur verifiziert. Vor dieser Commit-/Push-Übergabe wurde der Gradle-Aufruf `testDebugUnitTest lintDebug assembleDebug --offline` erneut erfolgreich ausgeführt (unveränderte Test-/Build-Ergebnisse teilweise als UP-TO-DATE übernommen). Die letzte Einstellungs-/Covergrößenänderung wurde nicht auf einem echten Gerät visuell geprüft. Backend und Container wurden für diese reine Android-/Dokumentationsübergabe nicht erneut getestet.

Offen bzw. vor Fortsetzung klären:

- Falls die jüngsten UI-Änderungen auch als GitHub-Download angeboten werden sollen, ein neues bzw. aktualisiertes APK-Release separat beauftragen. Der aktuelle Auftrag betrifft Commit/Push des Quellcodes und der Dokumentation.
- Gerätesichtprüfung: beide Designs, verschiedene Display-/Schriftgrößen, Covergrößen, Textclipping, Dialog-/Zurück-Navigation, Serverwechsel, Login/Persistenz und Fehlerfälle.
- Dauerhafte Release-Signierung, Schlüsselaufbewahrung und zukünftiges Versions-/Tagverfahren sind nicht verbindlich festgelegt. Aktuelle APK ist eine Debug-/Testversion; keine Play-Store-Veröffentlichung bestätigt.
- Keine allgemeine DB-Migrationslösung, automatische WAL-Backupstrategie oder CI-Pipeline in der eingesehenen Projektstruktur. Ob und wann diese ergänzt werden sollen, ist nicht entschieden.
- Öffentliches Betriebsziel, konkrete Caddy-Konfiguration, DNS/TLS und aktuelle Nutzdaten auf dem Zielrechner sind ungeklärt. Nicht aus früheren Screenshots/private IPs ableiten.
- Weitere Sicherheits-/Stabilitätsarbeiten nur nach erneuter konkreter Prüfung priorisieren. Historische Punkte „1–5“ bzw. „1, 2, 5, 6, 7 und 8“ nicht als bekannte offene Aufgaben erfinden.
- Behauptete vollständige Plattformlogoabdeckung, native Kontoverwaltung oder Offline-Synchronisation wären unzutreffend. Fehlende Funktionen nicht als bereits umgesetzt dokumentieren.
