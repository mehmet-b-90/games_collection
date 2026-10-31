# Games Collection für Android

**Android 0.8.1 / VersionCode 6 (31.10.2026).** [Release Notes](../release-notes/android-v0.8.1.md). Die GitHub-Veröffentlichung ist beauftragt; den verifizierten Abschluss dokumentiert `../PROJECT_CONTEXT.md`. Der Play-Test bleibt bei 0.7 / Code 4.


### Entdecken (seit Version 0.8)

Seit Version 0.8 bietet die App **Entdecken → Aktuell beliebt**. Dafür muss der Server ebenfalls diesen Stand mit `/api/discover` enthalten; bei einem älteren Server erscheint ein Aktualisierungshinweis. Die native Sammlung bleibt getrennt. Hinzufügen übernimmt IGDB-Metadaten in den vorhandenen Editor, während eigene Bewertung und Besitzplattformen von dir gewählt werden. Die Rangliste wird nur im Arbeitsspeicher gehalten; ein neuer Abruf benötigt die Serververbindung. Die direkte GitHub-APK ist jetzt für 0.8.1 / Code 6 vorgesehen; der Play-Test bleibt separat bei 0.7 / Code 4.

Die Android-App besitzt eine eigenständige native Oberfläche mit Kotlin, Jetpack Compose und Material 3. Es wird keine WebView verwendet. Über eine JSON-API verbindet sie sich mit deiner selbst gehosteten Games Collection und nutzt dieselben Benutzerkonten und dieselbe Datenbank wie die Webanwendung.

## Funktionen

- Native Anmeldung, Abmeldung und optional „Eingeloggt bleiben“
- Sammlung mit Cover-Raster, Titelsuche und Sortierung
- Filter nach Plattform, Status, Format, Engine, Altersfreigabe und Mindestbewertung
- Native Statistik mit Anzahl und Prozentwerten
- Spieldetails, Bearbeiten und Löschen mit Sicherheitsabfrage
- Hinzufügen über IGDB mit optionalem Release-Jahr und Titeltyp oder manuell
- Offline-Sammlung mit dauerhaft gespeicherten Änderungen und späterer Synchronisierung
- Mehrfachauswahl der Besitzplattformen sowie Status, Format und Bewertung
- Cinematic Dark und Editorial Shelf als lokale App-Designs
- Einstellungen mit lokal gespeicherter Auswahl von 1–3 Postern pro Reihe (Standard: 2)
- Externe Links zu IGDB und Deku Deals im Browser

Kontoerstellung und weitergehende Kontoverwaltung, etwa Passwortänderungen oder Benutzeradministration, erfolgen weiterhin in der Webanwendung. Nach einmaligem Online-Laden ist die Sammlung auch offline verfügbar. Spiele lassen sich manuell hinzufügen, bearbeiten und löschen; Änderungen werden lokal gespeichert und später synchronisiert.

## Server zuerst aktualisieren

Die native App benötigt die neuen JSON-Endpunkte dieser Projektversion. Lade nach der Veröffentlichung das aktuelle Server-Image und starte den Container damit neu:

```bash
docker compose pull
docker compose up -d
```

Die Datenbank im eingebundenen Verzeichnis `data/` bleibt erhalten. Der aktuelle Server ergänzt beim Start die neue Tabelle `game_write_receipt` für bestätigte Spieländerungen; bestehende Tabellen und Spiele werden nicht umgebaut oder gelöscht. Sichere bestehende Daten vor einem Serverupdate wie üblich. Neue Zugangsdaten sind nicht erforderlich.

Der aktuelle Android-Quellstand benötigt zum Speichern und Löschen die vom Server gemeldete Capability `idempotent_game_writes`. Fehlt sie, fordert die App ein Serverupdate an. Bestehende Browser- und ältere Android-Clients ohne Vorgangskennung bleiben serverseitig kompatibel.

Für lokale Serveränderungen siehe [Lokal aus dem Quellcode bauen](../README.md#lokal-aus-dem-quellcode-bauen).

## Installation und erster Start

Seit Android 0.8.1 steht unter dem Release-Jahr der IGDB-Suche das Dropdown **Titeltyp**. Standard ist **Alle Typen**; auswählbar sind Spielevollversion, DLC, Erweiterung, Bundle, eigenständige Erweiterung, Mod, Episode, Season, Remake, Remaster, erweiterte Spielversion, Portierung, Fork, Paket/Add-on und Update. Titel, Jahr und Typ werden gemeinsam bei IGDB gefiltert. Die Einordnung stammt von IGDB; beispielsweise kann auch eine Sonderedition als Spielevollversion gelten. Der Filter benötigt kein Serverupdate und speichert keinen Titeltyp am Sammlungseintrag.

Die folgenden Schritte betreffen die direkte APK-Installation. Zusätzlich läuft seit **20.10.2026** ein interner Google-Play-Test für Version **0.7 / Code 4**. Die App ist damit noch nicht öffentlich im Store. Ausgewählte Tester treten über den [Teilnahme-Link](https://play.google.com/apps/internaltest/4701398512557136843) mit ihrem eingetragenen Google-Konto bei und öffnen danach den angebotenen Store-Link. Bei der ersten Veröffentlichung kann die Verfügbarkeit mehrere Stunden dauern. Status, Fehlerdiagnose und nächste Schritte: [PLAY_STORE.md](PLAY_STORE.md).

Play- und bisherige Release-APKs haben unterschiedliche Signaturen; vor einem Wechsel den [Hinweis unten](#google-play-und-bisherige-release-apks) lesen.

1. Die [APK für Android-Version 0.8.1 herunterladen](https://github.com/mehmet-b-90/games_collection/releases/download/android-v0.8.1/Games-Collection-0.8.1-release.apk) und auf dem Smartphone öffnen. Bei einem aktuellen eigenen Build verwende die signierte Release-APK unter `app/build/outputs/apk/release/Games-Collection-0.8.1-release.apk`; zum Wechsel von der bisherigen Debug-Signatur siehe unten. Android fragt gegebenenfalls, ob die verwendete Browser- oder Datei-App APKs installieren darf.
2. Games Collection öffnen und deine Serveradresse eingeben, beispielsweise `https://games.example.org` oder für lokale Tests `http://192.168.178.20:7000`.
3. Bei HTTP die Option „HTTP für lokale Tests zulassen“ aktivieren. Ohne Protokollangabe ergänzt die App beim Verbinden automatisch `http://`, wenn die Option aktiviert ist, andernfalls `https://`. Ein ausdrücklich eingegebenes Protokoll bleibt erhalten; `http://` benötigt weiterhin die Freigabe. HTTPS ist für den regulären Betrieb empfohlen.
4. Mit deinem bestehenden Games-Collection-Konto anmelden.

Unter „Profil → Einstellungen“ lassen sich Design, Posteranzahl pro Reihe und Serveradresse ändern. Das Dropdown bietet 1, 2 oder 3 Poster pro Reihe; die Postergröße passt sich an die Bildschirmbreite an. Standard sind 2 Poster pro Reihe; darauf lässt sich die Auswahl zurücksetzen. Die bisherige Slider-Einstellung wird durch diesen Standard ersetzt. Design und Posteranzahl werden lokal auf diesem Gerät gespeichert, bleiben bei Serverwechsel erhalten und ändern nicht die Darstellung im Browser.

Beim Serverwechsel werden die bisherigen Anmelde-Cookies gelöscht; melde dich am neuen Server erneut an. Der Knopf `↻` lädt die Sammlung erneut.

Der Downloadlink verweist auf Version 0.8.1 (`android-v0.8.1`, VersionCode 6) mit Titeltypfilter und den bisherigen Entdecken-/Oberflächen-/Verbindungsfunktionen. Hinweise zum Signaturwechsel beachten. Änderungen und Voraussetzungen stehen in den GitHub Release Notes.

Das Smartphone muss den Server erreichen können, etwa im gleichen WLAN oder über ein VPN. `localhost` verweist auf dem Smartphone auf das Smartphone selbst. Im Android-Emulator ist der Hostrechner normalerweise unter `10.0.2.2` erreichbar. Die Anwendung erwartet die Serveradresse ohne Unterpfad, da die Webanwendung absolute Routen verwendet.

## Offline arbeiten und synchronisieren

Lade die Sammlung zuerst einmal angemeldet vom Server. Danach stehen die Sammlung und die Besitzplattformen auch ohne Verbindung und nach einem App-Neustart zur Verfügung. Neue Spiele lassen sich offline manuell anlegen; die direkte IGDB-Suche und noch nicht zwischengespeicherte Cover benötigen Internet, aber mit gültigem IGDB-Zugang keine Verbindung zum Sammlungsserver.

Speichern und Löschen werden zuerst dauerhaft im privaten App-Speicher abgelegt und sofort in Sammlung, Filtern und Statistik berücksichtigt. Die Anzeige „Änderungen ausstehend“ öffnet eine Übersicht einschließlich vorgemerkter Löschungen. Noch nicht gesendete Änderungen lassen sich weiter bearbeiten oder nach Rückfrage verwerfen; ein noch nicht gesendetes neues Spiel lässt sich vollständig lokal entfernen.

Sobald der Server wieder erreichbar ist, tippe auf „Synchronisieren“ beziehungsweise den Aktualisieren-Knopf oben. Beim erfolgreichen Verbinden oder Anmelden und beim Speichern mit bestehender Verbindung synchronisiert die App ebenfalls. Es gibt keinen Hintergrunddienst: Bei geschlossener App oder bloßer Netzwerkrückkehr wird keine automatische Übertragung garantiert.

Im aktuellen lokalen Quellstand werden Leseanfragen für Sitzung und Sammlung bei kurzzeitigen Netzwerkfehlern automatisch bis zu zweimal nach 0,5 und 1,5 Sekunden wiederholt. Erst nach erfolglosen Versuchen erscheint die Offline-Meldung; bestehende Zeitlimits gelten pro Versuch. Zertifikatsfehler und HTTP-Fehlerantworten werden direkt angezeigt. Schreibvorgänge behalten die oben beschriebene sichere Warteschlange. Diese Anpassung ist noch nicht Bestandteil der verlinkten GitHub-/Play-Version.

Vor jeder Übertragung prüft die App die Sitzung und das Konto. Ein abgelaufener Login erfordert eine erneute Anmeldung. Sammlung und Warteschlange sind an die genaue Serveradresse und Benutzer-ID gebunden. Abmelden und Serverwechsel blenden sie aus, löschen aber keine ausstehenden Änderungen: Nach erneuter Anmeldung am ursprünglichen Server/Konto werden sie wieder geladen. Das Löschen der App-Daten oder die Deinstallation entfernt auch nicht synchronisierte Änderungen; Android-Backups bleiben deaktiviert.

Nach einem Sendeversuch bleibt der betreffende Titel bis zur Klärung gesperrt. Die App wiederholt exakt dieselbe Anfrage mit derselben UUID; dadurch erzeugt auch eine verlorene Antwort keine doppelte Anlage. Andere Titel können weiter bearbeitet werden. Ein Vorgang wird erst entfernt, wenn die Serverantwort und das anschließende Laden der Sammlung erfolgreich waren und der neue lokale Stand gespeichert wurde.

Versionskonflikte und eindeutige Ablehnungen bleiben mit der Servermeldung in der Übersicht erhalten. Andere vorgemerkte Titel werden weiter synchronisiert. Die App überschreibt keine veralteten Bearbeitungen automatisch. Bei einem Konflikt kannst du die lokale Änderung nach Prüfung verwerfen und den aktualisierten Serverstand neu bearbeiten. Unklare Übertragungen können nicht einfach verworfen werden; synchronisiere sie zuerst erneut.

Noch nicht gespeicherte Editorentwürfe verwenden weiterhin Android-Saved-State. Ausdrückliches Abmelden oder Serverwechsel verwirft diese Entwürfe, aber nicht die dauerhaft gespeicherte Warteschlange. Aus älteren App-Ständen wiederhergestellte unklare Einzelvorgänge werden über „Vorgang erneut prüfen“ in die dauerhafte Warteschlange übernommen.

## Funktionsweise und Sicherheit

- Die App benötigt Android 8.0 oder neuer.
- Der Docker-Container bleibt der Server. Android speichert Sammlung und Warteschlange gemeinsam in einer atomar ersetzten JSON-Datei pro Server/Konto im privaten App-Verzeichnis; Zugangsdaten gehören nicht in diese Dateien.
- Twitch-Client-Secret und Flask-`SECRET_KEY` bleiben ausschließlich auf dem Server. Nur Client-ID und befristetes IGDB-Zugriffstoken werden zur Laufzeit verschlüsselt auf dem Gerät gespeichert; sie sind nicht in die APK eingebaut.
- Die API verwendet die bestehenden Flask-Sitzungs-Cookies und den vorhandenen CSRF-Schutz. Bei einer abgelaufenen Sitzung fordert die App eine erneute Anmeldung an. Ein abgelaufenes CSRF-Token wird einmal erneuert und die abgewiesene Anfrage wiederholt.
- Sitzungscookies und CSRF-Token bleiben im Arbeitsspeicher. Nur dauerhafte Cookies für „Eingeloggt bleiben“ werden verschlüsselt im privaten App-Speicher abgelegt; der Schlüssel liegt im Android Keystore. Passwörter werden nicht gespeichert. Ohne nutzbaren Keystore bleibt die Sitzung auf den laufenden App-Prozess begrenzt. Android-Backups sind deaktiviert.
- Die API überträgt Anmelde-Cookies nur an den eingestellten Server einschließlich Protokoll und Port. HTTP-Weiterleitungen werden nicht automatisch verfolgt. Gib daher direkt die endgültige Serveradresse an, etwa die HTTPS-Adresse statt ihrer HTTP-Weiterleitung. Cover werden separat über HTTPS von IGDB geladen.
- HTTPS-Zertifikatsfehler werden nicht übergangen. Verwende ein Zertifikat, dem Android vertraut.
- HTTP ist technisch für lokale Testinstallationen vorgesehen und muss im Verbindungsdialog ausdrücklich aktiviert werden. Dabei werden auch Anmeldedaten unverschlüsselt übertragen. Für HTTP muss auf dem Server `SESSION_COOKIE_SECURE=false` gelten; für HTTPS empfiehlt sich `true`.

## Selbst bauen

Benötigt werden ein JDK 17, das Android SDK mit Plattform 36 und Build-Tools 35.0.0. Am einfachsten öffnest du den Ordner `android/` in Android Studio und installierst die vom Projekt angeforderten SDK-Komponenten. Wähle JDK 17 als Gradle-JDK. Alternativ installiere die offiziellen [Android Command-Line-Tools](https://developer.android.com/studio#command-line-tools-only).

Für die reguläre Weitergabe wird die **Release-Variante** verwendet. Debug bleibt für Entwicklung und Fehlersuche verfügbar. Release ist ausdrücklich nicht debuggable; Code- und Ressourcenverkleinerung bleiben zunächst deaktiviert, damit keine zusätzlichen R8-/Reflection-Änderungen mit der Umstellung vermischt werden.

### Signierung einmalig einrichten

Mit JDK 17 auf dem Pfad bzw. in `JAVA_HOME`:

```bash
cd android
python3 create-release-key.py
```

Nur bei der **erstmaligen** Einrichtung ausführen. Das Skript erzeugt einen separaten RSA-3072-/PKCS12-Schlüssel mit 30 Jahren Laufzeit und zufälligem Passwort. Es überschreibt keine bestehende Signierung. Private Dateien:

- `android/keystores/games-collection-release.p12`
- `android/signing.properties`

Beide Dateien bleiben lokal und sind von Git ausgeschlossen. Sichere sie dauerhaft in einer geschützten Sicherung, bevor du APKs mit diesem Schlüssel verteilst. Für Updates und Builds auf einem anderen Rechner **dieselben Dateien sicher übertragen**, dort keinen neuen Schlüssel erzeugen. Ohne den privaten Schlüssel sind später keine kompatiblen Updates möglich.

Ein bereits vorhandener eigener Schlüssel kann stattdessen über eine lokale `signing.properties` eingebunden werden; Vorlage: `signing.properties.example`. `storeFile` ist relativ zum Ordner `android/` oder ein absoluter lokaler Pfad. Echte Passwörter gehören ausschließlich in die ignorierte Datei. Die Release-Paketierung bricht bei fehlender Signierung verständlich ab; es gibt keinen automatischen Debug-Schlüssel-Fallback und keine still erzeugte unsignierte Release-APK.

### Release bauen

Mit vorhandenem SDK und akzeptierten SDK-Lizenzen:

```bash
cd android
export ANDROID_HOME=/pfad/zum/Android/Sdk
export JAVA_HOME=/pfad/zum/jdk-17
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Alternativ kann der SDK-Pfad in der lokal ignorierten Datei `android/local.properties` als `sdk.dir=/pfad/zum/Android/Sdk` eingetragen werden. Der Gradle-Wrapper und Abhängigkeiten werden bei Bedarf heruntergeladen. Lokales `.toolchain/env.sh` nur verwenden, wenn die dazugehörigen Werkzeuge vorhanden sind.

Ergebnis ausgehend vom Repository-Hauptverzeichnis: `android/app/build/outputs/apk/release/Games-Collection-0.8.1-release.apk`. Der Dateiname folgt automatisch der `versionName`. Aktuell: **Version 0.8.1 / Code 6**. Neue Veröffentlichungen erhalten einen höheren VersionCode und eigene GitHub Release Notes.

### App Bundle für Google Play bauen

Mit derselben lokalen Release-Signierung und JDK-/SDK-Konfiguration:

```bash
./gradlew testReleaseUnitTest lintRelease assembleRelease bundleRelease
```

Ausführen im Ordner `android/`. Das Bundle liegt ausgehend vom Repository-Hauptverzeichnis unter `android/app/build/outputs/bundle/release/app-release.aab`; der Dateiname enthält keine Versionsnummer. Die Metadaten vor einem Upload prüfen. VersionCode **4 ist bereits in Play verwendet**; für einen neuen Upload zuerst einen höheren Code mit dem Betreiber abstimmen. Der vorhandene Build wurde am 20.10.2026 hochgeladen und als interner Test eingeführt. Neue GitHub-Assets, Tags oder Releases entstehen dadurch nicht.

Der lokale Keystore signiert den Upload. Die an Nutzer ausgelieferten Play-APKs signiert Google mit dem eingerichteten Google-App-Signaturschlüssel. Lokalen Keystore und `signing.properties` weiterhin sicher sichern und auf andere Build-Rechner übertragen, nicht neu erzeugen. Weitere Angaben und offene Store-Aufgaben stehen in [PLAY_STORE.md](PLAY_STORE.md).

### Google Play und bisherige Release-APKs

Am 20.10.2026 hat der Betreiber die Verwendung eines Google-App-Signaturschlüssels gewählt und die fehlende direkte Updatekompatibilität zu bisherigen Release-APKs akzeptiert. Der Import des lokalen privaten Schlüssels wurde abgebrochen; es wurde kein privater Schlüssel an Google übertragen. Lokale/GitHub-APKs behalten ihre bisherige Signatur. Beide Vertriebswege verwenden dieselbe App-ID und lassen sich daher nicht parallel installieren.

Vor einem bewussten Wechsel zur Play-Version sämtliche ausstehenden Offline-Änderungen synchronisieren und benötigte Daten sichern. Eine Deinstallation löscht lokale Sammlung, Warteschlange und Zugangsdaten; keine automatische Deinstallation vornehmen. Öffentliche Zertifikat-Fingerabdrücke und der aktuelle Teststand stehen in [PLAY_STORE.md](PLAY_STORE.md).

### Wechsel von den bisherigen APKs

Die bisherigen Debug-APKs und der am 18.10.2026 neu angelegte Release-Schlüssel haben unterschiedliche Signaturen. Android akzeptiert deshalb kein direktes Update darüber. Es wurde keine vorhandene App deinstalliert oder überschrieben. Vor einem bewussten Wechsel **alle lokalen Offline-Änderungen synchronisieren** und nötige Daten sichern; eine Deinstallation löscht private App-Daten einschließlich nicht synchronisierter Änderungen. Auch die lokalen Cookie-/Keystore-Daten gehen dabei verloren. Danach lässt sich die Release-APK neu installieren.

Künftige Updates mit demselben Release-Schlüssel und passender Versionsnummer können mit angeschlossenem Gerät installiert werden:

```bash
cd android
adb install -r app/build/outputs/apk/release/Games-Collection-0.8.1-release.apk
```

Für reine Entwicklungsprüfungen bleibt `./gradlew testDebugUnitTest lintDebug assembleDebug` verfügbar; dessen Ergebnis heißt weiterhin `app/build/outputs/apk/debug/app-debug.apk`. Debug und Release haben dieselbe App-ID und können deshalb nicht als zwei getrennte Apps parallel installiert werden.

Grundlage: [Android-Dokumentation zur App-Signierung](https://developer.android.com/studio/publish/app-signing). Ein lokaler Release-Build ist noch keine Veröffentlichung bei GitHub oder Google Play.

## Prüfungen

`testDebugUnitTest` prüft Serveradressen, Filter, Versionsnummern beim Speichern, die Begrenzung der Cookies auf den gewählten Server, Cookie-Persistenz, CSRF-Erneuerung, Weiterleitungen und Konflikte. Hinzu kommen Entwurfs-/Vorgangswiederherstellung und Kontotrennung, IO-Initialisierung, tatsächliches HTTP-Cancel bei verzögerten Headern und Body, stabile Vorgangskennungen nach Antwortverlust und CSRF-Erneuerung sowie Berechnungen für 10.000 Spiele. Zusätzliche Offline-Tests prüfen dauerhafte Serialisierung, stabile UUIDs und Versionen, lokale IDs, Bearbeiten/Löschen ungesendeter Neuanlagen, Löschprojektion und das Sperren bereits versuchter Anfragen. Dies sind JVM-Tests; eine echte Activity-/Prozess-Neuerstellung und Android Keystore sind damit nicht instrumentiert getestet. `testReleaseUnitTest` führt diese JVM-Tests gegen die Release-Variante aus. `lintRelease` analysiert die Release-Variante; `assembleRelease` kompiliert und signiert die APK mit der privaten Release-Konfiguration. Für Debug gelten entsprechend `lintDebug` und `assembleDebug`.

Serverseitig prüfen `python -m unittest discover -s tests -v` unter anderem JSON-Anmeldung und Abmeldung mit aktivem CSRF-Schutz, die Trennung der Sammlungen und die bestehenden Validierungsregeln.

## JSON-Schnittstelle

- `GET /api/session`: API-Version, Sitzungsstatus, öffentliche Benutzerdaten und CSRF-Token; auch vor der Anmeldung erreichbar
- `POST /api/session`: Anmeldung mit JSON-Feldern `username`, `password`, optional `remember` (Boolean)
- `DELETE /api/session`: Abmeldung
- `GET /api/games`: eigene Spiele einschließlich Versionsnummern und verfügbare Besitzplattformen
- `POST /api/search`, `POST /api/games`, `PUT /api/games/<id>`, `DELETE /api/games/<id>`: bestehende Spielesuche und Verwaltung

Ändernde Anfragen senden das CSRF-Token als `X-CSRFToken`, das zugehörige Sitzungs-Cookie und die konfigurierte Serveradresse als `Referer`. Damit bleibt auch die strenge HTTPS-Referrer-Prüfung von Flask-WTF aktiv. Nicht angemeldete Zugriffe auf geschützte API-Routen liefern JSON mit HTTP 401. Sitzung und Sammlung werden mit `Cache-Control: no-store` ausgeliefert. Es gibt keine CSRF-Ausnahme für die native App.

Vor einer Veröffentlichung sollten auf einem echten Smartphone Anmeldung und Abmeldung, „Eingeloggt bleiben“, beide Designs, Filter, Spielbearbeitung, externe Links, Serverwechsel und Verbindungsfehler geprüft werden.

Spieländerungen aus aktuellen Builds senden zusätzlich `Idempotency-Key` als kanonische UUID. Der Server speichert einen benutzergebundenen Beleg mit Request-Hash und Erfolgsantwort in derselben Transaktion wie die Änderung. Dieselbe Kennung mit anderer Methode, Route oder anderem Payload liefert HTTP 409. Die API-Version bleibt 1; `/api/session` meldet die additive Capability. Belege bleiben bis zur Kontolöschung erhalten und gehören in die reguläre Datenbanksicherung. Ohne definiertes Wiederholungsfenster dürfen sie nicht pauschal bereinigt werden.

## Direkte IGDB-Suche mit verschlüsseltem Zugang (19.10.2026)

Zuerst den Server auf diesen Quellstand aktualisieren und die App einmal erfolgreich verbinden/synchronisieren. Der neue, angemeldete und CSRF-geschützte Endpunkt `POST /api/igdb-token` liefert Client-ID, App-Access-Token und die tatsächliche Restlaufzeit. Der Server benötigt weiterhin seine Twitch-Konfiguration. Das Client-Secret bleibt ausschließlich auf dem Server; die Antwort wird mit `Cache-Control: no-store` ausgeliefert. HTTPS verwenden; ausdrücklich erlaubter lokaler HTTP-Testbetrieb verschlüsselt die Übertragung nicht.

Android speichert diese Zugangsdaten zusammen mit Server/Konto und Ablaufzeit AES-GCM-verschlüsselt im privaten App-Speicher. Der Schlüssel liegt im Android Keystore. Ohne erfolgreiche verschlüsselte Speicherung wird kein neuer Zugang übernommen; es gibt keinen Klartext-Fallback. Token landen weder im Sammlungscache noch im Saved-State. Abmelden und Server-/Kontowechsel entfernen den lokalen IGDB-Zugang. App-Backups bleiben deaktiviert.

„Spiel hinzufügen“ sucht anschließend direkt über HTTPS bei IGDB, ohne Sammlungs-Cookies oder CSRF-Token dorthin zu senden. Titel, optionales Jahr, numerische IGDB-ID und USK-vor-PEGI bleiben erhalten. Gefundene Spiele können in die lokale Warteschlange aufgenommen werden. Das Suchfenster zeigt die tatsächliche Ablaufzeit an; eine feste Anzahl Tage wird nicht angenommen.

Bei erfolgreicher Synchronisierung beschafft die App den Zugang, wenn er fehlt oder weniger als einen Tag Restlaufzeit hat. Der Server kann dabei denselben noch gültigen Token liefern; er ersetzt abgelaufene bzw. bei der Ausgabe als widerrufen erkannte Tokens. Bei einem IGDB-401 verwirft Android den Token, ohne den Sammlungs-Login abzumelden. Erneut suchen oder synchronisieren, sobald der Server erreichbar ist. Ohne gültigen Token und ohne Serververbindung bleibt manuelles Anlegen möglich.

Für die neue Funktion müssen Server und Android aktualisiert werden. Kein separater Cloud-Dienst erforderlich. Die lokale APK ist kein GitHub-Release; es erfolgte kein Deployment des laufenden Servers.
