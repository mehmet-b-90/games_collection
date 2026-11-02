# Games Collection v0.9

## Neue Funktion
### Freunde
- Benutzer können sich mit anderen befreunden und somit die Sammlung des anderen einsehen.
- Verwaltung unter Profil -> Freunde und Anfragen
<img width="374" height="822" alt="image" src="https://github.com/user-attachments/assets/bf4d3bef-b069-4ca5-a3a7-a93ade42356e" />

## Änderungen
- Die drei Kennzahlen "Spiele", "Durchnittliche Bewertung" und "Plattformen" in der Sammlung-Ansicht, in der mobilen Web-Ansicht, wurde in die Statistik-Ansicht verschoben. Somit ist die Sammlung-Ansicht übersichtlicher.
<img width="359" height="331" alt="image" src="https://github.com/user-attachments/assets/7e77a6bd-8f6d-4728-8b49-168b38aaeab9" />

## Voraussetzungen und Prüfungen

- Freunde benötigen den aktualisierten Server und eine Onlineverbindung. Vor dem Serverupdate die persistente SQLite-Datenbank sichern; die Tabelle `friendship` wird additiv ergänzt.
- 51 Backendtests bestanden; Android-Prüfberichte mit 39 erfolgreichen Tests, Release-Lint ohne Befunde und signierter APK 0.9 / Code 8.
- Smartphone-/Lifecycle-Prüfung und längere reale Nutzung bleiben offen. Ein veröffentlichtes Image ersetzt keinen laufenden Servercontainer.
