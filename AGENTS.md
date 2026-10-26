# Anweisungen für Games Collection

Aktueller Wiedereinstieg nach erneuter Codex-Projektzuordnung am 23.10.2026: [docs/WIEDEREINSTIEG.md](docs/WIEDEREINSTIEG.md). Enthält Repository-Verbindung, lokalen unveröffentlichten Stand und tatsächlich ausgeführte Prüfungen; vor Fortsetzung zusammen mit dem aktuellen Arbeitsbaum beachten.

Gilt für das gesamte Repository. Stand: 20.10.2026. Technische Bestandsaufnahme und offene Punkte stehen in [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md); Einrichtung in [README.md](README.md) und [android/README.md](android/README.md). Die Android-Stabilitätsprüfung mit priorisierten Findings und Fortsetzungsschritten steht in [ANDROID_REVIEW.md](ANDROID_REVIEW.md). Stand, Signierung, Prüfungen und Fortsetzung der Google-Play-Veröffentlichung stehen in [android/PLAY_STORE.md](android/PLAY_STORE.md).

## Zusammenarbeit und belegte Präferenzen

- Verbindlicher Dokumentationsauftrag vom 23.10.2026: Relevante Projektentscheidungen und Arbeitsfortschritte fortlaufend in Markdown festhalten, ohne erneute Aufforderung. Bei wesentlichen Arbeitsschritten spätestens vor der Übergabe `PROJECT_CONTEXT.md` und passende Fachdateien aktualisieren: Entscheidung und Begründung, umgesetzter Stand, tatsächlich ausgeführte Prüfungen, offene Punkte und nächste Schritte. Dauerhafte Vorgaben zusätzlich hier pflegen; Einrichtung in den READMEs, Android-/Serverprüfungen in den Reviewdateien und Play-Veröffentlichungen in `android/PLAY_STORE.md`. Aktuellen Stand klar von datierter Historie sowie Benutzerbestätigungen von selbst verifizierten Ergebnissen unterscheiden. Überholte Angaben kennzeichnen, bestehende lokale Änderungen erhalten und keine Geheimnisse dokumentieren. Dokumentationspflege ist keine Freigabe für Commit, Push oder Veröffentlichung.
- Mit dem Benutzer auf Deutsch kommunizieren; UI-Texte sind überwiegend deutsch.
- Projektname bleibt **Games Collection**. Pixel-/Game-/Album-Namen wurden erwogen, aber nicht übernommen.
- Verbindliches Projekt-Icon seit 20.10.2026: weißer Controller vor drei violett/blauen Spielcovern (gewählte Variante 5). Master: `design/brand/games-collection-master.png`; Exporte für Android, Web/PWA, Play Store und GitHub mit `python tools/export_icons.py` (Pillow). Motiv künftig beibehalten; keine alten Controller-/Emoji-Motive als Projektlogo einsetzen. Details: `design/brand/README.md`.
- Der Benutzer lernt Git/GitHub: bei entsprechenden Fragen konkrete Befehle und deren Wirkung erklären. Bei angefordertem Commit/Push die tatsächlich ausgeführten Schritte nachvollziehbar zusammenfassen.
- Moderne, platzsparende und konsistente Oberflächen sind wichtig. Desktop und Mobilansicht sowie **Cinematic Dark** und **Editorial Shelf** berücksichtigen.
- Fehler sollen verständlich angezeigt werden; Dialoge bei fehlgeschlagenem Speichern nicht kommentarlos schließen.
- Vor Änderungen den aktuellen Quellcode, Arbeitsbaum und vorhandene Hinweise prüfen. Bestehende lokale Änderungen erhalten. Gesprächsverlauf und Code können unterschiedliche Stände beschreiben.

## Projektentscheidungen erhalten

- Selbst gehosteter Docker-Server mit persistentem SQLite-Verzeichnis; Gunicorn statt Flask-Entwicklungsserver im Container.
- Server-Image: `ghcr.io/mehmet-b-90/games_collection:latest` für AMD64/ARM64. Compose lädt das fest eingetragene Image; `GAMES_COLLECTION_IMAGE` wird aktuell nicht ausgewertet. Für lokale Builds `docker build -t games-collection:local .` verwenden, den `image:`-Eintrag bzw. eine Compose-Override-Datei auf dieses Image setzen und mit `docker compose up -d --pull never` starten. Workflow `.github/workflows/docker-publish.yml` prüft Backend und Containerstart vor Veröffentlichung. Neue Server-Versionstags verwenden `server-v*` (bisherige `docker-v*` bleiben historisch erhalten), Android-Tags bleiben davon getrennt. Keine feste Docker-Version erfinden.
- Historisches Server-Release 0.8: Release-Tag `docker-v0.8`, Image `:0.8`, beauftragt auf `feature/popscore`. Tag-Push startet die Veröffentlichung auch vom Feature-Branch; `latest` bleibt an `main` gebunden. Workflow-Erfolg vor Nutzung bestätigen. Eine neue `release-notes/server-<Version>.md` im Release-Commit auf `main` fordert nach erfolgreicher Image-Veröffentlichung Tag und GitHub Release an; für normale Änderungen keine neue Release-Notes-Datei anlegen. Vorhandene Versionstags nicht verschieben.
- Datenbank heißt `database.db`, nicht mehr `games.db`. Containerstandard: `/app/data/database.db`, Compose-Bind-Mount `./data:/app/data`.
- Android ist eine **native Kotlin-/Jetpack-Compose-App**, keine WebView und aktuell kein Flutter-Projekt. Server und Benutzerkonten werden mit der Webanwendung geteilt.
- Reguläre Android-Weitergabe ab 18.10.2026 als signierter **Release-Build** (`testReleaseUnitTest lintRelease assembleRelease`), Debug bleibt für Entwicklung. Dateiname `Games-Collection-<versionName>-release.apk`. Release ist nicht debuggable; Shrinking ist derzeit bewusst deaktiviert. Private Signierung in ignorierter `android/signing.properties` und `android/keystores/`; keinen Debug-Fallback oder automatische Schlüsselneuerzeugung beim Build ergänzen. Die einmalige Erzeugung via `android/create-release-key.py` verweigert Überschreiben. Vor Verteilung Schlüssel/Konfiguration sicher sichern und auf weitere Rechner übertragen. Neuer Release-Schlüssel ist nicht mit bisherigen Debug-Signaturen updatekompatibel; niemals zur Umstellung ungefragt deinstallieren, insbesondere bei ausstehenden Offline-Änderungen.
- Beide Designs erhalten; Webdesign ist kontobezogen gespeichert, Androiddesign und Android-Covergröße gelten lokal auf dem Gerät.
- Altersfreigabe: **USK bevorzugen, sonst PEGI**, andere Systeme nicht als Ersatz speichern.
- IGDB-Suche über Titel und optionales Release-Jahr erhalten. Besitzplattformen und IGDB-verfügbare Plattformen sind unterschiedliche Informationen.
- Android: Aktuell `versionName '0.8'`, `versionCode 5` (GitHub-APK; Play-Test weiterhin 0.7 / Code 4); nicht als 2.0.0 bezeichnen. Versions-/Tagänderungen mit dem konkreten Auftrag abstimmen; bei neuen installierbaren Releases den Android-VersionCode beachten.
- Google Play: kostenlos, Deutsch, Typ App; persönliches verifiziertes Entwicklerkonto. Interner Test 0.7 / Code 4 seit 20.10.2026 aktiv, drei Konten in ausgewählter Testerliste; Testdownload inzwischen vom Betreiber bestätigt, Geräte-/Funktionsprüfung und öffentlicher Produktionszugang noch offen. Code 4 ist in Play bereits verwendet, neue Bundles brauchen einen höheren Code. Interner Test ersetzt nicht den geschlossenen Test mit zwölf Testern und 14 Tagen fortlaufender Teilnahme.
- Play App Signing verwendet auf ausdrücklichen Wunsch einen Google-Schlüssel. Der bestehende lokale Release-Schlüssel bleibt für AAB-Uploadsignierung und direkte APKs erhalten. Unterschiedliche Signaturen von Play- und bisherigen Release-APKs sind akzeptiert; keinen privaten Schlüsselimport/PEPK-Workflow wieder aufnehmen. Vor Wechsel Offline-Änderungen sichern/synchronisieren, niemals ungefragt deinstallieren. Google-Signierung, lokaler Uploadschlüssel und GitHub-APK getrennt behandeln.

## UI-Konventionen aus bisherigen Aufträgen

- Auftrag vom 24.10.2026: „Aktuell beliebt“ ausschließlich unter **Entdecken**, sowohl Web als auch Android; keine beliebten Titel in der Sammlung. „+ Hinzufügen“ öffnet den vorhandenen vorausgefüllten Editor. IGDB-Bewertung und verfügbare Plattformen nicht als persönliche Bewertung oder Besitz übernehmen.

- Suche als ein zusammenhängendes Feld mit integriertem Symbol; keine doppelte Umrandung.
- Android-Texteingaben an den Filterfeldern ausrichten: abgerundete Kontur, kleine interne Beschriftung, einheitliche Abstände, Typografie und Theme-Farben. Gemeinsame `FormTextField`-/`FormField*`-Bausteine verwenden.
- Mobile Webnavigation kompakt und schwebend. Android-Bottom-Bar seit Auftrag vom 23.10.2026 nach Screenshot als durchgehende Material-3-Fläche bis zum unteren Systembereich, mit pillenförmigem aktivem Symbolhintergrund und Beschriftungen darunter; Farben aus dem jeweiligen App-Design. Redundante Navigationsaktionen vermeiden: im Web kein zusätzlicher mobiler Filterknopf oben, kein doppelter Desktop-Statistikknopf und kein zusätzlicher Cinematic-Sidebar-Einstellungseintrag.
- Web-Abmelden liegt im Kontodialog. Android-Profil enthält Einstellungen, Kontoverwaltung im Browser und Abmelden; unter Einstellungen liegen Design, Covergröße und Serverwechsel.
- Android-Sammlung: keine große Überschrift „Meine Sammlung“, Suche über Top-Bar und schwebendes Feld, Kennzahlen auf der Statistikseite, kompakte Sortierung.
- Bei Coverkarten keine Texte an Rundungen abschneiden. Plattformnamen und Logos nicht überlagern; wo kein Logo vorhanden ist, lesbare Kurzbezeichnung verwenden. Keine Garantie auf echte Logos für jede Plattform.
- Im Web-Spieleditor steht auf Desktop links Format → Status → Bewertung, rechts eine höhere Plattformauswahl. Suchvorschläge und Buttonbeschriftungen müssen mobil ausreichend Platz erhalten.
- Statistikbeschriftungen lesbar halten; Prozentwerte in Klammern, auch innerhalb ausreichend großer Tortenstücke.

## Code und Datenkonsistenz

- Vorhandene Modularisierung weiterführen: Python-Blueprints und zentrale Validierung; ES-Module im Web; Compose-Bausteine, ViewModel, API-Client und SessionStore in Android. Neue gemeinsame Logik nicht mehrfach kopieren.
- Bestehenden Stil beibehalten: Python mit vier Leerzeichen und `snake_case`; JavaScript-ES-Module; Kotlin/Java mit den bestehenden Namens- und Einrückungskonventionen. Es ist kein repositoryweiter Formatter-/Linter-Standard festgelegt.
- API-Eingaben serverseitig prüfen; unerwartete Felder, ungültige Typen und unzulässige Werte nicht still übernehmen. Besitzplattformen gegen `PLATFORMS` validieren.
- Bewertung ist optional (`null`) oder eine ganze Zahl 0–10. IGDB-ID ist eine positive Ganzzahl, keine URL oder frei formatierter Text.
- Eigentumsprüfung und Benutzertrennung erhalten. Spiele-Updates benötigen `version`; Konflikte als HTTP 409 behandeln, nicht still überschreiben.
- Fremdschlüssel, Unique-/Check-Constraints und kaskadierendes Löschen erhalten. Schreibfehler müssen Rollback und passende Fehlermeldung auslösen.
- Android-Spieländerungen senden eine stabile `Idempotency-Key`-UUID. Vorgangsbeleg und Mutation müssen atomar committen; bei unklarem Ausgang dieselbe Kennung und denselben Payload erneut senden. Belege nicht unabhängig von einer definierten Wiederholungsfrist löschen. Neue Clients verlangen die Server-Capability `idempotent_game_writes`; alte Webclients ohne Header bleiben kompatibel.
- CSRF-Schutz gilt für Web **und** Android. Nicht durch Ausnahmen „reparieren“; bestehende einmalige Token-Erneuerung nur bei explizitem CSRF-Fehler erhalten.
- Keine Geheimnisse in Git, Dokumentation, APK oder Logs übernehmen. `.env`, Datenbanken, Keystores, lokale SDK-Pfade und Build-Caches bleiben lokal. `SECRET_KEY` ist nicht das Twitch-Client-Secret.
- Zertifikatsprüfungen und servergebundene Cookies nicht abschwächen. HTTP bleibt ausdrücklich zugelassener lokaler Testbetrieb.

## Daten, Git und Veröffentlichung

- Frühere Zustimmung zum Löschen einer Testdatenbank ist **keine dauerhafte Löschfreigabe**. Vor destruktiven Aktionen aktuellen Datenbestand und Auftrag prüfen; bei Unklarheit nachfragen.
- Der Benutzer benötigte damals keine Migration der gelöschten Testdatenbank. Daraus keine allgemeine migrationsfreie Strategie ableiten. `create_all()` migriert bestehende Tabellen nicht vollständig.
- Repository: `https://github.com/mehmet-b-90/games_collection` (**Unterstrich**). Nicht mit `games-collection` verwechseln. `origin` vor Push prüfen.
- Commit, Push, Tags, Release und APK-Upload nur im Rahmen eines entsprechenden Auftrags ausführen. Ein APK-Build ist keine Veröffentlichung. Keine vorbestehenden Änderungen ungeprüft mitcommitten.
- Bei einer Übergabe per Commit/Push diese Projekthinweise und den Kontext mit dem tatsächlich übertragenen Quellstand abgleichen. Die Commit-ID mit `git log -1` ermitteln; keine selbstreferenzielle Commit-ID in die Dateien schreiben. Ein Quellcode-Push aktualisiert eine vorhandene GitHub-Release-APK nicht automatisch.
- Bei ausdrücklich angeforderten Android-Veröffentlichungen README-Downloadlink, Tag, Version und hochgeladene APK konsistent halten. Private Signaturschlüssel niemals einchecken; für Updates ist dieselbe Signatur erforderlich.
- Play-Upload, internen/geschlossenen Test und öffentliche Produktion ausdrücklich unterscheiden. Ein Play-Auftrag aktualisiert keine GitHub-Assets oder Tags automatisch. Automatischer Play-Installationsschutz ist derzeit aktiviert; Auswirkungen auf andere Verteilungswege vor weiteren Releases prüfen. Tester-Adressen, Demo-Zugangsdaten und andere Secrets nicht in Projektnotizen übernehmen.

## Verifikation und Übergabe

- Backend: `python -m unittest discover -s tests -v` in einer Umgebung mit installierten `requirements.txt`. Die Tests konfigurieren ihre eigene temporäre Datenbank; keine echte Datenbank als Testziel verwenden.
- Android: in `android/` mit JDK 17 und SDK 36 regulär `./gradlew testReleaseUnitTest lintRelease assembleRelease` (lokale Release-Signierung erforderlich); ergänzend für Entwicklung `./gradlew testDebugUnitTest lintDebug assembleDebug`. Lint behandelt Warnungen als Fehler (Ausnahmen stehen in der Gradle-Konfiguration).
- Review und Umsetzung vom 17.10.2026: F1–F5 aus `ANDROID_REVIEW.md` sind im Quellcode bearbeitet; Geräte-/Lifecycle- und Performanceprüfung stehen aus. Entwürfe und ausstehende Vorgänge bleiben an Server/Konto gebunden; API-Erstellung ausschließlich über die suspending IO-Factory. Die Prüfungen liefen auf `linux-pc-mehmet`, Fortsetzung auf `linux-laptop-mehmet`.
- Docker bei Server-/Containeränderungen passend prüfen; einen laufenden Container nicht ohne Bezug zum Auftrag ersetzen. Auf Fedora können Bind-Mount-Rechte und SELinux relevant sein; nicht pauschal deaktivieren.
- UI-Änderungen möglichst in beiden Designs und relevanten Bildschirmgrößen prüfen. Build/Lint beweisen keine korrekte Darstellung auf einem echten Smartphone. Fehlende Sichtprüfung ausdrücklich nennen.
- Am Ende Änderungen, tatsächliche Prüfergebnisse und verbleibende Einschränkungen kurz benennen. Bei APK-Builds den erzeugten Dateipfad angeben; lokale APK und GitHub-Release unterscheiden.
- Diese Dateien bei wesentlichen Architektur-/Workflowänderungen aktualisieren. Unbestätigte Annahmen als offen kennzeichnen; historische nummerierte Listen nicht rekonstruieren, wenn ihre Inhalte fehlen.

- Ab Version 0.7 bei jeder ausdrücklich beauftragten Veröffentlichung GitHub Release Notes mit Änderungen, Voraussetzungen, Prüfungen und bekannten Einschränkungen erstellen; Android-Tag, README-Link und signierte APK konsistent halten.

- Ab 25.10.2026: künftige Server-Releases mit Tag `server-v<VERSION>`, Titel und Markdown-H1 `Games Collection v<VERSION>`, Abschnitt `Neue Funktion` gemäß `release-notes/TEMPLATE.md`. Der Workflow ergänzt `Veröffentlichtes Image` mit Digest und setzt das GitHub-Release-Label `Latest` (`--latest`). Das Docker-Image-Tag `latest` bleibt davon unabhängig an `main` gebunden. Vorhandene Tags/Releases nicht umbenennen oder verschieben.

- Android-Release-Titel seit Betreiberkorrektur am 25.10.2026: `Android-App v<VERSION>`; Android-Tags bleiben `android-v<VERSION>`. Der Betreiber hat den GitHub-Titel von 0.8 selbst korrigiert.
