# Serverprüfung vom 19.10.2026

Geprüft wurde der lokale Arbeitsstand einschließlich bereits vorhandener Änderungen. Umfang: App-Factory und Konfiguration, Modelle und SQLite-Einstellungen, Spiel-/Konto-/Sitzungs-/Authentifizierungsrouten, Validierung, Idempotenz, IGDB/Twitch sowie die Fehlerverarbeitung der vorhandenen Webclients. Dies ist eine gezielte Stabilitäts- und Integritätsprüfung, keine vollständige Sicherheits- oder Lastprüfung.

## Behobene Befunde

| Priorität | Befund | Korrektur und Nachweis |
| --- | --- | --- |
| P1 | Reine Besitzplattformänderungen änderten nur die Beziehungstabelle. SQLAlchemy erhöhte die Spielversion dadurch nicht; konkurrierende Bearbeitungen konnten sich überschreiben. | Jede akzeptierte Spielbearbeitung erhöht die Elternversion. Tests mit zwei parallelen Plattformänderungen bestätigen einmal HTTP 200 und einmal 409; Wiederholung mit derselben Vorgangskennung erhöht nur einmal. |
| P1 | Zwei parallele Erstregistrierungen konnten beide eine leere Benutzertabelle sehen und Administratorrechte erhalten. | Die Entscheidung fällt atomar im INSERT anhand der zu diesem Zeitpunkt bestehenden Benutzer. Ein paralleler Registrierungstest bestätigt zwei Konten mit genau einem Administrator. |
| P2 | Datenbank-Lesefehler und unerwartete Exceptions hatten keine einheitliche benutzerverständliche Antwort. | Zentrale Rollbacks, JSON für APIs und eine deutsche HTML-Fehlerseite. Datenbankfehler liefern 503, unerwartete Fehler 500. Antwort und Betreiber-Traceback enthalten dieselbe zufällige Fehler-ID. |
| P2 | Listen/Objekte in Format, Status oder Theme lösten TypeError aus. Unicode-Ziffern und zu große Ganzzahlen konnten Konvertierungs-/SQLite-Fehler erzeugen. Falsch typisierte leere Kontofelder wurden teilweise still ignoriert. | Typprüfung vor Set-Zugriff, gemeinsame begrenzte Ganzzahlvalidierung, Bereichsprüfung für Ressourcen-IDs und strengere Kontofelder. Ungültige Eingaben werden mit 400 bzw. nicht existierende Ressourcen mit 404 abgewiesen. Numerische Textwerte bestehender Webclients bleiben zulässig. |
| P2 | IGDB-Antworten mit ungültigen Listeneinträgen oder verschachtelten Altersfreigaben konnten Exceptions auslösen. Netzwerk-/OAuth-Fehler waren im Betreiber-Log nicht erkennbar. | Prüfung der Antwortstruktur, robuste USK-/PEGI-Auswahl und Warnlogs mit Fehler-ID, Exception-Typ und verfügbarem HTTP-Status. Requests-Exceptiontexte und externe Antwortkörper werden nicht geloggt, da sie Tokens enthalten können. |
| P2 | SQLAlchemy-Tracebacks konnten gebundene Parameter einschließlich Kontodaten enthalten. | Engine mit `hide_parameters=True`; ein absichtlich fehlschlagender SQL-Aufruf bestätigt, dass ein vertraulicher Parameter nicht in der Fehlermeldung auftaucht. |
| P2 | Fehlerantworten waren nicht durchgehend gegen Caching geschützt; die JSON-Umwandlung von HTTP-Fehlern verlor Protokollheader. | `no-store` für APIs, Fehler und persönliche HTML-Seiten; `Allow` bleibt bei 405 erhalten. HTTP 413 nennt die Größenbegrenzung. |
| P2 | Ein relativer `DATABASE_PATH` ohne Verzeichnis scheiterte bereits beim Anlegen des Verzeichnisses. | Pfad vor Verzeichniserstellung und Engine-Konfiguration absolut auflösen; relative Angaben beziehen sich auf das Arbeitsverzeichnis. Der Containerstandard bleibt `/app/data/database.db`. |

Zusätzlich: neue Passwörter sind bei Registrierung und Kontobearbeitung wie beim nativen Login auf höchstens 4096 Zeichen begrenzt. Vorhandene längere Passwörter bleiben über den bisherigen Weblogin nutzbar. Ungültige Benutzerkennungen im Sitzungscookie werden als fehlende Anmeldung behandelt. Der Healthcheck protokolliert Datenbankausfälle mit Fehler-ID.

## Betreiberdiagnose

Bei HTTP 5xx erscheint eine Fehler-ID direkt in der Benutzermeldung. APIs liefern sie außerdem als `request_id`; alle Antworten tragen `X-Request-ID`. Die ID wird vom Server erzeugt und nicht aus einem Clientheader übernommen. Mit dieser ID lässt sich der zugehörige Eintrag in den Containerlogs finden:

```bash
docker compose logs --since=30m games_collection
```

Datenbank- und unerwartete Fehler enthalten einen Traceback und die Routenvorlage, keine absichtlich mitprotokollierten Request-Bodies, Cookies oder Querystrings. Erwartete Validierungs-/Versionskonflikte benötigen keinen Fehler-Traceback. Zugriff auf Betreiberlogs weiterhin beschränken.

Eine 503 bedeutet einen derzeit fehlgeschlagenen Datenbankzugriff, nicht zwingend beschädigte Daten. Mount-/Schreibrechte, freien Speicher und das konkrete Log prüfen. Ein Verbindungsabbruch kann weiterhin einen unklaren Schreibausgang bedeuten: Spieländerungen mit derselben Idempotenzkennung und demselben Payload wiederholen, keine neue Kennung erzeugen.

## Verifikation und Grenzen

- `python -m unittest discover -s tests -v`: **36 Tests bestanden**, mit den festgelegten Paketen aus `requirements.txt`, Python 3.14 und ausschließlich temporärer Datenbank. Enthält echte parallele SQLite-Schreibtests, Rollback-/Idempotenzprüfungen, fehlerhafte JSON-Werte, Ausfallantworten, Größenlimit und SQL-Parameterabschirmung.
- Gunicorn-Smoke-Test mit einem Worker/vier Threads, Loopback-Port und temporärer Datenbank: Start, Health, Registrierung, CSRF, ungültiges Theme, Spielanlage und Plattformversionierung geprüft.
- `git diff --check`: ohne Befunde.
- Flask-Login meldet unter Python 3.14 eine bestehende DeprecationWarning zu `datetime.utcnow()`; die Tests bestehen. Keine Fremdpakete oder Pins geändert.
- Während der ursprünglichen Prüfung kein Docker verfügbar. Für die anschließende Server-Veröffentlichung 0.7 wurde das Image mit Podman gebaut: 36 Backend-Tests im Container bestanden und Healthcheck erfolgreich. Kein Produktivdeployment, Live-Twitch-/IGDB-Test, Last-/Dauertest oder visuelle Prüfung beider Designs durchgeführt. Kein laufender Produktivserver und keine Nutzdaten verändert.
- Die vorher vorhandene Entfernung von `files/schema.py` bleibt erhalten. Keine Migration ergänzt: `create_all()` migriert bestehende Tabellen nicht. Alte Datenbanken ohne benötigte Spalten müssen vor einem produktiven Update gesondert migriert werden. `/health` prüft die DB-Verbindung, nicht die Vollständigkeit des Schemas.
- DELETE bleibt gemäß bestehendem API-Vertrag ohne Versionsprüfung; Kontobearbeitungen besitzen ebenfalls keine optimistische Versionierung. Die hier ergänzte Versionsabsicherung gilt für Spiel-Updates.
- Vorbestehende Änderungen an Android, Compose, `.env.example` und Projektkontext wurden erhalten. Die anschließende Veröffentlichung des geprüften Serverstands als Version 0.7 ist separat beauftragt; lokale Android-Änderungen und Zugangswerte bleiben davon ausgenommen.
