# Freunde und Sammlungseinsicht

Lokaler Implementierungsstand vom 01.11.2026, noch nicht veröffentlicht. Auf ausdrücklichen Folgeauftrag vom 02.11.2026 sind Server/Web und Android nun auf **0.9** gesetzt (Android-Code **8**). Tags und Downloadlinks bleiben bei den veröffentlichten Artefakten 0.8.1.

## Nutzung

- Web: eigener Bereich **Freunde**, mobil über **Profil → Freunde und Anfragen**.
- Android: **Profil → Freunde und Anfragen**, native Compose-Oberfläche in beiden App-Designs.
- Anfrage über den genauen Benutzernamen eines registrierten Kontos auf demselben Server. Die bestehende Normalisierung ignoriert Groß-/Kleinschreibung und äußere Leerzeichen. Kein Benutzerverzeichnis und keine unvollständige Namenssuche.
- Getrennte Listen für Freunde, erhaltene und gesendete Anfragen. Nur der Empfänger kann annehmen; beide Seiten können eine Anfrage entfernen. Gegenseitige Anfragen werden nicht automatisch angenommen.
- Nach Annahme sehen beide die gesamte auf dem Server gespeicherte Sammlung des anderen: Titel/Cover, Jahr, Altersfreigabe, Besitzplattformen, Format, persönliche Bewertung, Status, Entwickler, Publisher, Engine und verfügbare Plattformen. Letztere bleiben von Besitzplattformen getrennt. Titelsuche und zusätzliche Spieldaten sind vorhanden; keine Bearbeitungs-/Löschaktionen für fremde Spiele.
- „Freundschaft beenden“ benötigt in der Oberfläche eine Bestätigung und entzieht die gegenseitige Freigabe für weitere Abrufe. Bereits angezeigte Daten oder Screenshots können nicht zurückgerufen werden; keine Echtzeit-Benachrichtigung oder Hintergrundaktualisierung.
- Freunde benötigen Verbindung zum aktualisierten Server. Keine Änderungen an Offline-Warteschlange oder IGDB. Fremde Sammlungen bleiben im Arbeitsspeicher und werden beim Schließen des Bereichs, beim Konto-/Serverwechsel und vor erneutem Abruf verworfen. API-Antworten sind `no-store`, der Service Worker cached keine API-Daten.

## Schema und API

`Friendship` / Tabelle `friendship`: ein sortiertes eindeutiges Benutzerpaar `user_low < user_high`, `requested_by` muss zum Paar gehören, Status `pending` oder `accepted`. Drei Fremdschlüssel mit `ON DELETE CASCADE`; Indizes für beide Paarseiten. Konto-Löschung entfernt zugehörige Beziehungen.

Beim Start ergänzt `db.create_all()` diese neue Tabelle mit ihren Indizes/Constraints auch in einer vorhandenen Datenbank. Keine bestehende Tabelle wird umgebaut und kein Spiel verändert. Das wurde mit bestehenden Konten in einer temporären Datenbank getestet; `create_all()` ist weiterhin keine allgemeine Migrationslösung. Vor Produktivupdates eine konsistente Datenbanksicherung anlegen. Keine echte Nutzdatenbank wurde geöffnet oder verändert.

| Methode und Route | Vertrag |
| --- | --- |
| `GET /api/friends` | Eigene Beziehungen mit `id` des Gegenübers, `username`, `status`, `incoming`; keine Adminflags oder Zugangsdaten |
| `POST /api/friends` | Ausschließlich `{"username":"…"}`; neue Anfrage `201`; bestehendes Paar `409`; Selbstanfrage `400`; unbekanntes Konto `404` |
| `PUT /api/friends/<user_id>` | Leeres JSON-Objekt; nur Empfänger darf annehmen |
| `DELETE /api/friends/<user_id>` | Leeres JSON-Objekt; Anfrage ablehnen/zurückziehen oder Freundschaft entfernen |
| `GET /api/friends/<user_id>/games` | Nur bei bestätigter eigener Beziehung; `{user: {id, username}, games: […]}`; sonst `403` |

Alle Endpunkte verlangen Login, Schreibaktionen zusätzlich den bestehenden CSRF-Schutz. Session-Capability `friendships: true`, API-Version bleibt 1. Der bestehende Serializer liefert Spieldaten; sämtliche bestehenden Spiel-Schreibendpunkte prüfen weiterhin den Eigentümer. Beziehungsmutationen prüfen beim Schreiben nochmals Paar, Absender und Status, damit veraltete Zugriffe keine inzwischen entfernten Beziehungen wiederbeleben. Eindeutigkeit verhindert doppelte Paare auch bei gleichzeitigen gegenseitigen Anfragen. Datenbankfehler nutzen Rollback und bestehende verständliche API-Fehler.

## Prüfungen und Grenzen

Die endgültigen Prüfergebnisse dieser Umsetzung stehen im aktuellen Abschnitt von `../PROJECT_CONTEXT.md`, `../SERVER_REVIEW.md` und `../ANDROID_REVIEW.md`. Backendtests verwenden ausschließlich temporäre Datenbanken; Webvorschau ausschließlich künstliche Konten. Keine Installation/Deinstallation, Produktivdatenänderung, Veröffentlichung oder Kommunikation mit echten Benutzern.

Offen bleiben Smartphone-/Lifecycle-Prüfung, längere reale Nutzung, große Freundeslisten/Sammlungen und Lastprüfung. Kein Push, keine E-Mail-Benachrichtigung, kein Blockier-/Meldesystem oder Anfragelimit ergänzt; Zugriff nur über exakten Benutzernamen. Listen und Sammlungen besitzen wie die bestehende eigene Sammlung keine API-Pagination. Diese Funktion ist keine öffentliche Profilfreigabe.
