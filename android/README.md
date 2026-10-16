# Games Collection für Android

Die Android-App besitzt eine eigenständige native Oberfläche mit Kotlin, Jetpack Compose und Material 3. Es wird keine WebView verwendet. Über eine JSON-API verbindet sie sich mit deiner selbst gehosteten Games Collection und nutzt dieselben Benutzerkonten und dieselbe Datenbank wie die Webanwendung.

## Funktionen

- Native Anmeldung, Abmeldung und optional „Eingeloggt bleiben“
- Sammlung mit Cover-Raster, Titelsuche und Sortierung
- Filter nach Plattform, Status, Format, Engine, Altersfreigabe und Mindestbewertung
- Native Statistik mit Anzahl und Prozentwerten
- Spieldetails, Bearbeiten und Löschen mit Sicherheitsabfrage
- Hinzufügen über IGDB mit optionalem Release-Jahr oder manuell
- Mehrfachauswahl der Besitzplattformen sowie Status, Format und Bewertung
- Cinematic Dark und Editorial Shelf als lokale App-Designs
- Einstellungen mit lokal gespeicherter Covergröße und Zurücksetzen auf die Standardgröße
- Externe Links zu IGDB und Deku Deals im Browser

Kontoerstellung und weitergehende Kontoverwaltung, etwa Passwortänderungen oder Benutzeradministration, erfolgen weiterhin in der Webanwendung. Die Android-App ist online ausgerichtet; eine Offline-Synchronisation ist nicht implementiert.

## Server zuerst aktualisieren

Die native App benötigt die neuen JSON-Endpunkte dieser Projektversion. Baue den Docker-Container nach Übernahme der Änderungen neu:

```bash
docker compose up -d --build
```

Die Datenbank im eingebundenen Verzeichnis `data/` bleibt erhalten. Die neuen API-Routen benötigen keine Schemaänderung oder neue Zugangsdaten.

## Installation und erster Start

1. Die [APK für Android-Version 0.6 herunterladen](https://github.com/mehmet-b-90/games_collection/releases/download/android-v0.6/games-collection-android-0.6.apk) und auf dem Smartphone öffnen. Bei einem eigenen Build verwende `app/build/outputs/apk/debug/app-debug.apk`. Android fragt gegebenenfalls, ob die verwendete Browser- oder Datei-App APKs installieren darf.
2. Games Collection öffnen und deine Serveradresse eingeben, beispielsweise `https://games.example.org` oder für lokale Tests `http://192.168.178.20:7000`.
3. Bei HTTP die Option „HTTP für lokale Tests zulassen“ aktivieren. HTTPS ist für den regulären Betrieb empfohlen.
4. Mit deinem bestehenden Games-Collection-Konto anmelden.

Unter „Profil → Einstellungen“ lassen sich Design, Covergröße und Serveradresse ändern. Kleinere Cover ermöglichen mehr Titel nebeneinander; die Spaltenanzahl passt sich automatisch an die Bildschirmbreite an. Die Covergröße kann auf den Standard zurückgesetzt werden. Design und Covergröße werden lokal auf diesem Gerät gespeichert und ändern nicht die Darstellung im Browser.

Beim Serverwechsel werden die bisherigen Anmelde-Cookies gelöscht; melde dich am neuen Server erneut an. Der Knopf `↻` lädt die Sammlung erneut.

Der Downloadlink oben verweist auf die bestehende Release-APK `android-v0.6`. Neuere Quellcodeänderungen sind darin erst nach einem gesonderten APK-Release enthalten; ein Git-Push allein aktualisiert die APK nicht.

Das Smartphone muss den Server erreichen können, etwa im gleichen WLAN oder über ein VPN. `localhost` verweist auf dem Smartphone auf das Smartphone selbst. Im Android-Emulator ist der Hostrechner normalerweise unter `10.0.2.2` erreichbar. Die Anwendung erwartet die Serveradresse ohne Unterpfad, da die Webanwendung absolute Routen verwendet.

## Funktionsweise und Sicherheit

- Die App benötigt Android 8.0 oder neuer.
- Der Docker-Container bleibt der Server. Die Android-App enthält keine eigene Datenbank und benötigt eine Netzwerkverbindung; eine eigenständige Offline-Synchronisation ist nicht implementiert.
- Es werden keine IGDB-Zugangsdaten und kein Flask-`SECRET_KEY` in der App hinterlegt. Diese bleiben ausschließlich auf dem Server.
- Die API verwendet die bestehenden Flask-Sitzungs-Cookies und den vorhandenen CSRF-Schutz. Bei einer abgelaufenen Sitzung fordert die App eine erneute Anmeldung an. Ein abgelaufenes CSRF-Token wird einmal erneuert und die abgewiesene Anfrage wiederholt.
- Sitzungscookies und CSRF-Token bleiben im Arbeitsspeicher. Nur dauerhafte Cookies für „Eingeloggt bleiben“ werden verschlüsselt im privaten App-Speicher abgelegt; der Schlüssel liegt im Android Keystore. Passwörter werden nicht gespeichert. Ohne nutzbaren Keystore bleibt die Sitzung auf den laufenden App-Prozess begrenzt. Android-Backups sind deaktiviert.
- Die API überträgt Anmelde-Cookies nur an den eingestellten Server einschließlich Protokoll und Port. HTTP-Weiterleitungen werden nicht automatisch verfolgt. Gib daher direkt die endgültige Serveradresse an, etwa die HTTPS-Adresse statt ihrer HTTP-Weiterleitung. Cover werden separat über HTTPS von IGDB geladen.
- HTTPS-Zertifikatsfehler werden nicht übergangen. Verwende ein Zertifikat, dem Android vertraut.
- HTTP ist technisch für lokale Testinstallationen vorgesehen und muss im Verbindungsdialog ausdrücklich aktiviert werden. Dabei werden auch Anmeldedaten unverschlüsselt übertragen. Für HTTP muss auf dem Server `SESSION_COOKIE_SECURE=false` gelten; für HTTPS empfiehlt sich `true`.

## Selbst bauen

Benötigt werden ein JDK 17, das Android SDK mit Plattform 36 und Build-Tools 35.0.0. Am einfachsten öffnest du den Ordner `android/` in Android Studio und installierst die vom Projekt angeforderten SDK-Komponenten. Wähle JDK 17 als Gradle-JDK. Alternativ installiere die offiziellen [Android Command-Line-Tools](https://developer.android.com/studio#command-line-tools-only).

Mit vorhandenem SDK und akzeptierten SDK-Lizenzen:

```bash
cd android
export ANDROID_HOME=/pfad/zum/Android/Sdk
export JAVA_HOME=/pfad/zum/jdk-17
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Alternativ kann der SDK-Pfad in der lokal ignorierten Datei `android/local.properties` als `sdk.dir=/pfad/zum/Android/Sdk` eingetragen werden. Der Gradle-Wrapper lädt die festgelegte Gradle-Version beim ersten Aufruf herunter; weitere Abhängigkeiten werden ebenfalls beim ersten Build heruntergeladen.

Ergebnis: `android/app/build/outputs/apk/debug/app-debug.apk` (ausgehend vom Repository-Hauptverzeichnis).

Mit angeschlossenem Gerät und aktiviertem USB-Debugging:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Die Debug-APK ist automatisch für lokale Tests signiert. Für eine langfristige Verteilung oder Play-Store-Veröffentlichung muss eine Release-Version mit einem eigenen, dauerhaft aufbewahrten Signaturschlüssel erstellt werden, beispielsweise über Android Studio → Build → Generate Signed App Bundle / APK. Nur mit demselben Schlüssel signierte Versionen können eine bestehende Installation ohne Neuinstallation aktualisieren. Private Signaturschlüssel gehören nicht ins Repository.

## Prüfungen

`testDebugUnitTest` prüft Serveradressen, Filter, Versionsnummern beim Speichern, die Begrenzung der Cookies auf den gewählten Server, Cookie-Persistenz, CSRF-Erneuerung und die Behandlung von Weiterleitungen und Konflikten. `lintDebug` analysiert Android-spezifische Fehler; `assembleDebug` kompiliert und signiert die APK.

Serverseitig prüfen `python -m unittest discover -s tests -v` unter anderem JSON-Anmeldung und Abmeldung mit aktivem CSRF-Schutz, die Trennung der Sammlungen und die bestehenden Validierungsregeln.

## JSON-Schnittstelle

- `GET /api/session`: API-Version, Sitzungsstatus, öffentliche Benutzerdaten und CSRF-Token; auch vor der Anmeldung erreichbar
- `POST /api/session`: Anmeldung mit JSON-Feldern `username`, `password`, optional `remember` (Boolean)
- `DELETE /api/session`: Abmeldung
- `GET /api/games`: eigene Spiele einschließlich Versionsnummern und verfügbare Besitzplattformen
- `POST /api/search`, `POST /api/games`, `PUT /api/games/<id>`, `DELETE /api/games/<id>`: bestehende Spielesuche und Verwaltung

Ändernde Anfragen senden das CSRF-Token als `X-CSRFToken`, das zugehörige Sitzungs-Cookie und die konfigurierte Serveradresse als `Referer`. Damit bleibt auch die strenge HTTPS-Referrer-Prüfung von Flask-WTF aktiv. Nicht angemeldete Zugriffe auf geschützte API-Routen liefern JSON mit HTTP 401. Sitzung und Sammlung werden mit `Cache-Control: no-store` ausgeliefert. Es gibt keine CSRF-Ausnahme für die native App.

Vor einer Veröffentlichung sollten auf einem echten Smartphone Anmeldung und Abmeldung, „Eingeloggt bleiben“, beide Designs, Filter, Spielbearbeitung, externe Links, Serverwechsel und Verbindungsfehler geprüft werden.
