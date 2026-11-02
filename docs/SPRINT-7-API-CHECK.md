# Sprint 7 – ExerciseDB gezielt prüfen

Ausgangspunkt: Die Live-Probe aus Sprint 6 meldete am 30.10.2026 HTTP 429 (historische Beobachtung).
Die grüne Standardsuite nutzt kontrollierte Antworten und beweist keine externe Verfügbarkeit.

## Vor jedem echten Lauf (Person 3)
- Zeitpunkt (Europe/Berlin), Kandidat-SHA und Umgebung (lokal / GitHub) notieren.
- In der Heroku-Backend-App nachsehen, ob EXERCISEDB_URLS gesetzt ist (Wert nicht kopieren).
    - Nicht gesetzt -> Backend nutzt https://oss.exercisedb.dev/api/v1 -> BEBETTER_LIVE_URL leer lassen.
    - Gesetzt -> BEBETTER_LIVE_URL auf genau diese eine Adresse setzen.
- Eine erhaltene Wartezeit vollständig abwarten; nie in einer Schleife testen.

## Datenvertrag (unverändert)
GET /exercises?bodyParts=chest&limit=25 (optional after), Antwort `data` als Liste, optional `meta.nextCursor`.
Eintrag: exerciseId, name, bodyParts, targetMuscles, equipments, optional gifUrl.
Detail: GET /exercises/{id}, `data` als Objekt.
Eine andere Quelle mit anderem Schema oder Login braucht einen eigenen Arbeitsauftrag.

## Lokal
Mac:
mvn -B -f backend/pom.xml -Dbebetter.live=true -Dtest=LiveExerciseDbTest test
Windows (PowerShell):
mvn -B -f backend/pom.xml "-Dbebetter.live=true" "-Dtest=LiveExerciseDbTest" test
Optional vorher: BEBETTER_LIVE_URL (Mac: export, Windows: $env:) – kein Token in der URL.

## GitHub
Optional: Settings -> Secrets and variables -> Actions -> Variables -> BEBETTER_LIVE_URL.
Actions -> ExerciseDB Live Check -> Run workflow -> main. Genau einmal starten, Link notieren.

## Ergebnis auswerten
- Erfolg: 15 echte Übungen -> danach echter Browserablauf bis zum ersten Plan.
- 429: Zeitpunkt und ggf. Retry-After notieren; nicht umgehen, nicht sofort wiederholen.
- 401/403: Berechtigung beim Anbieter klären; kein Workaround im Browser.
- Timeout/5xx: Anbieterstatus und Backendlog vergleichen.
- 2xx mit unpassenden Daten: kein Erfolg; Datenvertrag prüfen.

## Grenzen
Der URL-Check prüft nur die Form. Die Anbieterpause lebt im laufenden Backendprozess;
ein Neustart oder mehrere Dynos sind keine Lösung für ein Anbieterlimit (eine web-Instanz).