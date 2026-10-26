# Games Collection v0.8

## Neue Funktion

- **Entdecken → Aktuell beliebt:** eigener nativer Bereich mit bis zu 100 IGDB-Titeln, Rang, Genre und IGDB-Bewertung. Bereits vorhandene Titel werden als „In Sammlung“ markiert; die Sammlung bleibt getrennt.
- **„+ Hinzufügen“** öffnet den vorausgefüllten Spieleeditor. Persönliche Bewertung und Besitzplattformen bleiben zur eigenen Auswahl frei. Scrollpositionen bleiben beim Seitenwechsel erhalten.
- **Überarbeitete Darstellung:** neues Projekt-Icon, kompakte durchgehende Material-3-Navigation und Auswahl von 1, 2 oder 3 Postern pro Reihe. Beide Designs bleiben verfügbar.
- **Stabilere Verbindung:** GET-Leseanfragen werden bei kurzzeitigen Transportfehlern bis zu zweimal wiederholt. HTTP- und TLS-Fehler bleiben sichtbar; Schreibvorgänge behalten ihre sichere Offline-Warteschlange.
- **Einfachere lokale Tests:** Bei freigegebenem HTTP ergänzt eine Serveradresse ohne Protokoll automatisch `http://`, sonst `https://`.

## Installation und Voraussetzungen

**Android 0.8 / VersionCode 5**, Android 8 oder neuer. APK: **Games-Collection-0.8-release.apk**. Für Entdecken den Server auf 0.8 mit `GET /api/discover` aktualisieren und Twitch-/IGDB-Zugang konfigurieren. Das ist eine direkte GitHub-APK-Veröffentlichung; der Google-Play-Test bleibt unverändert bei 0.7 / Code 4.

Die bestehende lokale Release-Signatur bleibt erhalten. Updates bisheriger GitHub-Release-APKs sind damit möglich. Play-/Debug-Installationen besitzen andere Signaturen: vor einem bewussten Wechsel Offline-Änderungen synchronisieren und Daten sichern; keine automatische Deinstallation.

## Prüfungen und bekannte Einschränkungen

- Release-Prüflauf erfolgreich; vorhandene Berichte: 34 JVM-Tests ohne Fehler, frisches Release-Lint ohne Befunde. APK-v2-Signatur mit bestehendem Release-Zertifikat gültig; Version 0.8 / Code 5 bestätigt.
- Kein neuer Smartphone-/Lifecycle-/Keystore-Test. Entdecken benötigt Verbindung und hält die Rangliste nur im Arbeitsspeicher. Keine automatische Hintergrundsynchronisierung bei geschlossener App.
