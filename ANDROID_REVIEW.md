# Android-Codeprüfung vom 17.10.2026

## Veröffentlichung 0.8.1 abgeschlossen – 31.10.2026

- Geprüften gemeinsamen Quellstand nach `main` committed und per SSH nach GitHub gepusht. Release-Commit: `55f1fe253cae6facbfe3b17d959fd632bc67f0bb`. Die Git-Konfiguration enthielt keine Autorenidentität; ausschließlich für Commit/Tag dieselbe Identität wie in den letzten Projektcommits verwendet (keine dauerhafte globale Konfigurationsänderung). Alle bestehenden lokalen Titeltypfilter-, Versions- und Dokumentationsänderungen einschließlich OCR-Verwerfungsnotiz enthalten; keine Secrets/Nutzdaten.
- **Server/Web veröffentlicht:** [Games Collection v0.8.1](https://github.com/mehmet-b-90/games_collection/releases/tag/server-v0.8.1), Tag `server-v0.8.1` am Release-Commit; GitHub-Label **Latest** bestätigt. [Docker-Workflow 41150785641](https://github.com/mehmet-b-90/games_collection/actions/runs/41150785641) abgeschlossen mit **success**; Tests im Image, Containerstart, AMD64-/ARM64-Upload und Server-Release-Job erfolgreich.
- GHCR direkt geprüft: `ghcr.io/mehmet-b-90/games_collection:0.8.1` und `:latest` zeigen beide auf `sha256:e1f99156da28454284e30d79fa21e5f5bbaa97e762024f4b97cea2ccdb62ba98`; Manifest enthält **linux/amd64** und **linux/arm64**. Digest stimmt mit dem automatisch ergänzten Server-Release überein. Keine vorhandenen Versionstags verschoben.
- **Android veröffentlicht:** [Android-App v0.8.1](https://github.com/mehmet-b-90/games_collection/releases/tag/android-v0.8.1), Tag `android-v0.8.1` ebenfalls am Release-Commit. Asset `Games-Collection-0.8.1-release.apk`, 8.207.358 Bytes, Status `uploaded`; veröffentlicht, kein Draft/Prerelease. GitHub-Asset-Digest und lokale APK stimmen überein: `sha256:452d297b5349c0e3abfdb421d3b5c676226eee016c9085c82ccecd78c7339139`. Upload über bestehende angemeldete GitHub-Weboberfläche, da die GitHub-Anbindung keine Release-Asset-Uploadfunktion anbietet.
- Release Notes, README-Downloadlinks und AGENTS.md auf den veröffentlichten Stand abgeglichen. Vorprüfung: 46 Backendtests im lokalen Image erfolgreich; Android-Berichte 35 Tests ohne Fehler, Lint ohne Befunde, v2-Signatur und 0.8.1 / Code 6 bestätigt. Aktueller Android-Release-Prüflauf nutzte vorhandene Ergebnisse; kein erzwungener Neulauf. Einzelheiten und Einschränkungen in den vorangehenden datierten Prüfprotokollen und Release Notes.
- Keine Play-Veröffentlichung (weiterhin zuletzt bestätigt intern 0.7 / Code 4), keine App-Installation/-Deinstallation und kein Austausch eines laufenden Produktivservers. Geräte-/Lifecycle-/Keystore-Prüfung und vollständiger neuer Live-End-to-End-Test gegen IGDB bleiben offen. Das Serverupdate muss der Betreiber separat mit seiner Compose-Konfiguration ausführen. Ein nachfolgender reiner Dokumentationscommit hält diesen verifizierten Abschluss fest; Versions-Tags und Release-Artefakte bleiben am Release-Commit.

## Veröffentlichung 0.8.1 beauftragt – 31.10.2026

- Neuer expliziter Auftrag: aktuellen Server-/Web- und Android-Stand committen, nach GitHub pushen und als Docker-Image bzw. signierte APK veröffentlichen. Dieser Auftrag ersetzt die früheren Aussagen „nicht beauftragt“ in den datierten Abschnitten darunter. Google Play und laufendes Produktivdeployment bleiben außerhalb dieses Auftrags.
- Ausgangsstand auf `main`: frisch per SSH gefetcht und mit Remote-main identisch; Tags `server-v0.8.1` und `android-v0.8.1` vor Beginn nicht vorhanden. Alle 19 bereits lokal geänderten Dateien geprüft: Titeltypfilter, Versionierung und zugehörige Dokumentation einschließlich des vorhandenen OCR-Verwerfungsnachtrags. Keine Secrets oder Nutzdaten im Commit.
- Release Notes `release-notes/server-0.8.1.md` und `release-notes/android-v0.8.1.md` erstellt; README-Links, Android-Version 0.8.1 / Code 6 und APK-Dateiname konsistent vorbereitet. Server-Push nach `main` fordert über die neue Notes-Datei die automatische Veröffentlichung für AMD64/ARM64, `server-v0.8.1`, Titel „Games Collection v0.8.1“ und Latest an. Android separat unter `android-v0.8.1`, Titel „Android-App v0.8.1“; keine bestehenden Tags verschieben.
- Lokale Veröffentlichungsvorprüfung: Image `localhost/games-collection:0.8.1` im Docker-Format mit Podman gebaut; **46 Tests im Image ohne Netzwerk bestanden**, ausschließlich temporäre Testdaten. Separater Container ohne Netz mit frischer Containerdatenbank: Gunicorn-Healthcheck **HTTP 200 / status ok**; nur diesen Testcontainer anschließend entfernt. Erster Tmpfs-Versuch war mit lokalen Podman-Mountoptionen inkompatibel; erfolgreicher Lauf ohne diesen Mount. Keine Datenbank oder Produktivcontainer verändert.
- Android erneut `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon`: erfolgreich, 63 Tasks (2 ausgeführt, 61 aktuell). Vorhandene Berichte: **35 Tests**, keine Fehler/übersprungen; Lint 0 Befunde. Kein erzwungener Testneulauf. Signierte APK `android/app/build/outputs/apk/release/Games-Collection-0.8.1-release.apk`: v2-Signatur gültig, Release-Zertifikat und 0.8.1 / Code 6 bestätigt. Bekannte Toolchain-/Flask-Login-Hinweise bleiben. Keine neue Geräteprüfung.
- Veröffentlichung läuft erst nach dem Commit/Push; CI-Abschluss, Image-Digest und tatsächlicher APK-Upload werden als eigener Nachtrag verifiziert und dokumentiert. Lokale Commit-ID mit `git log -1` ermitteln.

## Versionsstand 0.8.1 vorbereitet – 31.10.2026

- Expliziter Auftrag: Versionszahlen für Android und Web/Server auf **0.8.1** setzen. Android `versionName '0.8.1'`, `versionCode 6` (zuvor 0.8 / Code 5). Für Web/Server erstmals zentrale Produktversionskonstante `APP_VERSION = '0.8.1'` in `files/config.py` ergänzt und in die Flask-Konfiguration übernommen. API-Protokollversion 1 unverändert; die neue Konstante ersetzt keine Release-Tags oder Image-Tags.
- READMEs und AGENTS.md unterscheiden den lokalen Quellstand von veröffentlichten Artefakten. Lokale Build-/Installationspfade auf 0.8.1 aktualisiert; bestehende GitHub-Downloadlinks, Tags und Image-Beispiele bleiben bei der tatsächlich veröffentlichten 0.8. Play bleibt separat bei 0.7 / Code 4. Keine neuen Release-Notes-Dateien angelegt, da nur die Versionsanpassung beauftragt ist; eine neue Server-Notes-Datei würde beim späteren Main-Push eine Veröffentlichung anfordern.
- Verifiziert: 46 Backendtests erneut erfolgreich in isolierter Python-3.12-Umgebung mit temporärer Testdatenbank; Flask-Konfiguration zeigt 0.8.1. Android `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich (63 Tasks, 17 ausgeführt, 46 aktuell); Unit-Test-Task UP-TO-DATE, kein erzwungener Neulauf der zuvor 35 erfolgreichen Tests. Lint 0 Befunde. APK-v2-Signatur gültig; `aapt` bestätigt 0.8.1 / Code 6. Bekannte SDK-XML-/Gradle-9-/Flask-Login-Hinweise bleiben. `git diff --check` ohne Befunde.
- Lokale signierte Prüf-APK: `android/app/build/outputs/apk/release/Games-Collection-0.8.1-release.apk`. Keine neue Geräteprüfung, Installation, Containerprüfung oder Veröffentlichung; kein Commit, Push, Tag, GitHub-Upload oder Play-Upload. Bestehende lokale Titeltypfilter- und Dokumentationsänderungen erhalten.

## IGDB-Titeltypfilter in Android – 30.10.2026

- Technische Machbarkeit vor Umsetzung anhand der [offiziellen IGDB-Dokumentation](https://api-docs.igdb.com/#game) und einer erfolgreichen Live-Abfrage geprüft: `game_type` verweist auf `game_types`; das frühere `category` ist veraltet. Alle 15 aktuellen Typen mit IDs 0–14 live bestätigt. Gefilterte Battlefield-6-Suche über `game_type.type` liefert Hauptspiel und Phantom Edition als `Main Game`; Editionen sind daher nicht automatisch ausgeschlossen. Ein ergänzender Live-Lauf für numerische Typ-/Jahr-/ID-Kombinationen scheiterte anschließend mit `URLError`; diese Kombinationen sind durch lokale Tests, nicht durch diesen weiteren Live-Lauf belegt. Keine Zugangsdaten ausgegeben oder gespeichert.
- Unter dem optionalen Release-Jahr im Android-Hinzufügen-Dialog das Dropdown **Titeltyp** ergänzt. Standard **Alle Typen**, dazu Spielevollversion, DLC und sämtliche weiteren aktuell bestätigten IGDB-Typen. Vorhandener `ChoiceField` mit gemeinsamen Formularmaßen und Theme-Farben wiederverwendet; Auswahl bleibt bei Konfigurationswechseln erhalten und ist während einer Suche gesperrt.
- Typisierte Auswahl durch ViewModel und separaten IGDB-Client bis zur Abfrage geführt. `game_type` wird mit Release-Jahr bzw. numerischer IGDB-ID vor dem Trefferlimit kombiniert; kein nachträglicher Filter auf zehn ungefilterte Treffer. Kein neues Sammlungsfeld, keine Datenbank-/Server-/Webänderung und kein Serverupdate erforderlich. Bestehenden lokalen OCR-Dokumentationsnachtrag erhalten.
- Verifiziert: `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` mit bestehender JDK-17-/SDK-36-Toolchain und privater Release-Signierung erfolgreich (63 Tasks, 44 ausgeführt, 19 aktuell). **35 Tests**, 0 Fehler/übersprungen; darunter Request-Übertragung des DLC-Typs sowie kombinierte Typ-/Jahr-/ID-Abfragen und ungefilterter Standard. Lint **0 Befunde**, APK-v2-Signatur gültig, `aapt` bestätigt 0.8 / Code 5; `git diff --check` ohne Befunde. Gradle erforderte wegen lokaler Socket-Beschränkung Ausführung außerhalb der Sandbox. Bekannte SDK-XML-/Gradle-9-Hinweise bleiben.
- Lokale Prüf-APK: `android/app/build/outputs/apk/release/Games-Collection-0.8-release.apk`. Versionsnummer unverändert; dies ist kein neuer installierbarer Release auf GitHub/Play. Keine Installation, Veröffentlichung, Commits, Pushes oder Tags. Keine neue Geräte-/Sichtprüfung in beiden Designs; auf kleinen Displays, mit Bildschirmtastatur und nach Rotation noch am Gerät prüfen. Die veröffentlichte GitHub-APK 0.8 enthält den neuen Filter noch nicht.

## Android-Veröffentlichung 0.8 – 25.10.2026

- Expliziter Auftrag: Android auf 0.8 erhöhen, signierte APK bauen und auf GitHub veröffentlichen. VersionCode auf 5 erhöht; bestehender Release-Schlüssel unverändert. Tag `android-v0.8`, Titel `Games Collection v0.8`, Label Latest; Notes `release-notes/android-v0.8.md`, Downloadlinks in beiden READMEs aktualisiert. Google Play bleibt beim internen Test 0.7 / Code 4; kein AAB-/Play-Upload.
- Enthalten: Entdecken mit PopScore, Projekt-Icon, Posteranzahl, Material-3-Navigation, GET-Verbindungswiederholungen und HTTP-Protokollauswahl. Entdecken benötigt Server 0.8 mit `/api/discover`; persönliche Bewertung/Besitz nicht aus IGDB übernehmen.
- Verifikation: `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` mit lokaler JDK-17-/SDK-36-Toolchain erfolgreich (63 Tasks, 17 ausgeführt / 46 aktuell). Vorhandene Unit-Testberichte: 34 Tests ohne Fehler, kein erzwungener Neulauf. Frisches Release-Lint: 0 Befunde. APK-v2-Signatur gültig, Zertifikat stimmt mit bestehendem Release-Schlüssel überein; `aapt` bestätigt 0.8 / Code 5, minSdk 26 / targetSdk 36. SDK-XML-/Gradle-9-Hinweise bleiben. Kein neuer Geräte-/Lifecycle-/Keystore-Test.
- APK: `android/app/build/outputs/apk/release/Games-Collection-0.8-release.apk`. Keine Installation/Deinstallation oder Nutzdatenänderung. Die zuvor vorbereitete Umstellung auf `server-v*` und künftige Release-Gestaltung wird mit diesem Quellstand übertragen; bestehende Server-/Android-Tags bleiben unverändert. Commit-ID mit `git log -1` ermitteln.


## Entdecken / PopScore – Umsetzung und Prüfung 24.10.2026

- Native `DiscoveryScreen` unter eigenem Bottom-Bar-Reiter **Entdecken**; die Sammlung enthält keine beliebten Titel. Beide Themes, lokale Posteranzahl, Rang, Genre, getrennte IGDB-Bewertung, Besitzmarkierung und vorausgefüllter vorhandener Editor. Die Filteraktion wechselt zur Sammlung; Seiten-Scrollzustände bleiben erhalten.
- `ApiClient.discover()` liest den neuen authentifizierten Serverendpunkt. Eigener ViewModel-Job/Zustand, kein globaler Offlinewechsel bei PopScorefehlern. Jobabbruch/Reset und Ergebnisprüfung bei Konto-/Serverwechsel; optionaler erneuter Abruf bei Fehlern, manuelle Aktualisierung, ansonsten bis zu 24 Stunden im RAM. Server-Fallback wird sichtbar gekennzeichnet. Kein persistenter Feed nach Prozessneustart, kein direkter IGDB-Fallback für alte Server.
- JDK 17 / SDK 36 aus vorhandener Toolchain: `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich. **34 JVM-Tests, 0 Fehler**, einschließlich zwei neuer Parsing-/Editor-Datentrennungstests; Lint **No issues found**. APK mit `apksigner verify --verbose` erfolgreich, Scheme v2 gültig. Vorbestehende SDK-XML-/Gradle-9-Hinweise bleiben.
- APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`; Version 0.7 / Code 4 unverändert, keine Veröffentlichung oder Installation. Für neuen Play-Upload ist weiterhin ein höherer Code nötig.
- Noch offen: echte Geräteprüfung beider Designs, kleine Display-/große Schriftgrößen, Entdecken bei Offline-/Sitzungsfehlern und Konto-/Serverwechsel während Abruf. Build/JVM-Tests ersetzen keine Compose-Gerätesichtprüfung. Kein Produktionsserver geändert; neuer Endpunkt muss für Nutzung deployt sein.


## Aktueller Nachtrag: Verbindungswiederholung am 23.10.2026

- Untersucht nach Betreiber-Screenshot mit sofortiger Offline-Meldung. Bestätigter Codepfad: ein einzelner Transportfehler bei Sitzung/Sammlung beendet den Lauf, setzt `connected=false` und verlangt danach manuelle Synchronisierung. OkHttp-Retry war ausdrücklich deaktiviert; kein automatischer Netzwerkrückkehr-Trigger. Welcher Transportfehler am Gerät ursprünglich auftrat (z. B. DNS, Verbindungsabbruch, Timeout), ist ohne Gerätedaten offen. Eine vorhandene WLAN-/Internetverbindung garantiert keine erfolgreiche einzelne Serveranfrage.
- Zentral in `ApiClient.readResponse` begrenzte Wiederholung nur für GET bei `IOException`: maximal drei Versuche insgesamt, 500/1500 ms Abstand, abbrechbar. TLS-Fehler werden sofort weitergereicht; HTTP-Fehler, Redirects und JSON-Verarbeitung werden außerhalb dieser Schleife behandelt. Bestehende Timeouts gelten je Versuch, daher dauert die endgültige Fehlermeldung bei dauerhaftem Ausfall gegebenenfalls länger. Keine automatische Wiederholung von POST/PUT/DELETE, keine Änderung an Auth-/CSRF-/Idempotenzschutz oder Offline-Speicher.
- Fünf neue JVM-Regressionstests simulieren Verbindungsabbruch mit Erholung, abgeschnittenen Sammlungsbody, dreimaligen Ausfall, HTTP 503 ohne Retry und Abbruch während der Wiederholungswartezeit. Bestehender Antwortverlust-Test prüft zusätzlich ausdrücklich, dass ein Write nicht automatisch erneut gesendet wird. Erster Prüflauf erkannte den abgeschnittenen Body als `ProtocolException`; die zunächst zu breite Ausnahme wurde korrigiert, damit auch dieser Transportabbruch wiederholt wird.
- Abschließende Verifikation: `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` mit lokalem JDK 17/SDK 36 und bestehender Signierung erfolgreich; **32 JVM-Tests**, keine Fehler oder übersprungenen Tests, Lint erfolgreich. 63 Tasks, 16 ausgeführt, 47 up-to-date. Gradle benötigte wegen des bekannten Socket-Fehlers einen Lauf außerhalb der Sandbox. SDK-XML-/Gradle-9-Hinweise bestehen fort. `git diff --check` ohne Befunde.
- Lokale Release-APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`, Version 0.7 / Code 4 unverändert; `apksigner verify --verbose` bestätigt gültige v2-Signatur. Geräteprüfung des konkreten Fehlers, WLAN-/VPN-Wechsel und Start nach Standby bleiben offen. Keine Live-Serverabfrage, Installation, Veröffentlichung oder Versionsänderung.

## Aktueller Nachtrag: Bottom-Bar am 23.10.2026

- Nach Screenshot des Betreibers in `CollectionUi.kt` auf Material-3-`NavigationBar`/`NavigationBarItem` umgestellt: durchgehende Fläche einschließlich Systemnavigationsbereich statt schwebendem Container, pillenförmiger aktiver Symbolhintergrund, 24-dp-Symbole und sichtbare Beschriftungen. Beide Designs verwenden ihre eigenen Theme-Farben. Aktionen und Hervorhebung aktiver Filter erhalten; Material-Komponenten übernehmen Auswahlsemantik und Systemabstände.
- Mit bestehendem JDK 17/SDK 36 und privater Signierung `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich: 63 Tasks, 18 ausgeführt, 45 up-to-date; **27 JVM-Tests**, keine Fehler oder übersprungenen Tests, Lint erfolgreich. Gradle lief nach dem bekannten Socket-Fehler außerhalb der Sandbox. SDK-XML-/Gradle-9-Hinweise bleiben bestehen. `git diff --check` ohne Befunde.
- Lokale APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`, unverändert Version 0.7 / Code 4. Signatur mit `apksigner verify --verbose` geprüft: gültig, v2. Kein Commit, Push, Upload oder Release. Keine Geräte-/Sichtprüfung beider Designs und Schrift-/Displaygrößen; ADB konnte innerhalb der Sandbox seinen Socket nicht öffnen, im lokalen SDK kein Emulator/Systemimage gefunden. Visuelle Abnahme bleibt offen.

## Aktueller Nachtrag: Serverprotokoll am 23.10.2026

- Fehlendes Protokoll wird beim Verbinden abhängig von „HTTP für lokale Tests zulassen“ ergänzt: mit Haken `http://`, sonst `https://`. Explizites HTTPS wird nicht umgeschrieben; explizites HTTP ohne Freigabe wird weiterhin abgewiesen. Zentrale Änderung in `ServerAddress.normalize`, zusätzlicher Regressionstest in `ServerAddressTest`.
- Mit vorhandenem JDK 17/SDK 36 und bestehender privater Signierung `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich: 63 Tasks, davon 19 ausgeführt; **27 JVM-Tests bestanden**, keine Fehler/übersprungenen Tests, **Lint ohne Befunde**. Gradle musste wegen blockierter lokaler Socket-Erkennung außerhalb der Sandbox laufen. Bestehende SDK-XML-/Gradle-9-Kompatibilitätshinweise bleiben bestehen.
- Lokale Release-APK: `android/app/build/outputs/apk/release/Games-Collection-0.7-release.apk`. Version 0.7 / Code 4 unverändert, kein Upload oder Play-Release. Keine neue Geräte-/Sichtprüfung; Backend und Container unverändert. Einrichtung und Entscheidung in `android/README.md` und `PROJECT_CONTEXT.md` dokumentiert.

## Aktueller Nachtrag: Play-Test am 20.10.2026

Android 0.7 / Code 4 ist im internen Google-Play-Test aktiv. Der Release-Build mit `testReleaseUnitTest lintRelease assembleRelease bundleRelease` war erfolgreich; Test-/Analyseaufgaben wurden überwiegend aus dem aktuellen Cache wiederverwendet, vorhandene Berichte enthalten 26 bestandene Release-JVM-Tests. Keine neue Geräte-, Lifecycle-, Keystore- oder Live-IGDB-Prüfung; die verbleibenden Geräteprüfungen unten bleiben offen.

Play zeigte Warnungen zu fehlenden Testern, fehlender R8-Zuordnungsdatei und fehlenden nativen Debug-Symbolen, keine als Fehler ausgewiesenen Meldungen. Inzwischen ist die Testerliste mit drei Konten ausgewählt. R8 ist bewusst deaktiviert; die geprüfte native AndroidX-arm64-Bibliothek liegt bereits ohne vollständige Debug-Symbole vor. Diese Hinweise sind keine neuen Stabilitäts-Findings und kein Anlass für einen ungeprüften R8-Umbau. Erfolgreicher Testdownload über Google Play inzwischen vom Betreiber bestätigt; die anfängliche Meldung „Nicht gefunden“ ist behoben. Geräte-/Funktionsprüfungen bleiben offen. Ausführlicher Stand und Signaturentscheidung: [android/PLAY_STORE.md](android/PLAY_STORE.md).

## Historische Grundlage der Review

Geprüfter Quellstand: `127619d` (`Improve Android settings and document project handoff`), Android 0.6 / versionCode 3. Der Arbeitsbaum war zu Beginn sauber. Ausgeführt auf **linux-pc-mehmet**, vorbereitet zur Fortsetzung auf **linux-laptop-mehmet**. Die ursprüngliche Prüfung änderte nur Dokumentation. Anschließend hat der Benutzer die Umsetzung aller fünf Findings einschließlich Commit und Push beauftragt; deren Stand ist im nächsten Abschnitt dokumentiert. Version, Signaturkonfiguration und bestehendes GitHub-Release bleiben unverändert.

## Umsetzung nach der Review

F1–F5 wurden im anschließenden Quellstand bearbeitet. Die ursprünglichen Fundstellen unten bleiben als historische Referenz auf `127619d` erhalten.

| Finding | Umsetzung | Prüfung / Grenze |
| --- | --- | --- |
| F1 | `EditorState` mit Auswahl, Editiermodus und ausstehendem Write im ViewModel; einzelner JSON-Entwurf im `SavedStateHandle`, Wiederherstellung nur für bestätigten Server/Konto. Rotation erhält außerdem Filter/Sortierung im ViewModel. | Serialisierung und Server-/Kontotrennung getestet; tatsächliche Activity-/Prozess-Neuerstellung auf Gerät noch offen. |
| F2 | Stabile UUID und eingefrorener Payload für Create/Update/Delete. Backend-Dekorator `idempotent_game_write` und neue Tabelle `game_write_receipt`, atomarer Commit mit Mutation, Unique `(user_id, key)`, Request-Hash gegen abweichende Wiederverwendung. Unklarer Vorgang bleibt im Dialog wiederholbar. | Antwortverlust und CSRF-Retry clientseitig, echte DB-Replays, parallele Creates, Versionsprüfung, Rollback, Kontotrennung, FK-Cascade und additive Tabellenerstellung serverseitig getestet. |
| F3 | `HttpCall.readBody()` liest per OkHttp-Callback und schließt die Response; Cancellation bleibt bis zum vollständig gelesenen Body an `Call.cancel()` gekoppelt. | Verzögerte Header und Body getestet, Job beendet innerhalb einer Sekunde und der zugrunde liegende Call tatsächlich gecancelt. Server-Commit wird dadurch nicht zurückgenommen. |
| F4 | Privater API-Konstruktor, zentrale suspending `ApiClient.create()`-Factory auf IO; auch beim Kaltstart kein Cookie-/Keystore-Laden mehr im ViewModel-Konstruktor. | Persistence-Aufruf findet nachweislich außerhalb des Aufruferthreads statt. Echter Android Keystore noch nicht instrumentiert geprüft. |
| F5 | Modellumwandlung, Filter/Sortierung und Summary auf Default; abbrechbare Berechnung, Suche 120 ms entprellt, Aggregate einmal je Sammlung. Statistikgruppen bestehen aus einzelnen Lazy-Items. | Berechnung mit 10.000 Spielen und vielen Engines sowie kooperativer Abbruch getestet. Pagination und harte Antwortlimits bleiben mögliche spätere Skalierungsarbeiten; Geräte-Framezeiten nicht gemessen. |

Die API-Version bleibt 1, ergänzt um `capabilities.idempotent_game_writes`. Aktuelle Android-Builds verlangen diese Capability für Spieländerungen; ältere Clients ohne Header bleiben kompatibel. Die zusätzliche Tabelle wird durch `create_all()` angelegt, ohne bestehende Spieletabellen zu verändern. Vorgangsbelege bleiben bis zur Kontolöschung erhalten; es ist bewusst keine zeitgesteuerte Löschung ohne vereinbartes Wiederholungsfenster vorgesehen.

Nach verlorenem Response wird ein Entwurf gesperrt, bis „Vorgang erneut prüfen“ dieselbe Operation bestätigt oder eine eindeutige Ablehnung eintrifft. Dialogschließen behält einen unklaren Vorgang; + oder Spielauswahl öffnen ihn wieder. Bei Sitzungsablauf bleibt Saved-State für erneute Anmeldung am selben Konto erhalten, ausdrückliches Abmelden/Serverwechsel löschen ihn. Das ist kein dauerhafter Offline-Auftragsspeicher.

Neue Verifikation: **25 Backendtests bestanden**, inklusive paralleler idempotenter Writes und Schemaerweiterung mit Bestandsdaten. Android: **17 JVM-Tests bestanden**, **Lint ohne Befunde**, **Debug-Build erfolgreich**. Die Details stehen im aktualisierten Projektkontext. Backend-Abhängigkeiten wurden aus `requirements.txt` isoliert unter `/tmp/games-collection-review-python` installiert und über den gebündelten Python-3.12-Runtime ausgeführt. Die früher fehlende Flask-Installation blockiert diese Umsetzung nicht mehr. Keine reale Datenbank verwendet.

ADB meldete kein verbundenes Gerät; daher keine Gerätesichtprüfung, kein instrumentierter Lifecycle-/Keystore-Test und keine Heap-/Framezeitmessung. Docker ist auf dem Ausführungsrechner nicht verfügbar; kein Container neu gebaut oder ersetzt. Die verbleibenden Geräteprüfungen sind auf `linux-laptop-mehmet` nachzuholen. Der frühere Abschnitt „Durchgeführte Prüfungen“ beschreibt ausschließlich die ursprüngliche Review vor den Fixes.

## Ursprüngliches Ergebnis und Priorität

Fünf Findings der Priorität **P2** (regulär zu beheben), innerhalb dieser Stufe nach Auswirkung geordnet. Kein nachgewiesener P0-/P1-Defekt und kein reproduzierter ungefangener App-Crash. Das ist keine Zusicherung der Crashfreiheit: Geräte-, Prozessneustart-, Keystore- und Speicherdrucktests fehlen. Die Findings unterscheiden beobachtetes Verhalten von aus dem Code abgeleiteten Risiken.

Alle folgenden Quellzeilen beziehen sich auf den oben genannten Commit. Pfade unter `android/app/src/main/java/de/gamescollection/viewer/` sind unten abgekürzt.

### F1 · P2 · Spielentwürfe gehen bei Activity-Neuerstellung verloren

**Stellen:** `CollectionUi.kt:102–104` und `:548`.

`selected`, `edit`, `adding` und der bearbeitete `draft` liegen ausschließlich in `remember`. Bei einer Konfigurationsänderung, etwa Rotation, werden sie neu initialisiert. Die Activity behandelt solche Änderungen nicht selbst. Dadurch verschwindet der Spieledialog einschließlich ungespeicherter Eingaben; das ViewModel mit der Sammlung bleibt bei Rotation dagegen erhalten. Filter und Sortierung werden ebenfalls zurückgesetzt (`:100–101`).

**Fix:** Auswahl, Editiermodus und Entwurf in einem an Server und Konto gebundenen ViewModel-Zustand halten. Für Wiederherstellung nach Prozessende nur die erforderlichen primitiven Entwurfsfelder und IDs über `SavedStateHandle` oder einen passenden Saver sichern, keine komplette Sammlung im Bundle. Bei Logout/Serverwechsel ausdrücklich löschen. Passwort weiterhin nicht speichern.

**Verifikation:** Bestehendes Spiel bearbeiten und neues manuelles Spiel anlegen; jeweils Titel, Jahr, Plattformen und Bewertung ändern, dann `ActivityScenario.recreate()` auslösen. Dialog und Entwurf müssen erhalten bleiben. Prozesswiederherstellung separat prüfen. Statisch belegt, noch kein Instrumentierungstest. Grundlage: [Android-Dokumentation zur Zustandssicherung](https://developer.android.com/develop/ui/compose/state-saving).

### F2 · P2 · Verlorene Speicherantwort kann beim erneuten Speichern Duplikate erzeugen

**Stellen:** `CollectionViewModel.kt:50–60`, `:105–108`; `ApiClient.kt:69`, `:80–81`; serverseitig `files/routes/games.py:81–95`, `files/models.py:33–37`.

Wenn der Server einen manuellen Neuanlage-POST bereits gespeichert hat, die Antwort aber unterwegs verloren geht, erhält die App einen `IOException`. Der Entwurf bleibt mit `id == 0` offen und „Speichern“ wird wieder verfügbar. Erneutes Speichern sendet wieder einen Neuanlage-POST. Manuelle Spiele ohne IGDB-ID haben keine passende Duplikatsperre. Bei Updates kann dieselbe Situation stattdessen in einem 409 mit inzwischen alter Version enden.

**Nachweis:** Eine temporäre MockWebServer-Probe trennte nach Empfang des ersten Requests die Verbindung. Ein erneuter manueller Speicherversuch übermittelte einen zweiten identischen POST ohne Idempotenzschlüssel. Die Probe zeigt den Transport-/Clientpfad, keinen echten Datenbank-Commit; die mögliche doppelte Anlage ergibt sich zusätzlich aus Serverroute und Constraints.

**Fix:** Einen stabilen Vorgangsschlüssel pro Entwurf/Speicheroperation verwenden und serverseitig atomar deduplizieren; beim Wiederholen denselben Schlüssel beibehalten. Bis dahin einen unklaren Speicherstatus als solchen anzeigen und einen Sammlungsabgleich ermöglichen. Den Entwurf dabei erhalten. Ein bloßer Titelvergleich reicht nicht zur zuverlässigen Zuordnung. Keine automatische Wiederholung schreibender Requests bei allgemeinen Netzwerkfehlern ergänzen. Die vorhandene einmalige Wiederholung bei explizitem CSRF-Fehler beibehalten.

**Verifikation:** Response-Verlust nach erfolgreichem Commit simulieren, denselben Vorgang wiederholen, genau ein Spiel bzw. eine Änderung erwarten. Auch Updates und Deletes auf unbekannten Ausgang prüfen.

### F3 · P2 · ViewModel-Abbruch beendet laufende HTTP-Anfragen nicht

**Stellen:** `ApiClient.kt:74–95`, besonders `:80`; `CollectionViewModel.kt:47–49`, `:76`, `:106`.

`withContext(Dispatchers.IO)` verlagert das blockierende `Call.execute()` auf einen Hintergrundthread, koppelt es aber nicht an `Call.cancel()`. Wird das ViewModel endgültig verworfen, kann der laufende Request einschließlich Response-Verarbeitung und Cookie-Persistenz weiterlaufen. Rotation allein verwirft das ViewModel normalerweise nicht. Besonders relevant sind ausstehende Schreiboperationen beim Schließen und späteren erneuten Öffnen der App: Der Client kennt den Ausgang womöglich nicht, und alte Requests können noch dieselbe Cookie-Ablage verändern.

**Nachweis:** Bei einer um 1,8 Sekunden verzögerten Response wartete `cancelAndJoin()` in einer Coroutine um `api.session()` noch **1803 ms**. Die Job-Cancellation hat den HTTP-Aufruf nicht beendet. Das ist eine zeitweilige Ressourcenbindung, kein nachgewiesenes dauerhaftes Speicherleck. Die 40 Sekunden gelten pro Call; ein CSRF-Refresh mit Wiederholung besteht aus mehreren Calls.

**Fix:** Eine suspending HTTP-Brücke mit `enqueue`, `suspendCancellableCoroutine` und `invokeOnCancellation { call.cancel() }` einführen. Responses bei Abbruch und Callback-Rennen zuverlässig schließen und auch das Lesen des Bodys an den Abbruch koppeln. Sessionänderungen weiterhin serialisieren. Cancellation beendet clientseitige Arbeit, garantiert aber keinen Rollback eines bereits serverseitig angekommenen Schreibvorgangs; F2 bleibt zusätzlich erforderlich.

**Verifikation:** Verzögerte Header und verzögerten Body testen, Job abbrechen, zeitnahes Ende und geschlossene Response prüfen. Anschließend ViewModel-Verwerfen und erneutes Starten mit derselben Cookie-Ablage testen. Grundlage: [Kotlin-Dokumentation zu Cancellation und blockierendem Code](https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/coroutines-cancellation.md).

### F4 · P2 · Cookie-Entschlüsselung und Keystore-Zugriffe laufen beim Start auf dem Hauptthread

**Stellen:** `CollectionViewModel.kt:37–40`, `:71`; `ApiClient.kt:20–22`, `:42`; `SessionStore.kt:24–39`.

Das ViewModel wird aus der Activity-Komposition erstellt. Sein Konstruktor erstellt unmittelbar einen `ApiClient`; dessen `ServerCookies` liest synchron die gespeicherten Cookies. Mit persistentem Login folgen SharedPreferences-Lesen, Android-Keystore-Zugriff und AES/GCM-Entschlüsselung auf demselben Thread. Auch die Neuerstellung in `connect()` erfolgt außerhalb des IO-Blocks. Keystore-Ausnahmen werden behandelt, Wartezeiten dagegen blockieren weiterhin die Oberfläche.

**Nachweis:** Eine temporäre Probe bestätigte, dass `CookiePersistence.read()` synchron auf dem Konstruktor-Aufruferthread läuft. Die Zuordnung zum UI-Thread ergibt sich aus dem ViewModel-Erstellungspfad. Keine ANR-Dauer auf einem Gerät gemessen; ANR ist hier ein bedingtes Risiko, kein beobachteter Crash.

**Fix:** API-/Cookie-Initialisierung einschließlich Entschlüsselung innerhalb eines suspendierenden IO-Pfads ausführen. Erst danach den fertigen Client und den Verbindungszustand im Hauptthread veröffentlichen. Initialisierung weiter gegen parallele Login-/Sessionaktionen absichern; die unverschlüsselte In-Memory-Ausweichlösung bei nicht nutzbarem Keystore erhalten.

**Verifikation:** Persistentes Login beim Kaltstart auf einem Gerät prüfen, mit StrictMode/Tracing und verzögerbarer Persistence im Test. UI muss während des Ladens reagieren; auch beschädigte Cookies und ungültiger Schlüssel sollen kontrolliert zur erneuten Anmeldung führen.

### F5 · P2 · Sammlungsumwandlung und Suche/Sortierung blockieren bei großen Sammlungen den UI-Thread

**Stellen:** `CollectionViewModel.kt:86–89`; `CollectionUi.kt:171`, `:299`, `:377–382`, `:393–401`, `:411`; `Models.kt:63–83`.

HTTP-Lesen und das Erstellen des `JSONObject` laufen bereits auf IO. Die anschließende Umwandlung sämtlicher Spiele und Plattformen läuft jedoch nach Rückkehr auf Main. Filterung und Sortierung erfolgen synchron innerhalb von `remember` in der Komposition; `remember` vermeidet Wiederholungen bei gleichen Inputs, verschiebt Arbeit aber nicht auf einen Hintergrundthread. Während der Suche werden zusätzlich alle Treffer gezählt. Statistik und Filteroptionen erzeugen weitere vollständige Zwischenlisten; `Distribution` komponiert sämtliche Gruppen innerhalb eines einzelnen LazyColumn-Items. Besonders bei vielen unterschiedlichen Engines wird dieser Bereich nicht zeilenweise virtualisiert.

Der Server liefert die gesamte Sammlung ohne Pagination (`files/routes/games.py:21–24`). `response.body.string()` hält zudem die vollständige Antwort im Speicher. Dies vergrößert den Skalierungsbedarf, beweist aber noch keinen konkreten OOM. Es wurden keine belastbaren Geräte-Grenzwerte oder Frametimes ermittelt.

**Fix:** Modellumwandlung, Filterung, Sortierung und Aggregation in `Dispatchers.Default` durchführen und fertige immutable Ergebnisse veröffentlichen. Suche bei großen Datenmengen kurz entprellen und veraltete Berechnungen verwerfen; lange CPU-Schleifen gegebenenfalls kooperativ abbrechbar gestalten. Aggregate einmal je Sammlung berechnen, Statistikzeilen als eigene Lazy-Items darstellen. Pagination/Antwortlimits als separate Vertragsänderung mit dem Backend planen, falls reale Sammlungsgrößen dies erfordern.

**Verifikation:** Synthetische Sammlungen etwa mit 1.000/10.000 Spielen und vielen Engines auf einem schwächeren Android-Gerät testen: Erstladen, Eingabe pro Zeichen, Sortieren, Statistikscrollen, Peak-Heap und Framezeiten messen. Die Finding-Basis ist der belegte Threadpfad, keine gemessene Performancegrenze.

## Durchgeführte Prüfungen

- Vollständige Lektüre aller acht Kotlin-Produktdateien und von `ServerAddress.java`, Manifest, Backupregeln, Gradle-Konfiguration sowie beiden Android-Testdateien. Passende Serverrouten, Validierung, Datenmodell und Backend-Tests als API-Vertrag geprüft.
- JDK **17.0.20.1**, vorhandenes SDK 36, Gradle 8.13. `./gradlew testDebugUnitTest lintDebug assembleDebug --offline --rerun-tasks`: **erfolgreich**, 55 Tasks ausgeführt; **9 Projekttests ohne Fehler**, Lint: **No issues found**. Bestehende Warnungen zu SDK-XML-Werkzeugversion, nicht entfernbaren nativen Debugsymbolen und Gradle-9-Inkompatibilität sind Build-/Werkzeughinweise, keine zusätzlichen App-Findings.
- Der erste Gradle-Aufruf im abgeschirmten Prozess scheiterte vor dem Build an der Ermittlung einer Wildcard-IP. Der anschließende Lauf mit erforderlichen lokalen Socket-Rechten war erfolgreich.
- Drei zusätzliche temporäre JVM-Proben: Cancellation bei verzögertem HTTP-Body, erneuter POST nach Response-Verlust und synchroner Persistence-Aufruf. **3/3 erfolgreich**. Ein anfänglich falscher Gradle-Init-Aufruf und ein lokaler IPv6-Verbindungsfehler der Probe wurden korrigiert; letzter Lauf mit explizitem IPv4-Loopback. Probequellen und -berichte liegen ausschließlich unter `/tmp/games-collection-review-*`, werden nicht mit Git übertragen und sind keine neuen Repository-Tests. Die Szenarien sind bei F2–F4 beschrieben.
- Nach den Proben wurden die ursprünglichen Projekttests erneut ohne eingebundene Zusatzquellen ausgeführt; Ergebnis siehe Projektkontext. Reguläre Berichte unter `android/app/build/reports/tests/testDebugUnitTest/` und `android/app/build/reports/lint-results-debug.html` sind lokale, ignorierte Dateien.
- `python -m unittest discover -s tests -v`: **nicht ausführbar**, Importfehler `ModuleNotFoundError: No module named 'flask'`; System-Python hat auch kein pip. Kein Backendtest hat in dieser Sitzung bestanden. Keine Systempakete nachinstalliert und keine Nutzdatenbank als Testziel verwendet.
- Lokale Debug-APK erzeugt: `android/app/build/outputs/apk/debug/app-debug.apk`. Keine Veröffentlichung, kein Upload und keine neue Signaturprüfung durch diese Review.

## Einordnung von Exceptions, Threading und Leaks

Positiv belegt: Responses werden mit `use` geschlossen; Cookie-Zugriffe sind synchronisiert; ViewModel-Aktionen über `busy` serialisiert; `CancellationException` wird weitergeworfen; erwartete Netzwerk-, API- und JSON-Exceptions werden angezeigt. Keine Activity-Referenz im ViewModel oder SessionStore gespeichert; diese verwenden die Application. Compose-Fokuseffekt ist über `LaunchedEffect` an die Komposition gebunden. Coil lädt die Cover; Lazy-Raster und Suchliste verwenden stabile IDs. Serverorigin-Grenzen, deaktivierte Redirects und Zertifikatsprüfung bleiben erhalten.

Kein zusätzlicher dauerhafter Ressourcen-Leak, Main-Thread-Netzwerkaufruf oder konkret erreichbarer ungefangener Null-/Index-Crash bei regulären API-Daten nachgewiesen. Defensive Tests für fehlerhafte JSON-Strukturen, Keystore-Ausfälle und Preferences sowie instrumentierte Lifecycle-Tests fehlen. Insbesondere decken die neun bisherigen Tests weder das ViewModel noch Compose oder Android Keystore ab. Ein grünes Lint-Ergebnis widerlegt F1–F5 daher nicht.

## Fortsetzung auf linux-laptop-mehmet

1. Mit `git pull --ff-only` den übertragenen Quellstand holen; zuvor `git status -sb` prüfen und lokale Änderungen erhalten. Die Commit-ID der Übergabe ergibt sich aus `git log -1`, nicht aus einem selbstreferenziellen Eintrag in dieser Datei.
2. JDK 17 / SDK 36 lokal einrichten; lokale `.toolchain`-/SDK-Pfade und Signaturschlüssel werden nicht mit Git übertragen. Android 0.6 / Code 3 ist unverändert; kein neues Release veröffentlicht.
3. Server nach regulärer Datensicherung aktualisieren. Er ergänzt `game_write_receipt`, bestehende Daten bleiben erhalten. Aktuelle Android-Builds brauchen `idempotent_game_writes` für Write-Operationen. Kein laufender Container wurde während dieser Umsetzung ersetzt.
4. `python -m unittest discover -s tests -v` mit installierten `requirements.txt`; `cd android && ./gradlew testDebugUnitTest lintDebug assembleDebug`. Die neuen Tests sind dauerhaft im Repository, die ursprünglichen `/tmp`-Review-Proben nicht erforderlich.
5. Smartphone/Emulator: beide Designs, Rotation mit geändertem/neuem Entwurf, Prozesswiederherstellung nach Anmeldung, erneuter Login nach 401, Dialogschließen mit unklarem Vorgang, Server-/Kontowechsel, abgebrochene HTTP-Requests, 409 und Antwortverlust prüfen. 10.000-Spiele-Testdaten nur in separater Testumgebung; Framezeiten und Heap auf schwächerem Gerät messen.
6. Saved-State ist an die vom System wiederhergestellte Aufgabe gebunden, kein dauerhafter Offline-Speicher. Für dauerhafte Aufträge über Force-Stop/Task-Löschung hinaus wäre eine gesonderte lokale Persistenz nötig. Größere Metadaten können weiterhin den Speicherbedarf von Antworten und Saved-State erhöhen; eine allgemeine Pagination wurde nicht Teil dieser Korrektur.

## Nachtrag 18.10.2026: dauerhafte Offline-Vorgänge

Die historischen Aussagen oben zum fehlenden Offline-Speicher beschreiben den Reviewstand vom 17.10.2026. Der neue Quellstand ergänzt `OfflineCollection.kt` und einen atomaren privaten Dateispeicher für Sammlung und Warteschlange. Entwürfe verwenden weiterhin Saved-State. Verhalten, Konfliktauflösung und Grenzen der Synchronisierung stehen in `android/README.md` und `PROJECT_CONTEXT.md`. Geräte-/Lifecycle-Prüfungen bleiben erforderlich.

### Veröffentlichung bestätigt – 25.10.2026

Branch `feature/popscore` und Tag `android-v0.8` erfolgreich gepusht. GitHub Release unter https://github.com/mehmet-b-90/games_collection/releases/tag/android-v0.8 veröffentlicht: Titel `Games Collection v0.8`, Label `Latest`, APK `Games-Collection-0.8-release.apk` (7,83 MB). Der auf der Release-Seite angezeigte SHA-256 `851979af53265d0812e0ae8ca27db725ab3ea68c24942b370d17f81a5d255d4f` stimmt mit der lokal gebauten und signaturgeprüften APK überein. Beide README-Downloadlinks zeigen auf dieses Asset. Release-Tag bleibt am geprüften Quellcommit; spätere Dokumentationscommits verschieben ihn nicht. Kein Play-Upload.
