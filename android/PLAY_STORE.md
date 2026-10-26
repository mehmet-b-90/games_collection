# Games Collection auf Google Play

**25.10.2026 – separate GitHub-Veröffentlichung:** Android 0.8 / Code 5 wird als direkt signierte APK unter `android-v0.8` bereitgestellt. Kein Upload an Play; die folgenden Play-Angaben zu 0.7 / Code 4 bleiben der zuletzt bestätigte Stand.

Stand: **20.10.2026**. Der interne Test ist aktiv. Eine öffentliche Produktionsveröffentlichung und ein geschlossener Test sind noch nicht bestätigt.

**Betreiberbestätigung vom 23.10.2026:** Die Android-App wurde bereits über Play Console als interner Test veröffentlicht. Dies bestätigt den dokumentierten Teststand; keine erneute Console-Prüfung, kein neuer Upload und keine Bestätigung einer öffentlichen Produktion in dieser Sitzung.

## Aktueller Stand

| Punkt | Bestätigter Stand |
| --- | --- |
| Entwicklerkonto | Persönliches Konto; Kontoverifizierung laut Betreiber abgeschlossen |
| App | Games Collection, Standardsprache Deutsch (`de-DE`), Typ App, kostenlos |
| Paket | `de.gamescollection.viewer` |
| Hochgeladener Release | `4 (0.7)`: versionName 0.7, versionCode 4, minSdk 26, targetSdk 36 |
| Interner Test | In Play Console als **Aktiv** bestätigt; neuester Release `4 (0.7)` |
| Tester | E-Mail-Liste „Tester“ ausgewählt, drei eingetragene Konten; das belegt keine drei tatsächlichen Testteilnahmen |
| Store-Anzeige | Temporärer Name `de.gamescollection.viewer (unreviewed)`; vor der ersten Prüfung möglich |
| Installation | Betreiber bestätigt erfolgreichen Download der Testversion über Google Play; das anfängliche „Nicht gefunden“ ist behoben. Funktionsprüfung noch offen |
| Nächster Schritt | Store-Icon und Screenshots ergänzen sowie Funktionen auf dem Gerät prüfen |

Der GitHub-Download unter `android-v0.7` bleibt separat bestehen. Während dieses Play-Vorgangs wurden weder GitHub-Release noch vorhandene Tags oder Assets geändert. Es gab keinen Commit oder Push. Ein aktiver interner Test bedeutet keine öffentliche Play-Store-Veröffentlichung.

## Links zur Fortsetzung

- [App-Dashboard](https://play.google.com/console/u/0/developers/9214347063282031623/app/4974864873717618364/app-dashboard)
- [Interner Test – Tester](https://play.google.com/console/u/0/developers/9214347063282031623/app/4974864873717618364/tracks/internal-testing?tab=testers)
- [Teilnahme-Link des internen Tests](https://play.google.com/apps/internaltest/4701398512557136843)
- [App-Signatur](https://play.google.com/console/u/0/developers/9214347063282031623/app/4974864873717618364/keymanagement)

Die Console-Links benötigen Berechtigungen im Entwicklerkonto; der Teilnahme-Link ist für die ausgewählten Tester bestimmt. Für den geschlossenen Test den dann in dessen Track angezeigten Link verwenden.

## Signierung und Vertriebswege

Der Betreiber hat ausdrücklich entschieden, **einen von Google verwalteten App-Signaturschlüssel** zu verwenden. Die fehlende direkte Updatekompatibilität mit bisherigen lokal signierten Release-APKs wurde akzeptiert. Nach Speichern und Neuladen in Play Console bestätigt:

- Google-App-Signatur, SHA-256: `27:D6:E9:02:C5:67:3C:C5:25:64:65:83:E9:8F:A2:53:71:4B:E5:DD:69:D2:96:00:FA:B4:AF:84:20:AD:88:2F`.
- Bisherige lokale Release-Signatur, mit `apksigner` geprüft: `A7:FD:45:C2:60:ED:64:16:77:06:F8:CB:E4:CE:EE:61:FA:84:3C:82:83:18:75:E2:81:CE:06:FD:89:E3:E3:28`.

Die Fingerabdrücke sind öffentliche Zertifikatsdaten. Der vorhandene lokale Schlüssel bleibt für die AAB-Uploadsignierung und die bisherige direkte APK-Verteilung erhalten. Google signiert die daraus für Play generierten APKs mit seinem eigenen Schlüssel. Die privaten Dateien `signing.properties` und `keystores/` bleiben ignoriert und müssen sicher gesichert und auf weitere Build-Rechner übertragen werden. Keinen neuen lokalen Schlüssel erzeugen oder vorhandene Schlüssel überschreiben.

Vor einem bewussten Wechsel von der bisherigen APK zur Play-Version sämtliche Offline-Änderungen synchronisieren und nötige Daten sichern. Deinstallation oder App-Daten-Löschen entfernt private App-Daten; während dieses Vorgangs wurde keine App installiert oder deinstalliert. Eine parallele Installation ist mit derselben App-ID nicht möglich.

**Abgebrochener Import:** Zunächst war die verschlüsselte Übernahme des bisherigen privaten Schlüssels über Googles PEPK-Werkzeug erlaubt. Die Downloads von PEPK und öffentlichem Verschlüsselungsschlüssel lieferten über die Browsersteuerung keine zugänglichen Dateien. Anschließend entschied der Betreiber für den Google-Schlüssel und beauftragte das Entfernen des leeren Ordners `android/.toolchain/play-signing/`. Dieser Ordner ist entfernt. Es wurde kein privater Schlüssel exportiert oder übertragen; der Import ist nicht fortzusetzen.

**Automatischer Installationsschutz:** In Play Console aktiviert, nicht von uns deaktiviert. Der angezeigte Hinweis kündigt eine Installationsquellenprüfung und Aufforderung zum Download über Google Play bei anderen Quellen an. Auswirkungen auf die parallele APK-Verteilung über GitHub sind vor weiteren Veröffentlichungen zu klären.

## Build, Quellstand und Prüfungen

Lokaler Build mit vorhandenem JDK 17, SDK 36 und privater Release-Signierung:

```bash
cd android
source .toolchain/env.sh
./gradlew --offline --no-daemon testReleaseUnitTest lintRelease assembleRelease bundleRelease
```

`env.sh` und Werkzeugcache sind lokale, ignorierte Einrichtungen. Für einen anderen Rechner JDK 17, Android SDK und Signierung gemäß [README](README.md) einrichten. Der erste Versuch scheiterte vor dem Build an der Sandbox-Netzwerkbeschränkung („Could not determine a usable wildcard IP“); der Wiederholungslauf mit lokalen Socket-Rechten war erfolgreich. Keine Netzwerk- oder Zertifikatsprüfung in der App wurde abgeschwächt.

- Build erfolgreich; Unit-Test- und Analyseaufgaben überwiegend aus dem aktuellen Gradle-Cache wiederverwendet, kein erzwungener vollständiger Neulauf.
- Vorhandene Release-Testberichte: **26 Tests, keine Fehler, keine übersprungenen Tests**. Release-Lint erfolgreich.
- App Bundle erstellt und signiert: `app/build/outputs/bundle/release/app-release.aab` (7.732.568 Bytes).
- Lokale APK: `app/build/outputs/apk/release/Games-Collection-0.7-release.apk` (8.052.226 Bytes).
- `jarsigner -verify` bestätigt die AAB-Signatur, meldet aber selbst signiertes Zertifikat, fehlenden Zeitstempel und Unterschiede zwischen JarFile-/JarInputStream-Lesewegen wegen Manifest-Anordnung. Google hat den Upload anschließend verarbeitet und als Artefakt angenommen.
- SDK-XML- und Gradle-Veraltungshinweise vorhanden. Shrinking bleibt bewusst deaktiviert; kein Umbau nur zum Ausblenden einer Store-Warnung.
- Keine neue Geräte-/Sicht-, Lifecycle-, Keystore- oder Live-IGDB-Prüfung. Die offenen Punkte aus [ANDROID_REVIEW.md](../ANDROID_REVIEW.md) bleiben bestehen. Backend und Container wurden für diesen Play-Vorgang nicht neu geprüft oder bereitgestellt.

Der Build enthält den lokalen Arbeitsstand einschließlich der bereits vorher vorhandenen Änderungen an der Posteranzahl (1–3 pro Reihe, Standard 2). Es ist kein reiner Build eines unveränderten Git-Commits. Vorbestehende Änderungen an `.env.example`, `PROJECT_CONTEXT.md`, `android/README.md`, `CollectionUi.kt` und `CollectionViewModel.kt` wurden erhalten. Build-Metadaten beweisen daher nicht, dass alle enthaltenen Änderungen bereits gepusht oder im GitHub-APK-Asset enthalten sind.

Version und VersionCode wurden nicht geändert. **VersionCode 4 ist in Play bereits verwendet; ein neues Bundle benötigt einen höheren Code.** VersionName, Code und ggf. GitHub-Release-Auftrag vor einem neuen installierbaren Release abstimmen. Vorhandene Tags nicht verschieben.

## Geprüfte Warnungen der Release-Vorschau

Die Vorschau zeigte drei Warnungen und keine als Fehler ausgewiesene Meldung:

| Warnung | Einordnung / weiteres Vorgehen |
| --- | --- |
| Keine Tester festgelegt | Inzwischen ist der interne Test aktiv und die Liste „Tester“ mit drei Konten ausgewählt. Diese ursprüngliche Ursache ist damit bearbeitet; nachträgliche Warnungsprüfung nicht erneut durchgeführt. |
| Keine Offenlegungsdatei | `minifyEnabled false`: keine R8-Verschleierung und keine passende Mapping-Datei. Für diesen Build kein angezeigter Veröffentlichungsblocker. |
| Keine nativen Debug-Symbole | Enthaltene Bibliothek `libandroidx.graphics.path.so` aus AndroidX graphics-path 1.0.1. Die geprüfte arm64-Bibliothek ist bereits ohne vollständige Debug-Symbole ausgeliefert. Fehlende Symbole begrenzen native Absturzdiagnosen, verhindern den internen Test aber nicht. |

## Versionshinweise des ersten internen Releases

Der Betreiber hat folgenden Text eingefügt; er wurde in der Vorschau bestätigt:

```text
<de-DE>
Erste Testversion von Games Collection.

• Spielesammlung durchsuchen, filtern und verwalten
• Spiele über IGDB suchen oder manuell hinzufügen
• Sammlung offline nutzen und Änderungen später synchronisieren
• Statistiken und zwei wählbare Designs
• Ein bis drei Poster pro Reihe

Voraussetzung: eigener kompatibler Games-Collection-Server und Benutzerkonto.

Bitte meldet Fehler und Darstellungsprobleme sowie Auffälligkeiten bei Anmeldung, Offline-Nutzung und Synchronisierung.
</de-DE>
```

## Teilnahme und „Nicht gefunden“

Google weist darauf hin, dass der Link bei einer erstmaligen Testveröffentlichung mehrere Stunden bis zur Verfügbarkeit benötigen kann. Daher nicht allein wegen der anfänglichen Meldung erneut hochladen oder einen neuen Release erstellen. Ein genauer Freischaltzeitpunkt ist nicht zugesichert.

1. Teilnahme-Link mit einem Google-Konto aus der ausgewählten Testerliste öffnen.
2. Auf der Teilnahmeseite dem Test beitreten, anschließend den angebotenen Play-Store-Link öffnen.
3. Im Play Store dasselbe Google-Konto verwenden. Ein Eintrag in der E-Mail-Liste ersetzt den Beitritt nicht.
4. Nach einigen Stunden erneut versuchen. Bleibt der Fehler bestehen, Track-/Releasestatus, ausgewählte Liste, tatsächlichen Testbeitritt, verwendetes Google-Konto und Gerätekompatibilität prüfen.

Der interne Test ist nicht über die normale öffentliche Store-Suche auffindbar. Drei Konten in der Liste sind keine Bestätigung dreier Installationen. Inzwischen hat der Betreiber den erfolgreichen Download der Testversion über Google Play bestätigt. Die anfängliche Nichtverfügbarkeit ist damit behoben; eine vollständige Geräte-/Funktionsprüfung ist dadurch nicht belegt. Es wurde keine automatische Überwachung oder Erinnerung eingerichtet.

## Store-Texte – noch nicht als übernommen bestätigt

**App-Name:** Games Collection

**Kurzbeschreibung:** Verwalte deine Spielesammlung mit deinem eigenen Games-Collection-Server.

**Vollständige Beschreibung:**

Games Collection bringt deine persönliche Videospielesammlung auf dein Android-Gerät. Verbinde die App mit deinem selbst gehosteten Games-Collection-Server und melde dich mit deinem bestehenden Konto an.

• Durchsuche und filtere deine Sammlung nach Plattform, Status, Format und weiteren Merkmalen.
• Ergänze Spiele über die IGDB-Suche oder lege sie manuell an.
• Verwalte Besitzplattformen, Spielstatus und Bewertungen.
• Entdecke Statistiken zu deiner Sammlung.
• Nutze deine zuvor geladene Sammlung auch offline. Änderungen werden lokal vorgemerkt und bei der nächsten Synchronisierung übertragen.
• Wähle zwischen Cinematic Dark und Editorial Shelf sowie ein bis drei Postern pro Reihe.

Voraussetzungen: Android 8.0 oder neuer, ein erreichbarer kompatibler Games-Collection-Server und ein dort eingerichtetes Benutzerkonto. Die App enthält keinen gehosteten Sammlungsserver. Die IGDB-Suche benötigt Internet und einen gültigen, vom eigenen Server bereitgestellten IGDB-Zugang. Kontoerstellung und Kontoverwaltung erfolgen in der Webanwendung.

## Offene Schritte bis zur öffentlichen Veröffentlichung

1. Download des internen Tests ist laut Betreiber erfolgreich. Beide Designs, Anmeldung, Serverwechsel, Offline-Warteschlange, Konflikte und Synchronisierung auf echten Geräten prüfen.
2. Länder, Zielgruppe und Store-Kategorie festlegen. App ist auf Wunsch des Betreibers kostenlos; der Wechsel zu kostenpflichtig ist nach kostenloser Veröffentlichung nicht mehr möglich.
3. Öffentliche Datenschutzerklärung mit tatsächlichem Betreiber, Kontakt und korrekten Datenflüssen bereitstellen und in der App verlinken oder anzeigen. Im Projekt bislang nicht gefunden. Selbst gehosteter Server, lokale Daten und direkte IGDB-Verbindungen berücksichtigen.
4. Datensicherheitsformular wahrheitsgemäß ausfüllen; Kontolöschungsanforderungen und Weblink prüfen. Kontoverwaltung über den Browser ist bereits vorhanden; keine pauschale Behauptung „keine Datenerhebung“ übernehmen.
5. Support-Kontakt, Store-Symbol, Feature-Grafik und geeignete Screenshots vorbereiten. Vorhandene Projekt-Screenshots nicht ungeprüft als Store-Assets übernehmen; private Daten entfernen und Store-Anforderungen prüfen.
6. Für Googles Prüfung einen erreichbaren Demo-Server, separates Testkonto und vollständige englische Zugangsanleitung bereitstellen. Keine Hauptkonto-Zugangsdaten oder Secrets in Markdown/Git ablegen.
7. Geschlossenen Test einrichten, mindestens zwölf tatsächliche Tester gewinnen und mindestens 14 Tage fortlaufende Teilnahme sicherstellen. Noch keine zwölf Tester bestätigt. Interner Test zählt dafür nicht. Beim Wechsel vom internen zum geschlossenen Test müssen Teilnehmer den internen Test zuerst verlassen und dem geschlossenen beitreten; dessen eigenen Link verwenden.
8. Nach erfülltem geschlossenen Test Produktionszugang beantragen und Googles Entscheidung abwarten. Anschließend Produktionsrelease separat vorbereiten. Kontoverifizierung allein ersetzt diese Schritte nicht.
9. Für neue Uploads höheren VersionCode abstimmen; Release-Hinweise mit Änderungen, Voraussetzungen, Prüfungen und Einschränkungen pflegen. GitHub-Release/Tag/Asset nur bei entsprechendem Auftrag aktualisieren und dann mit README konsistent halten.

## Offizielle Grundlagen

Am 20.10.2026 für diesen Vorgang nachgeschlagen; aktuelle Pflichtfelder beim Fortsetzen erneut in Play Console prüfen.

- [Interne, geschlossene und offene Tests; Verfügbarkeit und Teilnahme](https://support.google.com/googleplay/android-developer/answer/9845334?hl=de)
- [Testanforderungen für persönliche Konten](https://support.google.com/googleplay/android-developer/answer/14151465?hl=de)
- [App-Signierung und Play App Signing](https://developer.android.com/studio/publish/app-signing)
- [Google-Play-Programmrichtlinien einschließlich Nutzerdaten](https://support.google.com/googleplay/android-developer/answer/18258653?hl=de)
- [Anforderungen zur Kontolöschung](https://support.google.com/googleplay/android-developer/answer/13327111?hl=de)
- [Native Debug-Symbole](https://developer.android.com/build/include-native-symbols)

Die Annahme eines Bundles und ein aktiver interner Test sind keine vollständige Sicherheits-/Geräteprüfung und keine Produktionsfreigabe durch Google.
## Ausgewähltes Projekt-Icon (20.10.2026)

Verbindliches Motiv: weißer Controller vor drei violett/blauen Spielcovern, Variante 5. Fertiger Store-Export: `../design/brand/play-store-icon-512.png` (512 × 512, RGBA-PNG, deckend). Master und Exportworkflow: [Branding](../design/brand/README.md). Im lokalen Android-Quellstand integriert; der Upload des Store-Icons ist noch offen. Bereits veröffentlichte Bundles behalten das alte Launcher-Icon bis zu einer neuen Veröffentlichung mit höherem VersionCode.
