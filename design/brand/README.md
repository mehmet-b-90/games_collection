# Verbindliches Games-Collection-Icon

Am 20.10.2026 hat der Betreiber Variante 5 (Controller vor drei violett/blauen Spielcovern) für Android, Google Play, GitHub und weitere Projektbereiche ausgewählt. Das Motiv unverändert weiterverwenden. Es wurde mit dem integrierten Imagegen-Tool erzeugt; ursprüngliche Prompts stehen in `../icon-concepts/2026-10-20/CONTROLLER-VARIANTEN.md`.

- `games-collection-master.png`: unveränderter ausgewählter Master, 1254 × 1254 Pixel.
- `play-store-icon-512.png`: 512 × 512 Pixel, RGBA-PNG, deckend, ohne vorgerundete äußere Ecken; bereit für den Store-Eintrag.
- `github-icon-512.png`: 512 × 512 Pixel mit Sicherheitsrand für runde Darstellung; auch im Repository-README eingebunden.
- Android: adaptives Launcher-Icon mit dunkelblauem Hintergrund und gepolsterter Bildebene. minSdk 26 erlaubt die direkte Verwendung ohne Legacy-Mipmaps. Für Android-Themensymbole liegt eine vereinfachte monochrome Vektorsilhouette desselben Motivs vor. Anmelde- und Serveransicht verwenden das farbige Original.
- Web: Favicons, Apple-Touch-Icon, PWA-Icons, Offline-Seite und sichtbares Projektlogo verwenden das neue Motiv. Die Service-Worker-Cache-Version wurde angehoben.

Alle Ableitungen werden rein technisch skaliert und nötigenfalls mit einem Hintergrundrand versehen. Keine Neugenerierung oder Änderung des Motivs.

## Erneut exportieren

Im Repository mit Python und installiertem Pillow:

```bash
python tools/export_icons.py
```

Vorgaben: [Google Play](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [adaptive Android-Icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

Die lokale Integration ist keine Veröffentlichung. Store-Upload und eine gegebenenfalls gewünschte Änderung eines GitHub-Profil-/Organisationsbilds sind separat auszuführen. Vorhandene veröffentlichte APKs und AABs behalten ihr bisheriges Icon bis zu einer neuen Veröffentlichung. Versionsnummern und Tags wurden für diesen Icon-Auftrag nicht verändert.

## Verifikation am 20.10.2026

- Android-Release-Unit-Tests (26), Lint und signierter APK-Build erfolgreich.
- Master bytegleich zum gewählten Entwurf; PNG-Maße, deckender RGBA-Export und XML-Ressourcen geprüft. Store-Datei unter 1 MB.
- Runde Maskenvorschauen für Android, PWA und GitHub visuell geprüft. Echte Geräte-/Launcher- und Browserprüfung in beiden Designs noch offen.
- Backend-Tests konnten mit dem aktuellen Python nicht starten (`ModuleNotFoundError: flask`); keine Änderung an Backend-Logik.
