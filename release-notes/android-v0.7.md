## Neu in Android 0.7

- Spiele ohne Verbindung zum Sammlungsserver manuell hinzufügen, bearbeiten und löschen. Sammlung und ausstehende Änderungen bleiben nach App-Neustarts lokal erhalten und sind an Server/Konto gebunden.
- Spätere Synchronisierung über „Synchronisieren“ bzw. Aktualisieren; außerdem beim erfolgreichen Verbinden und bei Online-Änderungen. Konflikte bleiben sichtbar, verlorene Antworten erzeugen keine doppelten Einträge.
- Direkte IGDB-Suche mit Titel, optionalem Jahr oder IGDB-ID. Einmal vom Server bezogenes Zugriffstoken wird AES-GCM-verschlüsselt mit Android Keystore gespeichert. Die tatsächliche Ablaufzeit wird angezeigt; das Client-Secret bleibt auf dem Server.
- Signierter, nicht debuggbarer Release-Build: Version 0.7, VersionCode 4.

## Installation und Voraussetzungen

Die Datei **Games-Collection-0.7-release.apk** herunterladen. Android 8 oder neuer erforderlich.

**Server zuerst aktualisieren:** Die neue IGDB-Funktion benötigt `POST /api/igdb-token` aus diesem Quellstand und konfigurierte Twitch-Zugangsdaten. Nach erfolgreicher Verbindung/Synchronisierung kann Android mit gültigem Token direkt bei IGDB suchen, auch wenn der Sammlungsserver nicht erreichbar ist. Internet bleibt nötig. Nach Tokenablauf/Widerruf wird der Server zur Erneuerung gebraucht.

**Signaturwechsel:** Frühere Debug-APKs können nicht direkt mit diesem Release überschrieben werden. Vor einem bewussten Wechsel sämtliche Offline-Änderungen synchronisieren und benötigte Daten sichern. Eine Deinstallation löscht lokale Daten. Bereits mit derselben Release-Signatur installierte Builds können aktualisiert werden.

## Prüfungen und Grenzen

- 27 Backend-Tests und 26 Android-Release-JVM-Tests bestanden; Release-Lint ohne Befunde.
- Release-APK gebaut und Signatur/Versionsmetadaten geprüft.
- Keine automatische Hintergrundsynchronisierung bei geschlossener App.
- Smartphone-/Lifecycle-/Keystore-Prüfung und Suche mit echten IGDB-Zugangsdaten stehen noch aus.
