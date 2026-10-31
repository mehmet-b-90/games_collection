# Android-App v0.8.1

## Neue Funktion

- **Titeltypfilter bei der IGDB-Suche:** Unter dem optionalen Release-Jahr steht das Dropdown „Titeltyp“ mit Spielevollversion, DLC, Erweiterung, Bundle und allen weiteren aktuell unterstützten IGDB-Typen.
- Standard „Alle Typen“; Titel, Jahr und Typ werden gemeinsam direkt bei IGDB gesucht. Numerische IGDB-IDs berücksichtigen den gewählten Typ ebenfalls.
- Der Filter verwendet die vorhandenen Formularbausteine und beide App-Designs. Er wird nicht am Sammlungseintrag gespeichert.

## Installation und Voraussetzungen

**Android 0.8.1 / VersionCode 6**, Android 8 oder neuer. APK: **Games-Collection-0.8.1-release.apk**. Bestehender Server mit IGDB-Zugang für die Tokenbereitstellung erforderlich; für den direkten Android-Titeltypfilter ist kein zusätzliches Serverupdate nötig. Für denselben Filter im Web Server 0.8.1 verwenden.

Die APK ist mit dem bisherigen lokalen Release-Schlüssel signiert. Bestehende GitHub-Release-APKs können aktualisiert werden. Play-/Debug-Installationen verwenden andere Signaturen: vor einem bewussten Wechsel Offline-Änderungen synchronisieren und Daten sichern; keine automatische Deinstallation. Dies ist eine GitHub-APK-Veröffentlichung; Google Play bleibt unverändert bei intern 0.7 / Code 4.

## Prüfungen und bekannte Einschränkungen

- `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich. Vorhandene Berichte: **35 JVM-Tests ohne Fehler**, Lint ohne Befunde. Der Veröffentlichungslauf verwendete aktuelle Test-/Build-Ergebnisse (kein erzwungener Neulauf).
- APK-v2-Signatur gültig; Version 0.8.1 / Code 6 und bestehendes Release-Zertifikat geprüft.
- Kein neuer Smartphone-/Lifecycle-/Keystore-Test. Internet und gültiger IGDB-Zugang erforderlich. Die Typzuordnung stammt von IGDB; Sondereditionen können als Spielevollversion gelten.
- Bekannte SDK-XML-/Gradle-9-Hinweise bleiben bestehen.
