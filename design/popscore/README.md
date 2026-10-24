# Aktuell beliebt – Umsetzung und Entwurfshistorie

## Aktueller Stand – 24.10.2026

Der Betreiber hat die Gestaltung bestätigt und präzisiert: **ausschließlich unter Entdecken**, in Web und Android. Diese Entscheidung ist umgesetzt; die untenstehenden Varianten beschreiben die frühere Entwurfsphase und sind keine offenen Auswahlfragen mehr.

- Coverraster mit PopScore-Rang, Genre, IGDB-Bewertung, „In Sammlung“ und „+ Hinzufügen“ in beiden Designs. Keine beliebten Titel auf der Sammlungsseite.
- Gemeinsames `GET /api/discover` (Anmeldung erforderlich), bis zu 100 Titel nach IGDB Visits. Täglicher Cache im Serverprozess, Fehler-Backoff 60 Sekunden, gekennzeichnete Ersatzdaten bis sieben Tage. Serverneustart verwirft Cache. Die Live-Abfrage hat 100 Titel geliefert; Reihenfolge unterscheidet sich von der IGDB-Startseite.
- Editor wiederverwendet, keine automatische persönliche Bewertung und keine automatisch gewählten Besitzplattformen. Keine zusätzlichen Entdecken-Plattformfilter umgesetzt; die Chips gehörten nur zur alternativen Entwurfsskizze.
- Web-Desktop und mobile Webansicht (390 px) in beiden Designs geprüft. Testkonto und Schreibtest ausschließlich in temporärer Container-Datenbank. Aktuelle Screenshots: `web-cinematic.png`, `web-editorial.png`, `web-mobile-cinematic.png`, `web-mobile-editorial.png`.
- 44 Backendtests und 34 Android-JVM-Tests bestanden, Release-Lint ohne Befunde, signierte Release-APK gebaut/verifiziert. Android-Gerätesichtprüfung offen. Details in den Reviewdateien.
- Übergabe am 24.10.2026 beauftragt: Commit/Push auf `feature/popscore` und Server-Release `docker-v0.8` / Image `:0.8`; Workflow-Erfolg separat prüfen. Android bleibt 0.7 / Code 4 ohne APK-/Play-Veröffentlichung. Gerätetest offen. Aktueller Servercode ist Voraussetzung für den neuen Android-Bereich.

## Historie: Auftrag und Stand vor Auswahl

Feature für Web und native Android-App, Branch `feature/popscore`. Der Nutzer wünscht zuerst Entwürfe, bevor die Darstellung implementiert wird. Bestehende lokale Änderungen bleiben erhalten. Keine Version geändert, kein Commit/Push und keine Veröffentlichung.

## Historie: Vorschläge zur Auswahl

- **Coverleiste:** kompakter Einstieg oberhalb der Sammlung; Cover, Rang, Titel, Genre, IGDB-Bewertung, Besitzhinweis und Hinzufügen. „Alle ansehen“ führt konzeptionell zur vollständigen Liste. Empfehlung als Einstieg; auf dem Smartphone horizontal weiterblätterbar in der späteren Umsetzung. Die aktuelle Vorschau zeigt zwei Karten ohne echte Wischfunktion.
- **Entdecken:** eigener Bereich mit Coverraster und Plattformfiltern, auf Android zusätzlicher Navigationseintrag. Mehr Raum zum Stöbern; die Navigation muss bei der Umsetzung auf kleinen Geräten überprüft werden.
- **Rangliste:** eigener Bereich mit nummerierten, platzsparenden Zeilen und kleinen Covern; geeignet zum schnellen Überblick.

Alle Entwürfe bieten über die Darstellungsoptionen Cinematic Dark und Editorial Shelf sowie Web und Android. Die Android-Vorschau berücksichtigt eine durchgehende Bottom-Bar mit pillenförmiger aktiver Symbolfläche. Ein Klick auf Hinzufügen illustriert lediglich den vorgesehenen Übergang zum vorausgefüllten Editor und speichert nichts. Weitere Navigation und Plattformchips sind Gestaltungselemente, noch keine fertigen Funktionen.

## Historie: Daten und Umsetzungsvorschlag vor Auswahl

- Popularität zunächst nach IGDB-Seitenaufrufen (`popularity_type = 1`) über `/v4/popularity_primitives`, danach Spieldetails anhand `game_id` laden. Die konkrete Rangfolge der IGDB-Website ist damit nicht garantiert.
- PopScore-Rang und IGDB-Nutzerbewertung getrennt anzeigen; keine Popularitätswerte als Bewertung speichern. Bewertungen im Mockup stammen aus der Vorlage und sind keine verifizierten aktuellen API-Daten.
- Gemeinsamer serverseitiger Abruf/Cache für beide Clients wäre sinnvoll; Aktualisierung passend zum dokumentierten täglichen PopScore-Zyklus. Lade-, Fehler-, Leer- und veraltete Cachezustände bei der Implementierung berücksichtigen.
- Besitz anhand der eigenen Sammlung/IGDB-ID markieren; Hinzufügen öffnet den vorhandenen Editor. Verfügbare Plattformen sind nicht automatisch Besitzplattformen.
- Quellen: https://api-docs.igdb.com/#popscore und https://www.igdb.com/ (in dieser Unterhaltung vor der Entwurfsphase recherchiert).

## Historie: Mockup-Vorschau und damalige Prüfungen

Inline-Entwürfe liegen im zugehörigen Chat-Verzeichnis `/home/mehmet/.codex/visualizations/2026/10/24/01a16fca-30d6-7493-ab0c-697c017d4fe3/`: `popscore-entwuerfe.html` und `popscore-android.html`. Das sind lokale Gesprächsvorschauen, keine ausgelieferten Produktdateien; ein Repository-Klon enthält sie nicht.

- Fragment durch den Visualize-Renderer erfolgreich verarbeitet, eingebettete lokale Cover/Projektlogo, keine API-Abfragen.
- Web- und Android-Coverleiste in der lokalen Browser-Vorschau visuell geprüft. Themewechsel und übrige Varianten noch nicht separat visuell abgenommen.
- Kein Backend-/Android-Testlauf, da keine Produktcodeänderung; keine Prüfung auf einem echten Smartphone.
- Nächster Schritt: Gestaltung mit dem Betreiber auswählen, danach Backend, Web und Android implementieren und prüfen.
