# Games Collection

Games Collection ist eine selbst gehostete Webanwendung zur Verwaltung einer persönlichen Videospielesammlung. Spieldaten und Cover können über IGDB gesucht werden. Die Anwendung bietet zwei Designs, Filter, Statistiken, Benutzerkonten und eine installierbare PWA-Oberfläche.

## Voraussetzungen

- Docker Engine
- Docker Compose
- Twitch-/IGDB-Zugangsdaten für die Spielesuche

## Einrichtung

1. Beispieldatei für die Konfiguration kopieren:

   ```bash
   cp .env.example .env
   ```

2. In `.env` einen langen, zufälligen `SECRET_KEY` sowie die Twitch-/IGDB-Zugangsdaten eintragen.

3. Anwendung bauen und starten:

   ```bash
   docker compose up -d --build
   ```

4. `http://localhost:7000` öffnen. Der zuerst angelegte Benutzer erhält Administratorrechte.

Die SQLite-Datenbank wird im lokalen Verzeichnis `data/` abgelegt und nicht in das Repository übernommen.

## Tests

```bash
python -m unittest discover -s tests -v
```

## Technischer Aufbau

- Flask und Gunicorn
- SQLAlchemy mit SQLite
- Bootstrap sowie modulares JavaScript
- Docker Compose
- PWA mit Service Worker und Web-App-Manifest
