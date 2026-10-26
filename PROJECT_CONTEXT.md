# Games Collection – Projektkontext

## Übernahme nach main – 26.10.2026

- Betreiber bestätigt, dass PopScore im Alltag gut funktioniert, und beauftragt den Merge. Schreibweise `feature/popcore` anhand des einzigen vorhandenen Feature-Branches als `feature/popscore` verstanden. Arbeitsbaum vor Beginn sauber; lokaler Merge nach `main` konfliktfrei vorbereitet. Feature-Branch bleibt erhalten.
- Workflow berücksichtigt bereits vorhandene Server-Versionstags beim Übernehmen von Release Notes nach main: bestehende Version nicht erneut veröffentlichen, sondern nur latest/SHA bauen. Explizite Tag-Veröffentlichungen behalten ihre Prüfung gegen abweichende Commit-Ziele. Bestehende Versionstags bleiben unverändert.
- Backend: 44 Tests erneut im vorhandenen geprüften lokalen 0.8-Image ohne Netzwerk erfolgreich. Android-Produktcode seit dem verifizierten 0.8-Build unverändert; kein neuer APK-/Play-Release. Kein erneuter Geräte-/Sichttest. Neue Betreiberpräferenz dauerhaft in AGENTS.md: Android-Release-Titel `Android-App v<VERSION>`; 0.8 laut Betreiber selbst umbenannt.
- Verifiziert: vier Workflow-Shellblöcke mit `bash -n`; bestehende Release-Version auf main mit gestubbtem Git erfolgreich übersprungen; `git diff --check` ohne Befunde. Öffentlicher HTTPS-Abgleich bestätigt Remote-main und Feature-Branch unverändert auf den erwarteten Ausgangscommits.
- Nach Push nach main aktualisiert erst der erfolgreiche Docker-Workflow `latest`; laufende Server werden dadurch nicht automatisch neu gestartet. SSH-Remote-Abgleich/Push und CI-Status werden bei der Übergabe separat angegeben.


## Android-Veröffentlichung 0.8 – 25.10.2026

- Expliziter Auftrag: Android auf 0.8 erhöhen, signierte APK bauen und auf GitHub veröffentlichen. VersionCode auf 5 erhöht; bestehender Release-Schlüssel unverändert. Tag `android-v0.8`, Titel `Games Collection v0.8`, Label Latest; Notes `release-notes/android-v0.8.md`, Downloadlinks in beiden READMEs aktualisiert. Google Play bleibt beim internen Test 0.7 / Code 4; kein AAB-/Play-Upload.
- Enthalten: Entdecken mit PopScore, Projekt-Icon, Posteranzahl, Material-3-Navigation, GET-Verbindungswiederholungen und HTTP-Protokollauswahl. Entdecken benötigt Server 0.8 mit `/api/discover`; persönliche Bewertung/Besitz nicht aus IGDB übernehmen.
- Verifikation: `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` mit lokaler JDK-17-/SDK-36-Toolchain erfolgreich (63 Tasks, 17 ausgeführt / 46 aktuell). Vorhandene Unit-Testberichte: 34 Tests ohne Fehler, kein erzwungener Neulauf. Frisches Release-Lint: 0 Befunde. APK-v2-Signatur gültig, Zertifikat stimmt mit bestehendem Release-Schlüssel überein; `aapt` bestätigt 0.8 / Code 5, minSdk 26 / targetSdk 36. SDK-XML-/Gradle-9-Hinweise bleiben. Kein neuer Geräte-/Lifecycle-/Keystore-Test.
- APK: `android/app/build/outputs/apk/release/Games-Collection-0.8-release.apk`. Keine Installation/Deinstallation oder Nutzdatenänderung. Die zuvor vorbereitete Umstellung auf `server-v*` und künftige Release-Gestaltung wird mit diesem Quellstand übertragen; bestehende Server-/Android-Tags bleiben unverändert. Commit-ID mit `git log -1` ermitteln.


## Server-Release-Konvention – 25.10.2026

- Auftrag anhand des Screenshots: künftige Docker-Builds über `server-v*`, Release-Titel/H1 `Games Collection v<VERSION>`, Abschnitt `Neue Funktion`, anschließend automatisch `Veröffentlichtes Image` mit Digest; GitHub-Release-Label ausdrücklich `Latest`.
- Workflow umgestellt: Trigger, Versionsableitung, Tagprüfung/-erzeugung, Image-Metadaten und Publish-Bedingungen verwenden `server-v`. Release-Erstellung nutzt `--latest`; Container-Tag `latest` bleibt an `main` gebunden. Vorlage: `release-notes/TEMPLATE.md`. Voraussetzungen/Prüfungen/Einschränkungen bei Relevanz knapp aufnehmen, Details in den Fachdateien erhalten.
- Verifikation: alle vier Workflow-Shellblöcke mit `bash -n` erfolgreich geprüft, Tagpräfix im Workflow konsistent, `git diff --check` ohne Befunde. `actionlint` lokal nicht vorhanden; kein GitHub-Actions-Probelauf oder neuer Containerbuild (Produktcode unverändert).
- Bestehende Tags und veröffentlichte Releases unverändert; keine neue Version, kein Containerdeployment, Commit oder Push im Rahmen dieser Änderung. Arbeitsbaum war zu Beginn sauber auf `feature/popscore` mit Upstream. Die früheren Push-Blocker-Einträge sind historische Versuche; aktueller Remote-Veröffentlichungsstatus wurde hier nicht erneut online geprüft.


## Übergabe und Server-Release 0.8 – 24.10.2026

- Expliziter Auftrag: aktuellen Stand auf `feature/popscore` committen/pushen und Docker-Server 0.8 veröffentlichen. `origin` auf `https://github.com/mehmet-b-90/games_collection.git` geprüft und frisch gefetcht; Remote-Branch und Tag `docker-v0.8` existierten vor der Übergabe nicht.
- Geprüfter gemeinsamer Stand umfasst PopScore, vorhandene Branding-/Icon-/UI-/Android-Verbindungskorrekturen und Dokumentation. Keine Secrets, privaten Schlüssel, Datenbanken oder Buildausgaben werden übertragen. Android bleibt 0.7 / Code 4; kein APK-/Play-Upload.
- Release Notes: `release-notes/server-0.8.md`. Tag-Push startet den vorhandenen Docker-Workflow für `:0.8` (AMD64/ARM64) und anschließend GitHub Release Notes. `latest` bleibt an `main` gebunden; kein Merge nach main und kein laufender Produktivcontainer ersetzt. Compose für diesen Stand ausdrücklich auf `:0.8` setzen.
- Neu verifiziert: lokales 0.8-Image im Docker-Format mit Podman gebaut, 44 Backendtests bestanden, isolierter Gunicorn-Container mit HTTP 200 / `status: ok`. Erster Testmount scheiterte am SELinux-Kontext; lesender Mount mit `:z` erfolgreich.
- Android-Release-Prüfung mit vorhandener `.toolchain/env.sh` erfolgreich: 63 Tasks, 2 ausgeführt / 61 aktuell; kein erzwungener Testneulauf. Vorhandene Berichte: 34 Tests ohne Fehler, Lint ohne Befunde. Ein erster Lauf ohne lokalen Gradle-Cache scheiterte offline an fehlenden Plugins; korrekt konfigurierte Wiederholung erfolgreich. SDK-XML-/Gradle-9-Hinweise bleiben.
- Gerätesichtprüfung und erneuter Live-IGDB-/Web-Sichttest wurden bei der Übergabe nicht durchgeführt; frühere Featureprüfungen siehe unten. Lokale APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`. Veröffentlichungsstatus über GitHub Actions prüfen; Commit-ID mit `git log -1` ermitteln.


## Aktuell beliebt / PopScore – umgesetzt am 24.10.2026

- Verbindliche Auswahl: beliebte Titel ausschließlich im Reiter **Entdecken**, in Web und Android. Kein PopScore-Abschnitt in der Sammlung. „+ Hinzufügen“ öffnet den vorhandenen Editor mit IGDB-Metadaten; persönliche Bewertung und Besitzplattformen bleiben ungesetzt. Bereits vorhandene IGDB-Titel sind als „In Sammlung“ markiert.
- Implementiert auf `feature/popscore`: authentifiziertes `GET /api/discover`, bis zu 100 Titel nach IGDB-Aufrufen (`popularity_type = 1`), geordneter Detailabruf und gemeinsamer Cache pro Serverprozess. 24 Stunden frisch, bei Fehlern höchstens sieben Tage alter Stand mit Kennzeichnung; 60 Sekunden Fehler-Backoff. Kein Datenbankschema geändert. Keine Garantie einer identischen IGDB-Startseitenrangfolge.
- Web: separater Reiter, responsive Coverkarten in beiden Designs, bestehender IGDB-Editor wiederverwendet, eigene Lade-/Fehler-/Leerzustände und „Aktualisieren“. Sammlung behält Filter und Sortierung beim Reiterwechsel; PopScore-Karten werden nicht von Sammlungsfiltern erfasst. Service-Worker-Cache auf v3 samt neuem Modul aktualisiert.
- Android: eigener Navigationsbereich, native Compose-Coverkarten und zustandsgetrennter Abruf. Fehler beim Entdecken schalten die Sammlung nicht offline. Ergebnisse/Jobs werden beim Konto-/Serverwechsel verworfen; ausstehende Entwürfe und Schreibvorgänge verwenden weiterhin die vorhandene Editor-/Offline-Logik. Scrollzustände der Seiten werden beim Reiterwechsel erhalten. Rangliste nur im Arbeitsspeicher, kein persistenter Android-PopScore-Cache.
- Verifiziert: **44 Backendtests**, **34 Android-JVM-Tests**, Release-Lint ohne Befunde, `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich, APK-v2-Signatur gültig, JavaScript-Syntax und `git diff --check` ohne Befunde. Live-IGDB-Abruf lieferte 100 Titel. Gunicorn-Vorschau im isolierten Container mit temporärer Datenbank; Web in beiden Designs auf Desktop und bei 390 px geprüft. Hinzufügen/Speichern/Besitzmarkierung und Erhalt des Sammlungsfilters im Browser bestätigt.
- Lokale APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`. Version 0.7 / Code 4 unverändert (Prüfbuild, kein neues veröffentlichtes Release). Echter Android-Gerätetest sowie Lifecycle-/Offline-UI-Prüfung für Entdecken bleiben offen.
- Bestehende lokale Änderungen erhalten. Kein Commit, Push, Produktivdeployment oder Upload. Für den Einsatz benötigt die App den neuen Serverendpunkt; ältere Server liefern eine Aktualisierungsaufforderung. Details: `design/popscore/README.md`, `SERVER_REVIEW.md`, `ANDROID_REVIEW.md`.


## Aktuell beliebt / PopScore – Entwurfsphase 24.10.2026

- Auftrag: „Aktuell beliebt“ für Web und native Android-App einführen; ausdrücklich zuerst Mockups zur Gestaltung vorlegen. Lokalen Branch mit `git switch -c feature/popscore` angelegt. Vorbestehende unveröffentlichte Änderungen aus `main` bleiben im Arbeitsbaum erhalten und sind nicht automatisch Teil eines späteren Feature-Commits.
- Drei interaktive Gestaltungsvorschläge erstellt: Coverleiste über der Sammlung, eigene Entdecken-Ansicht und kompakte Rangliste. Web-/Android-Vorschau sowie Cinematic Dark/Editorial Shelf vorgesehen; Details und offene Entscheidungen in `design/popscore/README.md`.
- Aktueller Stand: ausschließlich Entwürfe und Dokumentation, noch keine PopScore-Produktlogik oder API-Anbindung. Beispielcover aus der Nutzervorlage; keine Live-Rangliste. Auswahl des Entwurfs steht aus. Kein Commit, Push, Build oder Veröffentlichung.


## Kurzzeitige Android-Verbindungsfehler 23.10.2026

- Auftrag: häufige Meldung „Server nicht erreichbar“ trotz bestehender Verbindung untersuchen. Im Code bestätigt: `ApiClient` deaktiviert OkHttp-Verbindungswiederholungen; `CollectionViewModel.action` setzt nach einem einzelnen Netzwerk-`IOException` sofort `connected=false`. Ohne manuellen neuen Lauf bleibt dieser Zustand bestehen. IGDB-Provisionierungsfehler werden separat abgefangen und erklären diese Meldung nicht. Der konkrete ursprüngliche Netzwerkfehler auf dem Gerät ist ohne Laufzeitdaten nicht nachgewiesen.
- `ApiClient` wiederholt nun ausschließlich GET-Leseanfragen bei Transportfehlern höchstens zweimal, nach 500/1500 ms. Jede Wiederholung liest den vollständigen Response erneut; auch vorzeitig abgebrochene Bodies werden berücksichtigt. TLS-Fehler und empfangene HTTP-Fehlerantworten lösen keine Wiederholung aus. Cancellation unterbricht auch die Wartezeit. Vorhandene Request-Timeouts bleiben bestehen, weshalb echte Nichterreichbarkeit länger bis zur Meldung brauchen kann.
- Schreibanfragen werden dadurch nicht automatisch wiederholt; UUID-/Payload-Bindung, CSRF-Erneuerung und Kontoprüfung vor der Warteschlange bleiben erhalten. Kein Hintergrunddienst oder Netzwerkrückkehr-Trigger ergänzt. Prüfergebnisse und Geräteprüfungen siehe aktuellen Nachtrag in `ANDROID_REVIEW.md`.
- Bestehende lokale Änderungen erhalten; Version 0.7 / Code 4 unverändert. Kein Commit, Push, Serverdeployment oder Upload. Ein späterer Play-Upload benötigt weiterhin einen höheren VersionCode.

## Android-Bottom-Bar nach Screenshot 23.10.2026

- Auftrag: Bottom-Bar an die vom Betreiber angehängte Vorlage anpassen. In `CollectionUi.kt` durchgehende Material-3-`NavigationBar` statt schwebendem, schmalem Container; aktive Symbole mit pillenförmigem Hintergrund, 24-dp-Symbole und darunter dauerhaft sichtbare Beschriftungen. Systemnavigationsabstände übernimmt die Material-Komponente; Fläche reicht bis zum unteren Systembereich.
- Sammlung, Filter, Statistik und Profil samt Aktionen erhalten; die bisherige Hervorhebung angewandter Filter bleibt bestehen. Farben kommen aus Cinematic Dark bzw. Editorial Shelf, keine Übernahme der fremden Produkttexte oder grünen Screenshot-Farbpalette. Dauerhafte Gestaltungsvorgabe in `AGENTS.md` angepasst.
- Prüfungen und Einschränkungen siehe aktuellen Nachtrag in `ANDROID_REVIEW.md`. Version 0.7 / Code 4 unverändert; bestehende lokale Änderungen erhalten. Kein Commit, Push, Upload oder Release.

## Anmeldung ohne Beachtung der Groß-/Kleinschreibung 23.10.2026

- Auftrag: Benutzernamen bei der Anmeldung unabhängig von Groß-/Kleinschreibung behandeln. Im untersuchten aktuellen Quellstand bereits zentral über `normalize_username` (`strip().casefold()`) und `User.username_key` umgesetzt, sowohl `/login` als auch `/api/session`; Android reicht den Namen an diese API weiter. Keine Änderung an Produktlogik oder Datenbankschema erforderlich. Angezeigte Namensschreibweise und Passwortprüfung bleiben erhalten.
- Zusätzlicher Regressionstest bestätigt Web-/API-Anmeldung mit gemischter Schreibweise, Umlauten, ß/SS und umgebenden Leerzeichen sowie Ablehnung bei geänderter Passwortschreibweise. Alle **37 Backendtests bestanden**, aktueller Quellcode im vorhandenen Image `:0.7`, ohne Netzwerk, nur temporäre Testdatenbank. Details in `SERVER_REVIEW.md`.
- Tatsächlich auf dem Server eingesetzter Quellstand und Live-Anmeldung nicht geprüft. Falls dort Groß-/Kleinschreibung weiter relevant ist, als nächsten Schritt Deploymentstand prüfen; keine unbelegte Datenmigration oder Client-Kleinschreibung ergänzen. Kein Produktivcontainer ersetzt, kein Commit, Push oder Release.

## Serveradresse automatisch ergänzen 23.10.2026

- Android ergänzt beim Verbinden eine Serveradresse ohne Protokoll anhand der Option „HTTP für lokale Tests zulassen“: aktiviert → `http://`, deaktiviert → `https://`. Umsetzung zentral in `ServerAddress.normalize`, das bereits von der Serverauswahl verwendet wird. Explizite Protokolle bleiben erhalten; HTTP ohne Freigabe bleibt gesperrt, Origin-/TLS-Prüfungen unverändert.
- Regressionstest ergänzt für beide Optionen, Leerzeichen, Port, localhost, IPv6 und explizites HTTPS trotz HTTP-Freigabe. Einrichtungshinweis in `android/README.md` aktualisiert. Vorbestehende lokale Änderungen erhalten; Android-Version weiterhin 0.7 / Code 4, kein Commit, Push oder Upload.
- Prüfstand siehe aktuellen Nachtrag in `ANDROID_REVIEW.md`; Geräteprüfung bleibt offen.

## Ergänzungen und Dokumentationsauftrag 23.10.2026

- Der Betreiber verlangt fortlaufende Markdown-Dokumentation aller relevanten Entscheidungen und Fortschritte für eine nachvollziehbare Fortsetzung über Chats hinweg. Dauerhafte Arbeitsregel in `AGENTS.md` ergänzt; diesen Kontext und betroffene Fachdateien bei wesentlichen Fortschritten aktualisieren, einschließlich Prüfungen, Einschränkungen und nächsten Schritten.
- `.env` wurde laut Betreiber mit den Zugangswerten für lokale Tests wiederhergestellt. Vorhandensein und Git-Ignorierung anschließend geprüft; Werte nicht ausgegeben, Gültigkeit noch nicht durch Live-Anfragen getestet. Die frühere Feststellung einer fehlenden `.env` beschreibt ausschließlich den Beginn der Bestandsaufnahme.
- Der Betreiber bestätigt erneut die bereits erfolgte Veröffentlichung der Android-App als internen Test in Google Play Console. Dokumentierter Versionsstand bleibt 0.7 / Code 4; keine neue Console-Prüfung oder öffentliche Produktionsfreigabe. Einzelheiten und Fortsetzung in `android/PLAY_STORE.md`.
- Nur Dokumentation aktualisiert; kein Commit, Push oder Release. Geräte-/Live-IGDB-Prüfungen bleiben offen.

## Wiedereinstieg 23.10.2026

Aktuelle Bestandsaufnahme nach erneutem Einbinden des vorhandenen Codex-Projektordners: **[docs/WIEDEREINSTIEG.md](docs/WIEDEREINSTIEG.md)**. Dort stehen Architektur, lokale/unveröffentlichte Änderungen, vorhandene Werkzeuge und die heutigen Prüfergebnisse. Die folgenden datierten Abschnitte bleiben als Historie erhalten.

- Codex-Projekt vorhanden; `origin` wegen fehlender SSH-Authentifizierung auf HTTPS umgestellt. Fetch erfolgreich, `main` und `origin/main` auf `9b51ecc`; lokale Änderungen erhalten, kein Commit/Push.
- 36 Backendtests mit aktuellem Quellcode im vorhandenen Container-Image bestanden. Android-Release-Prüfung erfolgreich, überwiegend up-to-date; vorhandene Berichte: 26 Tests erfolgreich, Lint ohne Befunde. APK-Signatur verifiziert; keine Geräteprüfung oder Veröffentlichung.
- Zu Beginn der Bestandsaufnahme keine `.env` und kein `data/`; `.env` inzwischen wiederhergestellt (siehe Ergänzung oben). Private Android-Signierung und Werkzeugcache vorhanden. Produktivserver nicht untersucht.
- Aktuelle Compose-Datei hat einen festen `image:`-Eintrag und wertet `GAMES_COLLECTION_IMAGE` nicht aus. Ältere Hinweise dazu sind überholt; aktuelle README beachten.

## Projekt-Icon 20.10.2026

- Betreiber hat Variante 5 (weißer Controller vor drei violett/blauen Spielcovern) verbindlich für Android, Play Store, GitHub und weitere Bereiche ausgewählt. Master und technische Exporthinweise: [design/brand/README.md](design/brand/README.md).
- Im lokalen Quellstand als Android-Launcher-Icon und in Server-/Anmeldeansicht, Web-Branding, Favicons, Apple-/PWA-Icons und Repository-README integriert. Store- und GitHub-PNGs vorbereitet. Keine Veröffentlichung, kein Upload und keine Versions-/Tagänderung im Rahmen dieses Icon-Auftrags.
- Icon-Prüfung: JDK 17/SDK 36, `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich; 26 Tests ohne Fehler/übersprungene Tests, Lint erfolgreich. Signierte lokale APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`. Maskenvorschau für Android/PWA/GitHub visuell geprüft, keine Prüfung im echten Launcher oder der laufenden Weboberfläche. Backend-Teststart scheitert in dieser Python-Umgebung an fehlendem Flask; Backend-Quellcode unverändert. Bereits hochgeladenes AAB und GitHub-Release nicht aktualisiert.

## Aktuelle Übergabe 20.10.2026: interner Google-Play-Test

- Persönliches Entwicklerkonto laut Betreiber vollständig verifiziert. Games Collection wurde mit Standardsprache Deutsch, Typ App und kostenloser Verteilung angelegt; rechtliche Erklärungen hat der Betreiber selbst akzeptiert.
- Android weiterhin **0.7 / Code 4**, App-ID `de.gamescollection.viewer`, minSdk 26, targetSdk 36. Signiertes AAB hochgeladen, von Google verarbeitet und als Release `4 (0.7)` gespeichert. Nach den weiteren Benutzeraktionen ist der interne Track als **Aktiv** mit diesem Release bestätigt. Ausgewählte E-Mail-Liste „Tester“ enthält drei Konten; tatsächliche Testbeitritte/Installationen sind damit nicht belegt. Keine öffentliche Produktionsveröffentlichung und kein geschlossener Test bestätigt.
- Betreiber hat Teilnahme-Link geöffnet, Store meldete „Nicht gefunden“. Google nennt mehrere Stunden mögliche Verzögerung bei der ersten Testveröffentlichung. Inzwischen hat der Betreiber den erfolgreichen Download der Testversion über Google Play bestätigt; die anfängliche Nichtverfügbarkeit ist behoben. Eine vollständige Funktionsprüfung bleibt offen. Gleiche eingetragene Google-Konten für Teilnahme und Play Store verwenden; tatsächlichen Testbeitritt prüfen. Kein erneuter Upload allein wegen dieses anfänglichen Fehlers nötig. Keine automatische Überwachung eingerichtet.
- **Signaturentscheidung:** Google-App-Signaturschlüssel verwenden; fehlende direkte Updatekompatibilität mit bisherigen Release-APKs ausdrücklich akzeptiert. Privater Schlüsselimport abgebrochen, kein privater Schlüssel exportiert/übertragen. Leerer Vorbereitungsordner auf Wunsch entfernt. Lokalen Release-Schlüssel für AAB-Uploadsignierung und direkte APKs erhalten. Keine App deinstalliert; vor Wechsel alle ausstehenden Offline-Änderungen synchronisieren. Automatischer Play-Installationsschutz ist aktiviert, Auswirkungen auf GitHub-Verteilung noch zu klären.
- Lokaler JDK-17-/SDK-36-Build: `testReleaseUnitTest lintRelease assembleRelease bundleRelease --offline --no-daemon` erfolgreich. Unit-Test-/Analyseaufgaben überwiegend aus aktuellem Cache, vorhandene Berichte: **26 Tests ohne Fehler oder übersprungene Tests**. Lint erfolgreich. AAB unter `android/app/build/outputs/bundle/release/app-release.aab`, lokale APK unter `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`. Keine neue Geräte-/Sicht-, Lifecycle-, Keystore- oder Live-IGDB-Prüfung; Backend/Container für diesen Vorgang nicht neu geprüft.
- Release-Vorschau: drei Warnungen, keine angezeigten Fehler. Fehlende Testerauswahl inzwischen bearbeitet; keine R8-Mapping-Datei, weil Shrinking deaktiviert; fehlende native Debug-Symbole betreffen AndroidX graphics-path. Versionshinweise vom Betreiber eingefügt und in Vorschau geprüft. Kein Codeumbau zum Ausblenden der Warnungen.
- Bundle enthält den lokalen Arbeitsstand einschließlich der vorbestehenden Posteranzahl-Änderungen (1–3, Standard 2). Vorbestehende Änderungen an `.env.example`, `PROJECT_CONTEXT.md`, `android/README.md`, `CollectionUi.kt` und `CollectionViewModel.kt` erhalten. Kein Commit/Push, neuer Tag, GitHub-Release oder Asset-Austausch während dieses Play-Vorgangs. Bestehender GitHub-Download bleibt unabhängig; Quellcode-Push ersetzt ihn nicht.
- **Fortsetzung:** Nach bestätigtem Testdownload Geräteprüfungen durchführen; Store-Eintrag mit Grafiken, Datenschutz/Datensicherheit, Kontolöschung, Support und Demo-Serverzugang vervollständigen. Danach geschlossenen Test mit mindestens zwölf fortlaufend für 14 Tage angemeldeten Testern durchführen, anschließend Produktionszugang beantragen. Drei Einträge des internen Tests zählen dafür nicht. Beim Trackwechsel internen Test verlassen und geschlossenen Test separat beitreten. Neue Uploads benötigen einen VersionCode über 4; Version/Release-Auftrag abstimmen.
- Vollständiges Vorgangsprotokoll, Console-/Teilnahme-Links, öffentliche Zertifikat-Fingerabdrücke, Versionshinweise, Store-Textentwürfe und offene Aufgaben: **[android/PLAY_STORE.md](android/PLAY_STORE.md)**. Dort beim nächsten Schritt Status aktualisieren; Tester-Adressen oder Zugangsdaten nicht dokumentieren.

## Ergänzung 19.10.2026: erneute Serverprüfung

- Befunde, Korrekturen, Diagnose und Grenzen stehen in [SERVER_REVIEW.md](SERVER_REVIEW.md). Spiel-Updates versionieren jetzt auch reine Plattformänderungen; die Adminvergabe bei paralleler Erstregistrierung erfolgt atomar im INSERT.
- Zentrale DB-/Exceptionbehandlung mit Rollback, deutschen API-/HTML-Fehlern, Fehler-ID und Betreiber-Traceback. SQL-Parameter werden in DB-Exceptions verborgen; externe Fehlerlogs enthalten Typ/HTTP-Status statt potenziell vertraulicher Exceptiontexte. Strengere Typ-/Zahl-/URL-Prüfung, robuste IGDB-Antwortverarbeitung und `no-store` für APIs/Fehler/persönliche HTML-Seiten.
- **36 Backend-Tests bestanden**, zusätzlich lokaler Gunicorn-Smoke-Test mit temporärer DB. Live-IGDB-/Last- und visuelle Prüfungen offen. Der Container wurde anschließend für die Server-Veröffentlichung 0.7 geprüft. Vorbestehende lokale Änderungen einschließlich entfernter Schema-Kompatibilität bleiben erhalten; keine Migration oder Änderung von Nutzdaten während der Prüfung; anschließende Server-Veröffentlichung siehe unten.

Stand: **20.10.2026**. Grundlage sind der lokal eingesehene Quellcode, die zugänglichen Konversationen und die geprüften Play-Console-Seiten. Datierte Abschnitte unten erhalten historische Prüfstände; für die aktuelle Play-Übergabe gilt der Abschnitt oben. Die systematische Android-Stabilitätsprüfung einschließlich anschließender Umsetzung steht in [ANDROID_REVIEW.md](ANDROID_REVIEW.md); sie ist keine vollständige Sicherheitsprüfung. Frühere Assistentenantworten sind nur teilweise verfügbar; insbesondere lässt sich der Inhalt früherer nummerierter Optimierungslisten nicht vollständig rekonstruieren.

## Ziel und aktueller Arbeitsstand

Selbst gehostete Verwaltung persönlicher Videospielesammlungen mit mehreren Benutzerkonten, Weboberfläche/PWA und nativer Android-App. Produktname weiterhin Games Collection.

- Git-Remote lokal: `origin` → `https://github.com/mehmet-b-90/games_collection.git`.
- Lokal am 20.10.2026 ermittelter letzter Commit: `9b51ecc` – `Add desktop and Android screenshots to README`. Arbeitsbaum enthält lokale Änderungen; für diese Dokumentationsübergabe kein Remote-Fetch oder Push. Historischer Ausgangscommit der Android-Review vom 17.10.2026: `127619d`. Vor weiterer Übergabe `git log -1` und `git status -sb` erneut prüfen.
- Android: Version **0.7**, Code **4**; davon unabhängiges Server-Image: `ghcr.io/mehmet-b-90/games_collection:latest`, alternativ ein veröffentlichter `sha-<Commit-ID>`-Tag über `GAMES_COLLECTION_IMAGE`.
- Aktueller Auftrag: wichtige Projekt- und Play-Vorgangsinformationen in Markdown festhalten. Kein Auftrag für Commit, Push, weitere Veröffentlichung oder Serveränderungen. Frühere Umsetzungs-/Veröffentlichungsaufträge sind in den datierten Abschnitten dokumentiert.
- Frühere Geräte-/Review-Arbeiten liefen auf **linux-pc-mehmet**, Fortsetzung war für **linux-laptop-mehmet** vorgesehen. Diese Rechnerzuordnung ist kein Beleg für den aktuellen Hostnamen. Vor Weiterarbeit auf anderem Rechner Arbeitsbaum prüfen und private Build-/Signierungsdateien sicher separat übertragen.
- README-Download verweist auf `android-v0.7` / `Games-Collection-0.7-release.apk`. Aktiver interner Play-Test separat; kein öffentlicher Store-Download bestätigt. Die lokale Posteranzahl-Änderung ist im hochgeladenen AAB enthalten, ein Austausch der GitHub-APK damit nicht belegt.

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
- Im geprüften Quellstand enthalten: Coverclip nur auf Bild statt auf gesamte Karte; gruppierte Profilaktionen; gemeinsame `FormTextField`-Gestaltung; Profil → Einstellungen für Design, Dropdown mit 1–3 Postern pro Reihe (Standard 2) mit Reset und Serverwechsel.
- Die Posteranzahl steuert `GridCells.Fixed`, wird lokal als `posters_per_row` gespeichert und überlebt Serverwechsel. Tatsächliche Coverbreiten richten sich nach verfügbarer Breite/Spaltenzahl. Seit 19.10.2026 ersetzt die feste Auswahl den bisherigen Covergrößen-Slider; alte `cover_size`-Werte werden nicht übernommen, da daraus ohne Displaybreite keine eindeutige Spaltenzahl folgt. Ohne neue Auswahl gelten 2 Poster pro Reihe.
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

## Ergebnis des Push-Versuchs am 24.10.2026

Der lokale Commit für PopScore/Server 0.8 wurde erstellt (aktuelle ID mit `git log` ermitteln). `git push -u origin feature/popscore` scheiterte an GitHub-HTTPS-Authentifizierung: „Invalid username or token“. Anschließendes `git ls-remote` bestätigte, dass der Remote-Feature-Branch weiterhin fehlt. Kein Tag angelegt/gepusht, kein GitHub Release und kein GHCR-0.8-Upload gestartet. Lokales geprüftes Image: `localhost/games-collection:0.8`. Git-Anmeldung erneuern, danach Feature-Branch pushen und `docker-v0.8` am Feature-Release-Commit anlegen/pushen; CI-Erfolg bestätigen. `.env.example` bleibt als unabhängige lokale Kommentaränderung uncommittet. Die vorläufige automatische Freigabeablehnung wegen vermuteten unvollständigen Stagings wurde durch Prüfung aller 59 Dateien geklärt; anschließend wurde der Commit freigegeben.

## Ergänzung zur Übergabe – `.env.example`

Auf ausdrücklichen Folgeauftrag wird auch die vorbestehende Entfernung des Kommentars in `.env.example` committet; die Datei enthält ausschließlich Platzhalter, keine realen Zugangswerte. SSH-Einrichtung für GitHub wird dem Betreiber als Anleitung übergeben; noch keine Schlüssel erzeugt oder Remote-Adresse geändert. Push und Docker-Veröffentlichung bleiben bis erfolgreicher Git-Authentifizierung offen.

### Veröffentlichung bestätigt – 25.10.2026

Branch `feature/popscore` und Tag `android-v0.8` erfolgreich gepusht. GitHub Release unter https://github.com/mehmet-b-90/games_collection/releases/tag/android-v0.8 veröffentlicht: Titel `Games Collection v0.8`, Label `Latest`, APK `Games-Collection-0.8-release.apk` (7,83 MB). Der auf der Release-Seite angezeigte SHA-256 `851979af53265d0812e0ae8ca27db725ab3ea68c24942b370d17f81a5d255d4f` stimmt mit der lokal gebauten und signaturgeprüften APK überein. Beide README-Downloadlinks zeigen auf dieses Asset. Release-Tag bleibt am geprüften Quellcommit; spätere Dokumentationscommits verschieben ihn nicht. Kein Play-Upload.

### Merge-Ergebnis und Push-Blocker – 26.10.2026

Lokaler Merge von `feature/popscore` nach `main` erfolgreich und konfliktfrei committet. Push nach `origin main` blieb ohne Antwort und endete nach begrenzter Laufzeit mit Timeout (Exit 124); auch SSH-Remoteprüfung auf Port 22 sowie GitHubs Alternative Port 443 mit strikter Hostschlüsselprüfung endeten im Timeout. Öffentlicher HTTPS-Abgleich funktioniert und bestätigte zuvor main noch auf dem alten Ausgangsstand. Merge daher noch nicht auf GitHub bestätigt; kein erfolgreicher CI-/latest-Build behauptet. Nächster Schritt bei wieder erreichbarem SSH: `git push origin main`, danach Docker-Workflow prüfen. Feature-Branch und vorhandene Releases/Tags bleiben erhalten.
