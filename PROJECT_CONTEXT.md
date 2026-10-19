# Games Collection – Projektkontext

## Ergänzung 19.10.2026: erneute Serverprüfung

- Befunde, Korrekturen, Diagnose und Grenzen stehen in [SERVER_REVIEW.md](SERVER_REVIEW.md). Spiel-Updates versionieren jetzt auch reine Plattformänderungen; die Adminvergabe bei paralleler Erstregistrierung erfolgt atomar im INSERT.
- Zentrale DB-/Exceptionbehandlung mit Rollback, deutschen API-/HTML-Fehlern, Fehler-ID und Betreiber-Traceback. SQL-Parameter werden in DB-Exceptions verborgen; externe Fehlerlogs enthalten Typ/HTTP-Status statt potenziell vertraulicher Exceptiontexte. Strengere Typ-/Zahl-/URL-Prüfung, robuste IGDB-Antwortverarbeitung und `no-store` für APIs/Fehler/persönliche HTML-Seiten.
- **36 Backend-Tests bestanden**, zusätzlich lokaler Gunicorn-Smoke-Test mit temporärer DB. Live-IGDB-/Last- und visuelle Prüfungen offen. Der Container wurde anschließend für die Server-Veröffentlichung 0.7 geprüft. Vorbestehende lokale Änderungen einschließlich entfernter Schema-Kompatibilität bleiben erhalten; keine Migration oder Änderung von Nutzdaten während der Prüfung; anschließende Server-Veröffentlichung siehe unten.

Stand: **18.10.2026**. Grundlage sind der lokal eingesehene Quellcode und die zugänglichen Konversationen. Die systematische Android-Stabilitätsprüfung einschließlich anschließender Umsetzung steht in [ANDROID_REVIEW.md](ANDROID_REVIEW.md); sie ist keine vollständige Sicherheitsprüfung. Frühere Assistentenantworten sind nur teilweise verfügbar; insbesondere lässt sich der Inhalt früherer nummerierter Optimierungslisten nicht vollständig rekonstruieren.

## Ziel und aktueller Arbeitsstand

Selbst gehostete Verwaltung persönlicher Videospielesammlungen mit mehreren Benutzerkonten, Weboberfläche/PWA und nativer Android-App. Produktname weiterhin Games Collection.

- Git-Remote lokal: `origin` → `https://github.com/mehmet-b-90/games_collection.git`.
- Geprüfter Ausgangscommit der Android-Review: `127619d` – `Improve Android settings and document project handoff`. Zu Beginn war der Arbeitsbaum sauber; lokal meldete `main...origin/main` keinen Abstand (kein erneuter Remote-Fetch). Den aktuellen Commit und Remote-Abgleich mit `git log -1` und `git status -sb` prüfen.
- Android: Version **0.7**, Code **4**; davon unabhängiges Server-Image: `ghcr.io/mehmet-b-90/games_collection:latest`, alternativ ein veröffentlichter `sha-<Commit-ID>`-Tag über `GAMES_COLLECTION_IMAGE`.
- Der Ausgangscommit `127619d` enthält die bisherigen UI-/Covergrößenänderungen. Der anschließende aktuelle Auftrag umfasst die Umsetzung der fünf Review-Findings sowie Commit und Push nach GitHub. Diese Übergabe enthält Produktfixes, dauerhafte Regressionstests und aktualisierte Dokumentation; kein Tag oder APK-Release. Die tatsächliche Übergabe-Commit-ID mit `git log -1` ermitteln.
- Ausführungsrechner: **linux-pc-mehmet**; gewünschte Fortsetzung: **linux-laptop-mehmet**. Vor lokalem Weiterarbeiten `git status -sb` prüfen und den übertragenen Stand mit `git pull --ff-only` holen, sofern keine lokalen Änderungen entgegenstehen.
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
- `files/idempotency.py`: benutzergebundene Wiederholung bestätigter Spieländerungen über `Idempotency-Key`, atomarer Commit des Belegs zusammen mit der Mutation.
- `files/igdb.py`: OAuth-Token-Cache und USK-/PEGI-Auswahl.
- `files/routes/`: `auth` (HTML-Anmeldung/Registrierung), `main` (Seite/Health/PWA/CSRF), `games`, `users`, `session` (native JSON-Anmeldung).
- `files/templates/`: Jinja-Templates `index.html`, `auth.html`.
- `files/static/`: gemeinsames CSS, Logos, PWA-Dateien und ES-Module in `js/`: `main`, `ui`, `api`, `games`, `igdb-search`, `account`, `admin`, `charts`, `pwa`.
- `tests/test_app.py`: Backend-/Frontend-Vertragstests mit `unittest`, temporärer DB und gemockten externen Aufrufen.
- `android/`: eigenständiges Gradle-Projekt; Anleitung in `android/README.md`.
- Android-Package: `android/app/src/main/java/de/gamescollection/viewer/`; `MainActivity`, `CollectionUi`, `FormTextField`, `CollectionViewModel`, `ApiClient`, `HttpCall`, `SessionStore`, `Models`, `EditorState`, `CollectionSummary`, `NativeIcons`, `ServerAddress`.
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
- `GameWriteReceipt`: Request-Hash, feste Erfolgsantwort und HTTP-Status pro `(user_id, key)`. Neue zusätzliche Tabelle `game_write_receipt`, via `create_all()` angelegt; keine Änderung bestehender Spielspalten. Belege werden mit dem Konto kaskadierend gelöscht und ansonsten erhalten, damit Wiederholungen auch später erkannt werden.
- `GamePlatformOwnership`: normalisierte Besitzplattformen mit eindeutigem Paar `(game_id, platform)`; API stellt sie aus Kompatibilitätsgründen als kommagetrenntes Feld `owned_platform` dar.
- Eindeutiges `(user_id, igdb_id)` verhindert doppelte IGDB-Spiele je Benutzer; mehrere manuelle Spiele ohne IGDB-ID sind möglich.
- Fremdschlüssel mit `ON DELETE CASCADE` und ORM-Cascades: Kontolöschung entfernt zugehörige Spiele und Plattformzuordnungen.
- Checks für Bewertung 0–10, Formate `Digital`/`Physisch`, Status `Nicht gespielt`/`Gespielt`/`Abgebrochen`, Release-Jahr 1900–2100 und unterstützte Altersfreigaben.
- Updates verlangen die gelesene `version`; SQLAlchemy-Versionierung und HTTP 409 schützen gegen veraltete Schreibzugriffe. Dies nicht pauschal als Versionsprüfung aller DELETE-/Benutzeroperationen verstehen.
- Zentraler Commit-Helfer rollt bei Konflikten/DB-Fehlern zurück; Fehlerstatus u. a. 409 und 503.
- SQLite: `foreign_keys=ON`, `journal_mode=WAL`, `synchronous=NORMAL`, `busy_timeout=15000`; Connect-Timeout 15 Sekunden. WAL verbessert Leser-/Schreiberparallelität, macht SQLite aber nicht zu einer Multi-Writer-Serverdatenbank.
- `db.create_all()` legt beim Start fehlende Tabellen einschließlich `user.ui_theme` aus den Modellen an. Die separate Kompatibilitätsergänzung für `ui_theme` wurde am 19.10.2026 entfernt. Bestehende Tabellen werden nicht migriert; Alt-Datenbanken ohne diese Spalte benötigen eine gesonderte Migration.

## Authentifizierung, API und externe Dienste

- Flask-Login mit signierten clientseitigen Flask-Sitzungs-Cookies; keine eigene serverseitige Sitzungstabelle implementiert. Passwörter gehasht über Werkzeug.
- Erster registrierter Benutzer erhält Adminrechte. Eigenes Konto bearbeiten/löschen erfordert aktuelles Passwort. Bei Selbstlöschung des letzten Admins kann ein verbleibender Benutzer zum Nachfolger werden.
- CSRF auch für native Clients aktiv; Fehler tragen `X-CSRF-Error: 1`. Browser und Android erneuern dann das Token und wiederholen die abgewiesene Anfrage genau einmal.
- `GET/POST/DELETE /api/session`: Sitzungsstatus inkl. `api_version: 1`, `capabilities.idempotent_game_writes: true` und CSRF, JSON-Login, Logout; GET auch anonym. Aktuelle Android-Builds verlangen diese Capability für Spieländerungen, ältere Clients ohne Idempotenzheader bleiben kompatibel.
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
- Im geprüften Quellstand enthalten: Coverclip nur auf Bild statt auf gesamte Karte; gruppierte Profilaktionen; gemeinsame `FormTextField`-Gestaltung; Profil → Einstellungen für Design, Covergrößen-Slider (100–240 dp Mindestbreite, Standard 150) mit Reset und Serverwechsel.
- Covergröße steuert `GridCells.Adaptive`, wird in lokalen Preferences gespeichert und überlebt Serverwechsel. Tatsächliche Coverbreiten richten sich nach verfügbarer Breite/Spaltenzahl.
- Spielentwurf, Editiermodus und offener Vorgang liegen im ViewModel; ein einzelner Entwurf wird im `SavedStateHandle` gesichert und erst nach bestätigter Server-/Kontozuordnung restauriert. Bei explizitem Logout oder Serverwechsel wird er gelöscht; bei bloßem Sitzungsablauf bleibt er für denselben erneuten Login erhalten.
- Nach unklarer Speicher-/Löschantwort sind Payload und UUID eingefroren. „Vorgang erneut prüfen“ wiederholt denselben Vorgang; der Server replayt einen bereits bestätigten Erfolg. Geschlossener unklarer Vorgang öffnet sich über + oder Spielauswahl erneut. Saved-State bleibt nur für Editorentwürfe zuständig; gespeicherte Änderungen liegen seit 18.10.2026 dauerhaft in `OfflineCollection`/`OfflineStore`.
- API-Erstellung ist auf eine suspending IO-Factory begrenzt; HTTP-Cancellation erreicht Call und Body-Lesen. Modellumwandlung, abbrechbare Filter-/Sortierberechnung und Aggregate laufen auf Default; Suche ist 120 ms entprellt, Statistikzeilen sind einzeln lazy.
- Kontoerstellung und weitergehende Kontoverwaltung bleiben im Browser. Dauerhafte Offline-Sammlung und Warteschlange mit späterer Synchronisierung; kein eigenes Android-Backend.
- Sitzungscookies und CSRF im Speicher; nur persistente Remember-Cookies verschlüsselt via Android Keystore im privaten Speicher. Kein gespeichertes Passwort; Backups deaktiviert.
- Cookies strikt an Serverorigin inkl. Protokoll/Port gebunden, keine automatischen Redirects oder TLS-Ausnahmen. HTTP nur mit ausdrücklicher Testfreigabe. Serverwechsel löscht Sitzung; Root-URL ohne Unterpfad verwenden.

## Betrieb und Fortsetzung auf einem anderen Computer

1. Richtiges Repository klonen bzw. aktualisieren. Mit `git log -1` und `git status -sb` prüfen, ob der übergebene Stand vorhanden ist. Nicht versionierte Secrets, Daten und Signaturschlüssel separat übertragen; sie werden durch Klonen nicht mitgenommen.
2. `.env.example` als `.env` übernehmen und eigene Werte setzen. Schlüssel z. B. mit `openssl rand -hex 32` oder `python3 -c "import secrets; print(secrets.token_hex(32))"` erzeugen. Reale Secrets nicht aus dieser Dokumentation erwarten.
3. Persistentes `data/` separat und sicher übertragen, wenn benötigt. Aktuelle Daten sind nicht als entbehrlich anzusehen. Für konsistente SQLite-Sicherung bei WAL nicht blind nur die laufende `.db` kopieren; SQLite-Backupverfahren oder sauber gestoppten Server verwenden.
4. Compose: `docker compose pull` und `docker compose up -d`; Hostport 7000 → Container 5000. Lokale Builds: `docker build -t games-collection:local .` und `GAMES_COLLECTION_IMAGE=games-collection:local docker compose up -d --pull never`. Gunicorn: ein Worker, vier Threads, Timeout 60; nicht-root Benutzer (Build-UID/GID standardmäßig 1000), Healthcheck `/health`.
5. Bind-Mount-Schreibrechte passend zum Containerbenutzer sicherstellen. Auf Fedora ggf. SELinux-Kontext prüfen. Caddy-/externes Netzwerk ist aktuell in Compose **auskommentiert**; historische manuelle Netzwerkerstellung ist keine portable Voraussetzung. HTTPS/Reverse Proxy auf dem Zielrechner klären.
6. Für lokales Backend ohne Docker: Abhängigkeiten aus `requirements.txt` in einer virtuellen Umgebung installieren, `SECRET_KEY` und absoluten `DATABASE_PATH` setzen. `.env` wird durch Compose injiziert; der Python-Code lädt sie nicht selbst via dotenv.
7. Android mit JDK 17, SDK 36 und akzeptierten SDK-Lizenzen einrichten. Vorhandene private Release-Signierung sicher übertragen (siehe Nachtrag unten), dann in `android/`: `./gradlew testReleaseUnitTest lintRelease assembleRelease`. Debug-Variante bleibt für Entwicklung verfügbar. `android/local.properties`, `ANDROID_HOME`, `JAVA_HOME` sind rechnerlokal.
8. Reguläre APK liegt unter `android/app/build/outputs/apk/release/Games-Collection-0.6-release.apk`. Für Updates denselben privaten Release-Schlüssel sicher separat übertragen; private Keystores und `signing.properties` nie committen. Bisherige Debug-Installationen haben eine andere Signatur und können nicht direkt damit aktualisiert werden; keine Deinstallation ohne Synchronisierung ausstehender Offline-Änderungen.
9. Lokaler ignorierter Werkzeugcache `android/.toolchain/` wurde auf dem bisherigen Laptop genutzt; er ist kein Bestandteil eines frischen Klons und keine allgemeine Buildvoraussetzung. Keine alten absoluten Rechnerpfade voraussetzen.

## Verifikation und offene Punkte

Docker-Veröffentlichung vom **17.10.2026**: Image lokal mit Podman im Docker-Format gebaut, **25 Backend-Tests im Image bestanden**, Gunicorn gestartet und Healthcheck `/health` erfolgreich. Workflow mit `actionlint` validiert. Der tatsächliche GHCR-Buildstatus ist über GitHub Actions zu prüfen; anonymer Pull setzt die öffentliche Paketsichtbarkeit voraus.

Am **17.10.2026 auf linux-pc-mehmet** nach Umsetzung erfolgreich:

- JDK 17.0.20.1 / SDK 36: `./gradlew testDebugUnitTest lintDebug assembleDebug --offline` – **17 JVM-Tests bestanden**, **Lint ohne Befunde**, Debug-APK gebaut. Acht neue Stabilitätstests ergänzen die neun bisherigen Tests: Entwurfs-/Vorgangsserialisierung, Server-/Kontotrennung, IO-Factory, Cancel von Call bei verzögerten Headern/Body, Wiederholung nach Response-Verlust, UUID-Erhalt bei CSRF-Erneuerung, Blockieren unsicherer Writes an alten Servern und Berechnungen für 10.000 Spiele.
- Backend: **25 Tests bestanden**, `python -m unittest discover -s tests -v` mit den unveränderten Pins aus `requirements.txt`. Zusätzlich getestet: replayte Creates/Updates/Deletes, unveränderte Versionsprüfung, andere Payloads mit gleicher Kennung, parallele Creates, atomarer Rollback, Benutzertrennung, FK-Cascade, Auth/CSRF/UUID-Validierung und Erzeugen der zusätzlichen Tabelle bei bestehenden Spieldaten. Ausschließlich temporäre Testdatenbank.
- Backend-Testumgebung lokal: gebündeltes Python 3.12 unter `~/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3`, Pakete isoliert in `/tmp/games-collection-review-python`, per `PYTHONPATH` eingebunden. Diese Pfade sind nicht auf `linux-laptop-mehmet` vorauszusetzen; dort eine virtuelle Umgebung mit `requirements.txt` einrichten. Die fehlende Flask-Installation der ursprünglichen Review wurde für diese Prüfungen damit umgangen.
- `git diff --check` zur Übergabe prüfen; GitHub-Remote `origin` bestätigt als `https://github.com/mehmet-b-90/games_collection.git`. Vor der Übergabe frisch gefetcht, Ausgangsstand ohne Abweichung zu `origin/main`.

Lokale APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Android-Version bleibt **0.6 / Code 3**; kein neuer Tag, keine neue Release-APK, keine neue Signaturprüfung. Bisheriger GitHub-Download bleibt separat.

### Umsetzung und Fortsetzung auf linux-laptop-mehmet

F1–F5 aus [ANDROID_REVIEW.md](ANDROID_REVIEW.md) sind im Quellcode bearbeitet; die ursprünglichen Quellzeilen im Bericht beziehen sich weiterhin auf `127619d`. Die vollständige Review und Umsetzungs-/Testmatrix stehen dort. Die ursprünglichen drei temporären JVM-Proben sind durch dauerhafte Regressionstests ersetzt und für die Fortsetzung nicht erforderlich.

Nach regulärer Datensicherung den aktualisierten Server bereitstellen; er ergänzt `game_write_receipt`, ohne bestehende Tabellen umzubauen. Keine Belege ohne vereinbarte Wiederholungsfrist löschen. Aktuelle Android-Builds benötigen die Server-Capability zum Speichern/Löschen. Während dieser Arbeit wurde kein laufender Server/Container ersetzt.

**Noch nicht verifiziert:** echte Activity-Neuerstellung, systemseitige Prozesswiederherstellung, Saved-State-Größenlimits bei sehr großen Metadaten, Android-Keystore-Verhalten, Geräteperformance/Framezeiten, beide Designs auf Smartphone. ADB zeigte kein verbundenes Gerät; kein Emulator vorhanden. Bei der damaligen Android-Review war Docker nicht installiert, daher erfolgte dort keine Containerprüfung. Auf `linux-laptop-mehmet` diese Prüfungen nachholen. API-Pagination und harte Antwortlimits wurden nicht ergänzt; sehr große Antworten können weiterhin hohen Speicherbedarf verursachen.

Offen bzw. vor Fortsetzung klären:

- Falls die jüngsten UI-Änderungen auch als GitHub-Download angeboten werden sollen, ein neues bzw. aktualisiertes APK-Release separat beauftragen. Der aktuelle Auftrag umfasst Produktfixes sowie Commit/Push des Quellcodes, kein APK-Release.
- Gerätesichtprüfung: beide Designs, verschiedene Display-/Schriftgrößen, Covergrößen, Textclipping, Dialog-/Zurück-Navigation, Serverwechsel, Login/Persistenz und Fehlerfälle.
- Release-Signierung ist seit 18.10.2026 lokal eingerichtet (siehe Nachtrag); geschützte Sicherung und Übertragung auf weitere Rechner bleiben Aufgabe des Besitzers. Versions-/Tagverfahren für neue Veröffentlichungen weiterhin konkret abstimmen. Bestehende GitHub-Download-APK bleibt historischer Debug-Build, keine Play-Store-Veröffentlichung bestätigt.
- Docker-CI/CD: `.github/workflows/docker-publish.yml` baut ein AMD64-Testimage, führt die Backend-Tests und einen Container-Healthcheck aus und veröffentlicht anschließend AMD64/ARM64 in GHCR. Relevante Pushes auf `main` aktualisieren `latest` und den SHA-Tag; `docker-v*` veröffentlicht separate Versionstags. Pull Requests prüfen ohne Veröffentlichung; manueller Lauf auf `main` möglich. Neues GHCR-Paket muss einmalig vom Besitzer öffentlich gestellt werden, damit anonyme Pulls möglich sind. Kein zusätzliches Registry-Secret erforderlich. Der Buildkontext beschränkt sich auf Dockerfile, Requirements und `files/`.
- Keine allgemeine DB-Migrationslösung oder automatische WAL-Backupstrategie vorhanden.
- Öffentliches Betriebsziel, konkrete Caddy-Konfiguration, DNS/TLS und aktuelle Nutzdaten auf dem Zielrechner sind ungeklärt. Nicht aus früheren Screenshots/private IPs ableiten.
- Weitere Sicherheits-/Stabilitätsarbeiten nur nach erneuter konkreter Prüfung priorisieren. Historische Punkte „1–5“ bzw. „1, 2, 5, 6, 7 und 8“ nicht als bekannte offene Aufgaben erfinden.
- Behauptete vollständige Plattformlogoabdeckung, native Kontoverwaltung wären unzutreffend. Fehlende Funktionen nicht als bereits umgesetzt dokumentieren.

## Ergänzung 18.10.2026: Android-Offline-Synchronisierung

- `OfflineCollection.kt`: atomarer JSON-Snapshot mit Serverstand, Plattformen und Warteschlange pro Serveradresse/Benutzer-ID. Negative IDs existieren ausschließlich lokal; neue Serveranfragen verwenden weiterhin ID 0 ohne Versionsfeld.
- Änderungen zuerst dauerhaft lokal speichern, dann optional übertragen. Noch ungesendete Vorgänge können ersetzt oder verworfen werden; nach erstem Sendeversuch bleiben Payload und UUID unverändert. Erfolgsbeleg und anschließender Sammlungsabruf müssen bestätigt sein, bevor der Vorgang lokal entfernt wird.
- Synchronisierung bei erfolgreichem Verbinden/Anmelden, online beim Speichern sowie manuell über Aktualisieren/„Synchronisieren“. Kein Worker/Hintergrunddienst und kein automatischer Netzwerkrückkehr-Trigger.
- Serverseitige Sitzung/Kontobindung vor jedem Synchronisierungslauf prüfen. Abmelden/Serverwechsel behalten die getrennten Dateien, blenden sie aber aus; Wiederzugriff nach erneutem Login am ursprünglichen Konto. Deinstallation/App-Daten-Löschen verliert nicht synchronisierte Änderungen.
- Eindeutige Ablehnungen bleiben sichtbar, andere Titel können synchronisieren. Konflikte bewusst verwerfen und auf aktuellem Serverstand neu bearbeiten; kein automatisches Überschreiben. Bestehender DELETE-Vertrag bleibt ohne Versionsprüfung.
- Keine Server-, Versions-, Tag- oder Releaseänderung. IGDB-Suche benötigt weiterhin Verbindung. Geräteprüfung für Offline-Neustart, vollen Speicher, Verbindungsabbruch, Konto-/Serverwechsel und beide Designs steht aus.
- Verifikation dieses Nachtrags: `testDebugUnitTest lintDebug assembleDebug --offline` mit lokalem JDK 17/SDK 36 erfolgreich; **22 JVM-Tests**, keine Fehler, Lint ohne Befunde. Lokale Debug-APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Keine Geräte-/Sichtprüfung, kein APK-Upload. Backend unverändert. Vorbestehende Änderungen an `.env.example` und `docker-compose.yaml` wurden nicht bearbeitet.

## Ergänzung 18.10.2026: Release-Build als reguläre Weitergabe

- Auftrag: auf Release-Build umstellen und Dokumentation aktualisieren; kein neuer Versions-/Tagauftrag, Commit, Push, APK-Upload oder Play-Store-Release. Android bleibt **0.6 / versionCode 3**. Vor einem nächsten veröffentlichten App-Update den VersionCode erhöhen und Version/Tag abstimmen.
- `android/app/build.gradle`: expliziter nicht debuggbarer Release-Build, separate Signierung aus lokaler `android/signing.properties`, keine Passwörter im Buildskript und kein Debug-Fallback. Ohne vollständige Signierungsdaten/Keystore bricht die Paketierung ab. Code-/Ressourcenverkleinerung zunächst deaktiviert; R8-Optimierung kann später mit eigener Geräteprüfung folgen.
- Einmalige Erzeugung über `android/create-release-key.py`: separater PKCS12-/RSA-3072-Schlüssel, 30 Jahre Laufzeit, kryptographisch zufälliges Passwort, private Dateien mit Rechten 0600. Wiederholter Aufruf verweigert Überschreiben. Keine vorhandenen Schlüssel geändert. Vorlage für eigene Schlüssel: `android/signing.properties.example`.
- Lokal angelegt: `android/keystores/games-collection-release.p12` und `android/signing.properties`. Beide sind ignoriert und werden **nicht mit Git übertragen**. Für `linux-laptop-mehmet` dieselben Dateien sicher separat kopieren; dort keinen neuen Schlüssel erzeugen. Dauerhafte geschützte Sicherung ist noch durch den Besitzer vorzunehmen.
- APK-Name automatisch aus `versionName`: `android/app/build/outputs/apk/release/Games-Collection-0.6-release.apk`. Debug bleibt für Entwicklung unter dem bisherigen Pfad verfügbar. Es handelt sich um den aktuellen lokalen Arbeitsstand einschließlich der vorbestehenden Offline-Änderungen, nicht um eine veröffentlichte neue App-Version.
- Der neue Release-Schlüssel unterscheidet sich von der bisherigen Debug-Signatur. Ein direktes Update vorhandener Debug-Installationen ist nicht möglich. Vor einem bewussten Wechsel lokale Warteschlange synchronisieren und nötige Daten sichern; keine App wurde installiert, deinstalliert oder deren Daten gelöscht.
- Bestehende lokale Änderungen an Offline-Code/-Tests, `.env.example`, `docker-compose.yaml`, `ANDROID_REVIEW.md` und bereits bearbeiteten Dokumenten bleiben erhalten. Dieser Auftrag ändert nur Build-/Signierungsworkflow und zugehörige Dokumentation.
- Verifikation: `./gradlew testReleaseUnitTest lintRelease assembleRelease --offline` erfolgreich mit JDK 17 / SDK 36; **22 Release-JVM-Tests bestanden**, **Lint ohne Befunde**. APK per `apksigner verify --verbose --print-certs` erfolgreich geprüft (v2, RSA 3072); Metadaten bestätigen Release, Version 0.6 / Code 3 und `debuggable=false`. Öffentlicher Zertifikat-SHA-256: `a7fd45c260ed64167706f8cbe4ceee61fa843c82831875e281ce06fd89e3e328`. Unterschied zur vorhandenen Debug-Signatur bestätigt.
- Negativprüfungen: In separater temporärer Projektkopie wird fehlende Release-Signierung verständlich abgewiesen; der Debug-Taskgraph benötigt keinen Release-Schlüssel. Wiederholte Schlüsselerzeugung verweigert Überschreiben, beide privaten Dateien haben Rechte 0600 und werden von Git ignoriert. `git diff --check` ohne Befunde. Keine Geräte-/Sichtprüfung, kein instrumentierter Lifecycle-/Keystore-Test, kein Commit/Push und keine Veröffentlichung. Backend und Container unverändert.

## Ergänzung 19.10.2026: direkte Android-IGDB-Suche

- `IgdbClient.kt` übernimmt die direkte HTTPS-Suche mit eigenem HTTP-Client ohne Sammlungs-Cookies; Titel/Jahr/ID und USK-vor-PEGI bleiben erhalten.
- `POST /api/igdb-token` verlangt Login und CSRF, akzeptiert nur `{}` und liefert Client-ID/Token/Restlaufzeit mit `no-store`. Kein Client-Secret in Antworten. Twitch-Tokenanforderung nutzt jetzt Form-Body statt URL-Query, Cache ist gegen parallele Threads geschützt und an die Twitch-Konfiguration gebunden. Vor Ausgabe vorhandene Tokens bei Twitch validieren.
- `SessionStore` besitzt einen separaten Namensraum/Keystore-Alias für verschlüsselte IGDB-Zugangsdaten und prüft dort den erfolgreichen Schreibabschluss. Kein Klartext- oder unverschlüsselter Speicherfallback. Cookie-Alias bleibt kompatibel. Token ist lokal an Server/Konto gebunden, wird beim Wechsel/Logout entfernt und niemals im Offline-Sammlungssnapshot oder Saved-State abgelegt.
- Initiale Bereitstellung und spätere Erneuerung über den Sammlungsserver, anschließend direkte Internet-Suche bis Tokenablauf/Widerruf. Ablaufzeit im Suchdialog. IGDB-401 meldet nicht von Games Collection ab. Kein Cloud-Dienst und kein Hintergrundworker ergänzt.
- Serverupdate erforderlich; keine Version/Tags geändert und kein Release veröffentlicht. Echte IGDB-Zugangsdaten wurden für Tests nicht verwendet. Geräteprüfung des Android Keystore, beider Designs und echter IGDB-Suche steht aus.
- Verifikation: **27 Backend-Tests** mit isolierten Paketen aus `requirements.txt` und temporärer Datenbank bestanden; **26 Android-Release-JVM-Tests**, Lint ohne Befunde, `assembleRelease` erfolgreich. APK: `android/app/build/outputs/apk/release/Games-Collection-0.6-release.apk`. Kein Live-IGDB-, Geräte-/Keystore- oder Container-Deployment-Test.

## Veröffentlichung Android 0.7 (19.10.2026)

- Android-Version 0.7, VersionCode 4; Tag `android-v0.7`, Asset `Games-Collection-0.7-release.apk` mit bestehender Release-Signatur.
- Enthält dauerhafte Offline-Sammlung/Warteschlange, direkte IGDB-Suche mit verschlüsseltem Token und Release-Build-Konfiguration. Serverupdate für `POST /api/igdb-token` erforderlich.
- Ab jetzt bei beauftragten Veröffentlichungen GitHub Release Notes pflegen. APK-Link in beiden READMEs aktualisiert. Historische Angaben zu 0.6 oben beschreiben den damaligen Stand; aktuellen Commit mit `git log -1` ermitteln.
- Vorbestehende lokale Anpassungen an `.env.example` und `docker-compose.yaml` sind nicht Teil der Übergabe. Private Signierungsdateien bleiben ignoriert.
- Geräte-/Keystore-/Live-IGDB-Prüfung weiterhin offen; die Release-Signatur ist nicht mit früheren Debug-APKs kompatibel. Vor einem bewussten Installationswechsel ausstehende Änderungen synchronisieren.

## Server-Veröffentlichung 0.7 (19.10.2026)

- Auftrag: geprüfte Serveränderungen committen/pushen und als `docker-v0.7` mit GitHub Release Notes veröffentlichen; Docker `:0.7` und `:latest` für AMD64/ARM64 bereitstellen. Notizen: `release-notes/server-0.7.md`. Android-Version, vorhandener Android-Tag und APK bleiben unverändert. Lokale Android-Änderungen und Zugangswerte gehören nicht zum Server-Commit.
- Enthält die Serverkorrekturen aus `SERVER_REVIEW.md`, die neue HTML-Fehlerseite und 36 Backend-Tests. Entfernung der alten `ui_theme`-Kompatibilitätsergänzung übernommen und dokumentiert; alte Datenbanken benötigen ggf. eine separate Migration. Keine Nutzdaten verändert.
- Lokale Verifikation: Docker-Image mit Podman im Docker-Format gebaut, 36 Tests darin bestanden, Gunicorn-Container gestartet und Healthcheck erfolgreich. GitHub Actions wiederholt Tests und Healthcheck vor der Veröffentlichung. Live-IGDB-, Geräte-, Last- und visuelle Prüfungen weiterhin offen.
- Neue Release-Notes-Datei auf `main` aktiviert die zusätzliche Versionstag-Veröffentlichung. Erst nach erfolgreichem Upload erstellt ein separater Job mit `contents: write` Tag und GitHub Release. Bestehende Versionstags dürfen keinem anderen Commit zugeordnet werden. Normale Serveränderungen veröffentlichen weiterhin nur `latest`/SHA.
