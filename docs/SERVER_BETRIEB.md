# Serverbetrieb

Die Ersteinrichtung steht in der [Projekt-README](../README.md#server-mit-docker-einrichten).

## Exemplare: Update vom bisherigen Besitzmodell

Der lokale Quellstand vom 10.11.2026 ergänzt eine transaktionale Datenmigration zu einzelnen Exemplaren. Vor dem Update synchronisieren, konsistent sichern, alte Serverinstanzen stoppen und den neuen Server mit demselben Datenverzeichnis starten. [Übernahme, Clientwechsel und Rollbackgrenzen](EXEMPLARE.md). Noch keine Veröffentlichung oder Produktivumstellung.

## Festes Server-Image verwenden

Für eine feste veröffentlichte Version im Compose-Eintrag `image:` einen vorhandenen Tag wie `ghcr.io/mehmet-b-90/games_collection:0.9` setzen. Vor Updates den gewünschten Tag und die Hinweise im jeweiligen [Server-Release](https://github.com/mehmet-b-90/games_collection/releases) prüfen.

## Lokal aus dem Quellcode bauen

Für eigene Änderungen zuerst den Eintrag `image:` in `docker-compose.yaml` auf `games-collection:local` setzen und anschließend das lokale Image bauen und starten:

```bash
docker build -t games-collection:local .
docker compose up -d --pull never
```

Der Docker-Buildkontext enthält ausschließlich Dockerfile, `requirements.txt` und die Anwendung unter `files/`. Lokale Konfiguration, Datenbank, Git-Verlauf und Android-Werkzeuge werden nicht an den Builder übertragen.

Mit Podman den lokalen Build als `localhost/games-collection:local` markieren und diesen Namen im Compose-Eintrag `image:` verwenden:

```bash
podman build --format docker -t localhost/games-collection:local .
podman compose up -d --pull never --force-recreate games_collection
```

Bei `ModuleNotFoundError: No module named 'PIL'` enthält der gestartete Container die neue Cover-Abhängigkeit nicht. Pillow steht in `requirements.txt` und wird beim Image-Build installiert. Wenn der lokale Quellcode in ein vorhandenes Image eingebunden wird, den Mount für `/app/files` beim Test mit dem frisch gebauten Image weglassen. Ein Neustart des alten Containers übernimmt die neue Abhängigkeit nicht.

## Fehlerdiagnose im Serverbetrieb

Bei Datenbank- oder unerwarteten Serverfehlern zeigt die Anwendung eine deutsche Meldung mit Fehler-ID. Dieselbe ID steht im Betreiber-Log; API-Antworten enthalten zusätzlich `request_id` und den Header `X-Request-ID`.

```bash
docker compose logs --since=30m games_collection
```

Im Log nach der gemeldeten ID suchen. Datenbankfehler liefern HTTP 503; Schreibkonflikte liefern 409 und erfordern einen erneuten Abruf des Datensatzes. SQL-Parameter werden in Datenbank-Tracebacks ausgeblendet. Die Ergebnisse und Grenzen der aktuellen Prüfung stehen in [SERVER_REVIEW.md](../SERVER_REVIEW.md).

## Datenbank sichern und wiederherstellen

Administratoren öffnen im Web über das Zahnrad die **Serververwaltung**. Dort stehen **Datenbank exportieren** und die **Benutzerverwaltung** gemeinsam bereit. Der Export erstellt im laufenden Betrieb einen konsistenten SQLite-Snapshot und lädt `Games-Collection-Backup-<Datum>_<Zeit>_UTC.db` herunter. Prüfe nach dem Download, dass die Datei tatsächlich gespeichert wurde.

Das Backup enthält sämtliche Konten (einschließlich Passwort-Hashes), Sammlungen, Besitzplattformen, Exemplare, Migrationsbelege, Freundschaften, eigene Cover und Vorgangsbelege. Geschützt und getrennt vom VPS aufbewahren. `.env`, Server-/Proxykonfiguration und sonstige Dateien außerhalb der Datenbank müssen separat gesichert werden. Der Download ist nicht verschlüsselt; für produktiven Fernzugriff HTTPS verwenden.

Eine Wiederherstellung erfolgt bewusst außerhalb der Weboberfläche: alle auf die Datenbank zugreifenden Serverinstanzen stoppen, den aktuellen Datenbestand einschließlich eventuell vorhandener `database.db-wal` und `database.db-shm` separat sichern und die Backup-Datei im Datenverzeichnis als `database.db` bereitstellen. Alte WAL-/SHM-Dateien dürfen nicht mit der wiederhergestellten Datei kombiniert werden. Dateieigentümer und Berechtigungen passend zum Container erhalten. Zunächst mit einer Kopie und zur Sicherung passender Serverversion prüfen, danach Konten und Sammlungen kontrollieren. Bei Compose liegt die Datenbank standardmäßig unter `./data/database.db`. Eine Wiederherstellung setzt alle Daten auf den Sicherungszeitpunkt zurück; neuere Änderungen sind darin nicht enthalten.

Der produktive Datenbestand muss bei Updates erhalten bleiben. Diese Verwaltungsfunktion verändert das Schema nicht. `create_all()` ersetzt keine Migration bestehender Tabellen; künftige Schemaänderungen benötigen einen datenerhaltenden, geprüften Upgradeweg.
