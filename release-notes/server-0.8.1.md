# Games Collection v0.8.1

## Neue Funktion

- **Titeltypfilter in der IGDB-Suche:** Im Web-Spieleditor steht unter dem Release-Jahr ein Dropdown für Spielevollversion, DLC, Erweiterung, Bundle und die weiteren IGDB-Typen. Standard ist „Alle Typen“.
- Titel, optionales Release-Jahr und Titeltyp werden gemeinsam bei IGDB gefiltert, bevor das Trefferlimit greift. Auch die numerische IGDB-ID-Suche berücksichtigt den Typ.
- Der Server prüft die Typwerte. Bestehende Clients ohne Titeltyp bleiben kompatibel; der Filter wird nicht am Sammlungseintrag gespeichert.
- Zentrale Web-/Serverversionsangabe auf **0.8.1** gesetzt; API-Protokollversion weiterhin 1.

## Voraussetzungen und Update

Vorhandene Twitch-/IGDB-Konfiguration und das persistente Verzeichnis `/app/data` weiterverwenden. Keine Datenbank-Schemaänderung gegenüber 0.8. Vor dem Update ein konsistentes SQLite-Backup erstellen.

Nach erfolgreicher Image-Veröffentlichung in `docker-compose.yaml` für eine feste Version `image: ghcr.io/mehmet-b-90/games_collection:0.8.1` setzen und `docker compose pull` sowie `docker compose up -d` ausführen. Alternativ bleibt `latest` an den erfolgreichen Build von `main` gebunden. Die Veröffentlichung ersetzt keinen laufenden Produktivcontainer.

## Prüfungen und bekannte Einschränkungen

- Lokales Docker-Format-Image mit Podman gebaut; **46 Backendtests im Image bestanden**, ohne Netzwerk und ausschließlich mit temporären Testdaten. Gunicorn-Start und `/health` mit HTTP 200 / `status: ok` geprüft.
- JavaScript-Syntax und Suchverhalten geprüft; lokale Browser-Vorschau in Cinematic Dark und Editorial Shelf auf Desktop und bei 390 px geprüft. Kein vollständiger neuer Live-End-to-End-Test gegen IGDB oder echtes Smartphone.
- Die Einordnung stammt von IGDB: Sondereditionen können ebenfalls als Spielevollversion gelten. Keine Garantie auf eine fehlerfreie Klassifizierung durch IGDB.
- GitHub Actions prüft Tests und Containerstart erneut vor dem AMD64-/ARM64-Upload. Lokal wurde AMD64 geprüft. Bekannte Flask-Login-DeprecationWarning bleibt.
