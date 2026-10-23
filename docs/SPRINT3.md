# Sprint 3 lokal prüfen

Voraussetzung: fertiger Sprint 2 auf main, Java 21, Node 22, Docker,
eigener JWT_SECRET beim normalen Backendstart.
Lokale Datenbank: bebetter. Testdatenbank: bebetter_test (Port 5433).
Migrationen: V1 bis V4; bestehende Migrationen nicht ändern.

Im Hauptordner:
docker compose up -d
mvn -f backend/pom.xml clean verify
mvn -f backend/pom.xml spring-boot:run

Im Ordner frontend:
npm ci
npm test
npm run build
npm run dev

Browser: http://localhost:5173
Neues Konto -> Onboarding -> /training -> Einheit -> Übungsdetails.
Ein Nutzer mit fertigem Profil landet nach dem Login auf /home und öffnet Training direkt.

Optionaler echter API-Test, einmal getrennt ausführen:
macOS:    mvn -f backend/pom.xml test -Dtest=LiveExerciseDbTest -Dbebetter.live=true
Windows:  mvn -f backend/pom.xml test "-Dtest=LiveExerciseDbTest" "-Dbebetter.live=true"

Gezielte Ausfallprobe (lokal, nie auf Heroku): Backend mit
EXERCISEDB_URLS=http://127.0.0.1:9 starten, danach normal neu starten.

EXERCISEDB_URLS ist optional; Standardadresse steht im Client.
JWT_SECRET bleibt ausschließlich im Backend.
VITE_API_URL ist die öffentliche Backend-Origin beim Frontend-Build.
Kein Kalender, keine Markierungen, noch kein Wochenwechsel (Sprint 4).

## Tatsächliche Nachweise 
Geprüfter Commit: 6fb7d66
Datum und prüfende Personen: 23.10.2026, Person 1, Person 2, Person 3
Lokale automatische Tests: 53 Backend-Tests grün, 1 Live-Test übersprungen; 7 Frontend-Tests und Build grün
Browserablauf und Kontowechsel: durchgeführt
API-Ausfall und erneuter Versuch: durchgeführt
Live-API-Ergebnis: offen (HTTP 429 Rate Limit)
Manuell veröffentlichter Backend-Commit und URL: 6fb7d66, https://bebetter-fitness-app-0caa005ed4cc.herokuapp.com
Manuell veröffentlichter Frontend-Commit und URL: 6fb7d66, https://bebetter-frontend-8982b2eba40e.herokuapp.com
Offene Fehler: keine