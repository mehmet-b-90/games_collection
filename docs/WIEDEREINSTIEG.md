# Games Collection – Bestandsaufnahme vom 23.10.2026

**Nachtrag vom selben Tag:** Der Betreiber hat `.env` für lokale Tests wiederhergestellt; Vorhandensein und Git-Ignorierung wurden bestätigt, Zugangswerte nicht ausgegeben oder live geprüft. Außerdem bestätigt er erneut den bereits veröffentlichten internen Google-Play-Test. Die nachstehende Bestandsaufnahme beschreibt den zuvor ermittelten Zustand; aktueller Fortschritt und der verbindliche Auftrag zur fortlaufenden Markdown-Dokumentation stehen in `../PROJECT_CONTEXT.md` und `../AGENTS.md`.

## Ausgangslage und Repository

Nach dem versehentlichen Entfernen des Codex-Projekts wurde der vorhandene Ordner erneut untersucht. Codex führt ihn bereits wieder als lokales Git-Projekt „Games Collection“. Diese Bestandsaufnahme rekonstruiert den Arbeitsstand aus Dateien und Git; sie stellt keine gelöschten Chatverläufe wieder her.

- Ordner: `/home/mehmet/Dokumente/ChatGPT/Games Collection`.
- Repository: <https://github.com/mehmet-b-90/games_collection>.
- Branch `main`; nach erfolgreichem Fetch sind `HEAD` und `origin/main` identisch: `9b51ecc9650800145b3feed51519d71be7730126`, „Add desktop and Android screenshots to README“.
- Die vorhandene SSH-Adresse scheiterte außerhalb der Sandbox mit `Permission denied (publickey)`. `origin` wurde für Fetch und Push auf `https://github.com/mehmet-b-90/games_collection.git` umgestellt. `git fetch origin` war erfolgreich. Kein Pull/Merge, Reset, Commit oder Push.
- Die verbundene GitHub-App kann das Repository lesen und meldet Schreib-/Adminberechtigung. Ein späterer Push über die lokale Git-CLI benötigt deren HTTPS-Authentifizierung; ein authentifizierter CLI-Push wurde nicht getestet.
- Vorbestehende lokale Änderungen bleiben erhalten. Der Gleichstand der Commit-IDs bedeutet ausdrücklich nicht, dass der gesamte lokale Arbeitsstand bereits auf GitHub liegt.

## Umfang

Untersucht wurden eigener Python-, JavaScript-, Kotlin-/Java-Quellcode, Templates/CSS, Android-Ressourcen, Tests, Build-/Container-/CI-Konfiguration, Export- und Signierungsskripte sowie Projekt-, Review- und Veröffentlichungsdokumentation. Bilddateien, Screenshots und Build-Artefakte wurden ihrem Zweck zugeordnet. Keine erneute visuelle Abnahme der Bilder oder laufenden Oberflächen.

Die rund 2,3 GB unter `android/` bestehen überwiegend aus ignoriertem SDK/JDK, Gradle-Abhängigkeiten und Build-Ausgaben. Fremdbibliotheken, SDK-Quellbestände und generierte Dateien wurden nicht wie eigener Produktcode vollständig geprüft. Private Schlüssel und Passwörter wurden nicht ausgegeben.

## Komponenten und Zuständigkeiten

| Bereich | Einstiegspunkte | Aufgabe |
| --- | --- | --- |
| Flask-Anwendung | `files/app.py`, `config.py`, `extensions.py` | App-Factory, Konfiguration, Login/CSRF, Datenbank, Fehlerantworten |
| Datenmodell | `files/models.py`, `database.py`, `validation.py`, `idempotency.py` | Benutzer, Spiele, Besitzplattformen, Vorgangsbelege, Validierung und Transaktionen |
| HTTP-Routen | `files/routes/{auth,main,session,games,users}.py` | HTML-Anmeldung, Sammlung, JSON-API, Konten/Admin, Healthcheck |
| IGDB am Server | `files/igdb.py`, `routes/games.py`, `routes/session.py` | Twitch-Token, Websuche und Tokenbereitstellung für Android |
| Web/PWA | `files/templates/`, `files/static/style.css`, `files/static/js/` | Zwei Designs, Editor, Suche/Filter, Statistiken, Konten und Installation |
| Android-Oberfläche | `MainActivity.kt`, `CollectionUi.kt`, `FormTextField.kt`, `NativeIcons.kt` | Native Compose-/Material-3-Oberfläche, keine WebView |
| Android-Zustand | `CollectionViewModel.kt`, `Models.kt`, `CollectionSummary.kt`, `EditorState.kt` | Sitzungs-/Editorzustand, Filter, Sortierung, Statistik und gespeicherter Entwurf |
| Android-Netzwerk | `ApiClient.kt`, `HttpCall.kt`, `ServerAddress.java`, `SessionStore.kt` | Origin-Prüfung, Cookies/CSRF, abbrechbare Requests, verschlüsselte Persistenz |
| Android-Offline/IGDB | `OfflineCollection.kt`, `IgdbClient.kt` | Atomare Sammlung/Warteschlange und direkte IGDB-Suche |
| Betrieb/Veröffentlichung | `Dockerfile`, `docker-compose.yaml`, `.github/workflows/docker-publish.yml`, `release-notes/` | Container, CI-Prüfungen, GHCR und getrennte Server-/Android-Releases |
| Gestaltung | `design/brand/`, `tools/export_icons.py`, `docs/screenshots/` | Verbindliches Icon, technische Exporte und historische Screenshots |

Android-Dateinamen in der Tabelle liegen unter `android/app/src/main/java/de/gamescollection/viewer/`.

## Server und Datenfluss

Der Server nutzt Flask, SQLAlchemy und SQLite. Gunicorn startet im Container mit einem Worker und vier Threads als Benutzer mit UID/GID 1000. Compose verwendet fest `ghcr.io/mehmet-b-90/games_collection:latest`, bindet `./data` nach `/app/data` ein und veröffentlicht Hostport 7000 auf Containerport 5000. Datenbankstandard ist `/app/data/database.db`.

**Die aktuelle Compose-Datei wertet `GAMES_COLLECTION_IMAGE` nicht aus.** Für ein anderes Image muss der `image:`-Eintrag geändert oder eine explizite Compose-Override-Datei verwendet werden. Ältere Hinweise zur Umgebungsvariable sind überholt. `.env` wird von Compose injiziert; der Flask-Code lädt diese Datei nicht selbst.

SQLite verwendet Fremdschlüssel, WAL und einen Busy-Timeout. Modelle: `User`, `Game`, `GamePlatformOwnership`, `GameWriteReceipt`. Benutzer sind voneinander getrennt. Spiel-Updates verlangen eine Version; auch reine Plattformänderungen erhöhen sie. Veraltete Versionen liefern 409. Erfolgreiche Android-Schreibvorgänge und ihre UUID-Belege werden atomar gespeichert und bei Wiederholung wiedergegeben. DELETE besitzt weiterhin keine Versionsprüfung.

Web und Android verwenden gemeinsame Konten, Sitzungscookies und CSRF-Schutz. `/api/session` liefert API-Version 1 und `idempotent_game_writes`; `/api/games` liefert die gesamte eigene Sammlung und die erlaubten Besitzplattformen. `/api/search` sucht serverseitig nach Titel/Jahr oder numerischer IGDB-ID. `/api/igdb-token` liefert angemeldet und CSRF-geschützt einen befristeten Zugang für direkte Android-Suchen, niemals das Twitch-Client-Secret.

Die Weboberfläche verwendet Jinja, Bootstrap, Chart.js und neun ES-Module. Das Theme ist kontobezogen gespeichert; Sortierung und Covergröße liegen im Browser. Die PWA cached statische Ressourcen und zeigt bei fehlendem Netzwerk eine Hinweisseite. Sie hat keine Offline-Sammlung oder Schreibwarteschlange. Bootstrap, Chart.js, einige Plattformlogos und Cover benötigen externe Ressourcen.

## Android

Aktuell `versionName 0.7`, `versionCode 4`, Paket `de.gamescollection.viewer`, minSdk 26, compile/targetSdk 36, JDK 17. Kotlin 2.2.20, Android Gradle Plugin 8.13.2 und Gradle-Wrapper 8.13 sind im Projekt festgelegt. Die Release-Variante ist signiert und nicht debuggable; Shrinking bleibt deaktiviert.

Die Oberfläche bietet Sammlung, Filter, Statistik und Profil in Cinematic Dark und Editorial Shelf. Android-Theme und die lokale Auswahl von 1–3 Postern pro Reihe (Standard 2) werden gerätebezogen gespeichert. Kontoregistrierung/-verwaltung erfolgen im Browser.

Sammlung und Warteschlange werden zusammen in einer atomar ersetzten JSON-Datei pro Server/Konto gesichert. Neue lokale Spiele besitzen negative IDs. Vor dem ersten Sendeversuch sind Vorgänge bearbeitbar; danach bleiben UUID und Payload unverändert. Erst nach bestätigtem Schreiben und anschließendem Sammlungsabruf wird der Auftrag entfernt. Abgelehnte Vorgänge bleiben sichtbar. Synchronisierung erfolgt beim Verbinden/Anmelden, bei Online-Änderungen und manuell; kein Hintergrundworker und kein automatischer Netzwerkrückkehr-Trigger.

Remember-Cookies und IGDB-Zugang werden über Android Keystore/AES-GCM geschützt; Passwörter werden nicht persistiert. IGDB nutzt einen separaten HTTP-Client ohne Sammlungs-Cookies. Entwürfe liegen im ViewModel/SavedStateHandle und sind an Server/Konto gebunden. HTTP-Abbruch, IO-Initialisierung und Hintergrundberechnung für Filter/Statistik sind umgesetzt.

## Lokale Änderungen und Dateien

Vor Beginn bereits vorhanden:

- Neues verbindliches Icon (weißer Controller vor drei violett/blauen Spielcovern) einschließlich Android-Launcher, Web-/PWA-Icons, Branding und Exportskript; alte Icon-Dateien teilweise gelöscht.
- Android-Auswahl der Posteranzahl statt des bisherigen adaptiven Covergrößen-Sliders.
- Play-Store-Dokumentation und Ergänzungen in README, Projektkontext und Reviewdateien.
- Eine lokale Änderung an `.env.example`; keine Zugangswerte übernommen oder geändert.

Die neuen Verzeichnisse `design/`, `tools/`, einige Ressourcen und `android/PLAY_STORE.md` waren noch unversioniert. Ein frischer GitHub-Klon enthält diese lokalen Ergänzungen daher nicht vollständig.

Im untersuchten Projektordner fehlen `.env` und `data/`. Eine produktive Serverdatenbank wurde hier somit nicht vorgefunden; ihr Standort außerhalb des Ordners und der aktuelle Serverbetrieb wurden nicht geprüft. Das ist keine Aussage über einen Datenverlust auf dem tatsächlichen Server.

Vorhanden und ignoriert sind `android/signing.properties`, `android/keystores/`, `android/local.properties` und `android/.toolchain/`. Keine Schlüssel neu erzeugt. Die bestehende APK trägt den lokalen Icon-Stand; das ältere AAB wurde in dieser Sitzung nicht neu gebaut. APK, AAB und GitHub-/Play-Artefakte dürfen trotz gleicher Versionsnummer nicht als bytegleich betrachtet werden.

## Prüfungen am 23.10.2026

| Prüfung | Ergebnis |
| --- | --- |
| GitHub-App / Git Fetch | Repository erreichbar; Fetch erfolgreich, `main` gleich `origin/main` |
| Backend mit System-Python | Start scheitert an fehlendem Flask; auch pip fehlt im System-Python |
| Backend im vorhandenen Image `:0.7` | **36 Tests bestanden**, aktuelles `files/` und `tests/` lesend eingebunden, Netzwerk deaktiviert, ausschließlich temporäre Testdatenbank |
| Android `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` | **BUILD SUCCESSFUL**, 63 Tasks: 2 ausgeführt, 61 up-to-date; kein erzwungener vollständiger Testneulauf |
| Zugehörige Android-Testberichte | **26 Tests**, 0 Fehler, 0 übersprungen; 5 Testklassen |
| Release-Lint-Bericht | **0 Befunde** |
| APK mit `apksigner verify --verbose` | **Verifies**, APK Signature Scheme v2 gültig |
| `git diff --check` | Ohne Befunde |

Android nutzte vorhandenes JDK 17/SDK 36 und die bestehende private Signierung. Gradle benötigte wegen lokaler Socket-Beschränkungen einen Lauf außerhalb der Sandbox. Keine App installiert oder deinstalliert. Hinweise zu SDK-XML-Versionen und Gradle-9-Kompatibilität bleiben bestehen; Flask-Login meldet im Container eine `datetime.utcnow()`-DeprecationWarning.

Lokale APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`.

Kein neues Container-Image gebaut, kein Produktivcontainer ersetzt, kein neuer Gunicorn-Smoke-Test, keine aktuelle Browser-/Smartphone-Sichtprüfung und keine Live-IGDB-Abfrage. Die Backendtests verwenden Flask-Testclients; die Androidtests sind JVM-Tests und ersetzen keine Geräte-/Lifecycle-/Keystore-Prüfung.

## Veröffentlichungen und Fortsetzung

Die Dokumentation nennt Server 0.7 (`docker-v0.7`) und Android 0.7 (`android-v0.7`) als veröffentlichte Versionen. Die aktuellen GitHub-Release-Assets und GHCR-Digests wurden nicht erneut online verifiziert. Der Play-Stand stammt aus dem Protokoll vom 20.10.2026: interner Test aktiv, Download vom Betreiber bestätigt, keine bestätigte öffentliche Produktion. Play Console wurde am 23.10.2026 nicht erneut geöffnet.

Vor nächster Arbeit beachten:

1. Lokalen Arbeitsbaum erhalten und für einen später beauftragten Commit gezielt prüfen. Icon-/Posteränderungen sind noch nicht vollständig auf GitHub.
2. Vor Änderungen am realen Server dessen Deployment, `.env`, Datenverzeichnis und Sicherung klären. `create_all()` ist keine allgemeine Datenbankmigration; `/health` prüft die Verbindung, nicht das vollständige Schema.
3. Geräteprüfungen aus `ANDROID_REVIEW.md` nachholen: Offline-Neustart, Antwortverlust, Konflikte, Server-/Kontowechsel, Rotation/Prozesswiederherstellung, Keystore, beide Designs und große Sammlungen. API-Pagination und Antwortlimits fehlen weiterhin.
4. Play-Arbeiten anhand von `android/PLAY_STORE.md` fortführen und dann den aktuellen Console-Stand prüfen. Code 4 ist laut Protokoll bereits verwendet. Lokale APK-Signatur und Google-App-Signatur bleiben getrennt; vor Installationswechsel Offline-Vorgänge synchronisieren.
5. Ältere datierte Abschnitte in `PROJECT_CONTEXT.md`, Reviews und Release Notes als historische Stände lesen. Insbesondere 0.6-/Code-3-Angaben, frühere fehlende Offline-Funktionen und `GAMES_COLLECTION_IMAGE` beschreiben nicht den aktuellen Stand.

Diese Sitzung ergänzt nur die Bestandsaufnahme und Einstiegshinweise sowie die lokale Git-Remote-Konfiguration. Produktcode, Versionen, Tags, Nutzdaten und Veröffentlichungen bleiben unverändert.
