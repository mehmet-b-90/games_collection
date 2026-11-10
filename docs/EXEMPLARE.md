# Einzelne Exemplare pro Spiel

Lokaler Quellstand vom 10.11.2026, noch keine Veröffentlichung dieser Erweiterung. Server/Web bleiben 0.10, Android 0.10 / Code 9. Eine vorhandene veröffentlichte Version mit derselben Nummer enthält diese lokalen Änderungen nicht automatisch.

## Bedienung

Ein Spieleintrag enthält eine Liste „Mein Besitz“. Jedes Exemplar hat eine Besitzplattform, das Format Digital oder Physisch und eine optionale Ausgabe (höchstens 80 Zeichen). Mehrere Exemplare mit derselben Plattform und demselben Format sind erlaubt, auch mit identischen Ausgaben. Pro Spiel sind bis zu 100 Exemplare möglich. Das letzte Exemplar darf entfernt werden; der Titel mit seinen Metadaten bleibt in der Sammlung.

- Android: Spieldetails → Mein Besitz → Exemplar hinzufügen bzw. Stift. In der Detailansicht speichert „Exemplar speichern“ über die bestehende dauerhafte Offline-Warteschlange. Im laufenden Spieleditor übernimmt „Übernehmen“ das Exemplar zunächst in den Entwurf; anschließend das Spiel speichern. Entfernen fragt nach Bestätigung. Zurück/Abbrechen verwirft nur die Eingaben des Exemplardialogs. Wisch-Navigation bleibt während der Exemplarbearbeitung gesperrt.
- Web: Spiel öffnen → Mein Besitz. Exemplare hinzufügen, bearbeiten oder nach Bestätigung entfernen; „Übernehmen“ übernimmt das Exemplar in den Spielentwurf. Erst „Speichern“ am Spiel sichert alle Änderungen gemeinsam. Ein offener Exemplardialog muss vorher übernommen oder abgebrochen werden. Fehlgeschlagenes Speichern lässt den Editor geöffnet.
- Titel, Cover, persönliche Bewertung und Spielstatus gelten weiterhin gemeinsam für das Spiel. IGDB-verfügbare Plattformen bleiben von Besitzplattformen getrennt. Bestätigte Freunde sehen die Exemplare nur lesend.
- Eine Karte pro Titel; die Anzahl der Exemplare steht bei den Besitzangaben. In „Mein Besitz“ steht links neben jedem Eintrag ein Download-Symbol für Digital oder ein Disc-Symbol für Physisch (Screenshotfreigabe vom 10.11.2026). Beide Clients verwenden kompakte Kontursymbole in der jeweiligen Theme-Farbe; die Formatnamen bleiben zusätzlich lesbar. Plattform- und Formatfilter müssen dasselbe Exemplar treffen. „PS4 + Physisch“ trifft nicht allein deshalb zu, weil ein Titel digital auf PS4 und physisch auf einer anderen Plattform vorhanden ist.
- Kennzahlen „Spiele“ und „Ø Bewertung“ zählen Titel. Die Plattformkennzahl zählt unterschiedliche Besitzplattformen. Die Plattform- und Formatdiagramme zählen Exemplare und sind entsprechend beschriftet.
- Bestehende kontoweite Standardwerte bleiben bestehen: Für neue Spiele entsteht je gewählter Standardplattform ein Exemplar mit dem Standardformat und leerer Ausgabe. Bestehende Spiele und Entwürfe werden dadurch nicht verändert.

## Datenmigration

`files/migrations.py` wird beim Serverstart vor dem Annehmen von Anfragen ausgeführt. Eine explizite SQLite-Transaktion mit `BEGIN IMMEDIATE` serialisiert Schemaanlage und Datenübernahme auch bei parallelen Starts. Sie ergänzt `game_copy` und `schema_migration`; vorhandene Tabellen werden weder ersetzt noch gelöscht. Auch die Anlage noch fehlender älterer additiver Tabellen erfolgt in dieser Transaktion.

Für jede vorhandene Zeile in `game_platform_ownership` wird genau ein Exemplar angelegt: gleiche Spiel-ID und Plattform, Format aus dem bisherigen Spieleintrag, Ausgabe leer. Ohne bisherige Besitzzuordnung wird kein Exemplar erfunden. IDs, Konten, Spielversionen, Metadaten, Bewertungen, Status, Cover, Freundschaften, Vorgangsbelege und Einstellungen bleiben beim Upgrade unverändert. Zwei bisher nicht unterscheidbare Exemplare werden nicht erraten; zusätzliche digitale/physische Exemplare ergänzt der Nutzer anschließend.

Der Beleg `2026_11_game_copies` wird zusammen mit den Exemplaren committet. Ein erneuter Start übernimmt nichts doppelt und stellt auch bewusst gelöschte Exemplare nicht wieder her. Fehler rollen die Transaktion inklusive neuer Tabellen zurück und verhindern den erfolgreichen Anwendungsstart. Bereits vorhandene Exemplare ohne Migrationsbeleg werden nicht still erneut übernommen; der Start verlangt eine Prüfung des Datenbestands.

`game_copy` hat einen indexierten Spiel-Fremdschlüssel mit Löschkaskade und Constraints für Format/Ausgabenlänge. Plattformen, Typen, erlaubte Felder und Listenlänge werden zusätzlich serverseitig validiert. Änderungen ersetzen die Exemplarliste innerhalb derselben versionierten Spieltransaktion. Ein Versionskonflikt liefert HTTP 409, ein Schreibfehler Rollback. Android-Vorgangsbelege und Mutation bleiben atomar.

## Update für die beiden aktuellen Nutzer

Der Betreiber hat am 10.11.2026 bestätigt: Derzeit nutzen er und sein Schwager die Anwendung; beide wollen auf den neuen Client wechseln. Ein dauerhafter Mischbetrieb ist nicht vorgesehen.

1. Beide Nutzer synchronisieren ihre ausstehenden Android-Änderungen mit dem bisherigen Server. Abgelehnte oder unklare Vorgänge vorher prüfen; nicht durch Neuinstallation verlieren.
2. Konsistentes Datenbank-Backup über Serververwaltung erstellen und außerhalb des Servers sichern. Zusätzlich Serverkonfiguration und private Android-Signierung wie bisher sichern. [Backup und Wiederherstellung](SERVER_BETRIEB.md#datenbank-sichern-und-wiederherstellen).
3. Update zunächst mit einer Kopie des Backups und dem neuen Serverstand prüfen. Danach alle alten Serverinstanzen, die auf die produktive Datenbank schreiben, stoppen. Den neuen Server mit demselben Datenverzeichnis und derselben Konfiguration starten; nicht parallel mit alten Serverprozessen betreiben. Keine leere Datenbank anlegen.
4. Startlogs und `/health` prüfen; anschließend Anmeldung, Anzahl der Spiele und einige Exemplare in beiden Konten kontrollieren. Die Migration erfolgt beim Start automatisch, ohne gesonderten SQL-Befehl.
5. Beide Android-Apps aktualisieren und offene Webseiten neu laden. Neue Exemplarschreibvorgänge benötigen die Server-Capability `game_copies`. Der aktuelle PWA-Cache ist v14.

Es wurde kein Produktivupdate ausgeführt. Für lokale Builds siehe [Serverbetrieb](SERVER_BETRIEB.md#lokal-aus-dem-quellcode-bauen). Ein späterer Veröffentlichungsauftrag muss Versionen und Android-VersionCode separat festlegen.

## Kleine Absicherung für den Übergang

Die bisherigen Besitzfelder bleiben als abgeleitete Darstellung erhalten, sind aber keine separaten Eingabefelder mehr. Die Exemplarliste ist maßgeblich. Alte Clients können daraus nur Plattformen und ein gemeinsames Format anzeigen; sie zeigen komplexen Besitz nicht vollständig an.

Alte Schreibanfragen mit Besitzfeldern werden nur übernommen, wenn der gesamte Besitz noch verlustfrei im alten Schema darstellbar ist: genau ein Exemplar je Plattform, ein gemeinsames Format, keine benannte Ausgabe. Andernfalls antwortet der Server mit HTTP 409 und einem Updatehinweis. Reine Statusänderungen ohne Besitzfelder bleiben möglich. Neue Anfragen senden `copies` statt `format`/`owned_platform`; gemischte Schreibformate werden abgewiesen.

Eine bereits versuchte Offline-Anfrage behält auch nach einem App-Update ihren alten Payload und ihre UUID. So kann ein bereits serverseitig gespeicherter Vorgang nach verlorener Antwort anhand seines Belegs bestätigt werden. Die App konvertiert solche Vorgänge nicht still in eine neue Anfrage. Normale neue Bearbeitungen verwenden Exemplare. Ein alter Server erhält keine neue Exemplaranfrage; ein Updatehinweis erscheint, und lokale Änderungen bleiben erhalten.

Diese Absicherung benötigt keine zusätzliche Bedienung. Sie dient möglichen offenen Vorgängen und noch offenen Webseiten während des gemeinsamen Updates, nicht der dauerhaften Pflege alter Apps.

Keinen alten Server schreibend auf die bereits umgestellte Datenbank zurücksetzen: Er würde die neuen Exemplare nicht mitpflegen. Ein Rollback benötigt einen abgestimmten Wiederherstellungsweg mit dem Backup vor dem Upgrade; neuere Änderungen müssten zuvor gesichert und gesondert übernommen werden. Die alten Spalten allein sind kein verlustfreier Downgradeweg.

## Verifikation am 10.11.2026

- 63 Backendtests bestanden, ausschließlich mit temporären Testdatenbanken im lokalen Container. Neue Prüfungen: Übernahme mit mehreren Konten/Plattformen, vollständiger Vergleich bestehender Tabellen einschließlich Cover, Freundschaften, Einstellungen und Vorgangsbelege; Wiederholung, parallele Starts, Fehler-Rollback, Constraints/Validierung, Versionskonflikte, Kontentrennung, Kaskade und Idempotenz.
- Zusätzlich eine Testdatenbank mit dem bisherigen lokalen Image `localhost/games_collection:0.10` erzeugt und mit dem neu gebauten Testimage `localhost/games_collection:copies-test` geöffnet. Alle alten Tabelleninhalte unverändert; zwei erwartete Exemplare, ein Migrationsbeleg, Integritäts- und Fremdschlüsselprüfung erfolgreich. Erneuter Start mit dem Testimage ebenfalls erfolgreich.
- Firefox gegen einen separaten lokalen Gunicorn-Testserver mit aktuellem Quellcode: Hinzufügen, Bearbeiten/Abbrechen, Speichern/Neuladen, kombinierter Plattform-/Formatfilter sowie echtes HTTP 409 mit erhaltenem Editor geprüft. Sichtprüfung Cinematic/Desktop (1200 px) und Mobil (390 px), Editorial/Mobil (390 und 320 px). Keine Produktivdaten benutzt.
- Das fertig gebaute Testimage anschließend unter Gunicorn mit der migrierten Testdatenbank gestartet: `/health` erfolgreich, Session meldet `game_copies`. Der zuvor vorübergehend durch das Nutzungslimit der Freigabeprüfung blockierte Smoke-Test wurde nach wieder funktionierender Prüfung erfolgreich nachgeholt.
- Android: 48 JVM-Tests bestanden, Release-Lint ohne Befunde, `testReleaseUnitTest lintRelease assembleRelease --offline --no-daemon` erfolgreich, APK-v2-Signatur und Version 0.10 / Code 9 verifiziert. Lokale APK: `android/app/build/outputs/apk/release/Games-Collection-0.10-release.apk`. Weitere Details und verbleibende Geräteprüfung stehen im [Android-Review](../ANDROID_REVIEW.md). Keine Installation, kein Commit/Push, kein Upload und keine Veröffentlichung.
