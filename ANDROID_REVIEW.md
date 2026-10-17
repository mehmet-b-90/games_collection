# Android-Codeprüfung vom 17.10.2026

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
