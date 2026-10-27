# Training erfassen und einfacher Wochenwechsel

## Getesteter Stand
Datum: noch einzutragen
Commit-SHA: noch einzutragen
Person und Betriebssystem: noch einzutragen (Person 1 Mac, Person 2 Mac, Person 3 Windows)

## Ergebnis nach Umsetzung
- [ ] Eigene Übung geschafft/nicht geschafft mit Istwerten speichern.
- [ ] Korrektur bis einschließlich Sonntag, gleiche Markierungs-ID.
- [ ] Zukunft, fremde Übung und geschlossene Woche werden abgewiesen.
- [ ] Bei Teilmarkierung nur offene Übungen automatisch nicht geschafft.
- [ ] Ganz unmarkierte Einheit bleibt ohne alte Markierung und wird übernommen (carried).
- [ ] Neue Woche: gleiche Vorgaben, neue IDs, keine kopierten Istwerte.
- [ ] Mehrere verpasste Wochen und parallele Anfragen erzeugen keine Duplikate.
- [ ] Fehlende Übungsdetails (ExerciseDB) verhindern keine gespeicherten Trainingswerte.
- [ ] Backend verify, Frontendtests (11) und Build erfolgreich.
- [ ] Browserablauf auf Mac und Windows geprüft.
- [ ] Alle sechs PRs tatsächlich geprüft und CI grün.

## Testbefehle aus dem Repository-Hauptordner
    docker compose up -d
    mvn -B -f backend/pom.xml clean verify
    cd frontend
    npm ci
    npm test
    npm run build
    cd ..

## Vereinbarte Grenzen
Keine Gewichts-/Wiederholungsanpassung, kein Kalender und kein Deployment in Sprint 4.
Heroku läuft weiterhin mit dem Sprint-3-Stand.
Ein Wertepaar pro Übung; bei mehreren Sätzen Werte des letzten absolvierten Satzes.
Automatische Fehlmarkierungen sind keine gemessenen Trainingswerte.
Der Job läuft montags 00:05 Europe/Berlin nur bei laufendem Backend.
GET /plans/current und POST /plans holen versäumte Wochen ebenfalls nach.
Nicht ausgeführte Prüfungen bleiben offen; keine erfundenen Nachweise.
Externe ExerciseDB-Verfügbarkeit wird durch die Mocktests nicht nachgewiesen.
Adminoberfläche (mit GraphQL) folgt in Sprint 5.

## Beobachtungen und Restpunkte
Hier tatsächliche Ergebnisse, Fehler und Nachprüfungen eintragen.