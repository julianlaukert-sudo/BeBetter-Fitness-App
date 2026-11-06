# Abschließender Release – Deployment 3

## Vorher erfassen
- Echte App-Adressen über „Open app“ bzw. Settings -> Domains (nicht raten).
- Letzte funktionierende Release-IDs beider Apps (Tab Activity, z. B. v12).
- Kandidat-SHA: `git rev-parse HEAD` auf aktuellem main; main bis Ende beider Deployments einfrieren.
- Grüne CI genau für diesen SHA und erfolgreicher Live-Check der ExerciseDB.
- Manuelles Datenbank-Backup (data.heroku.com -> Datenbank -> Durability -> Create Manual Backup).
- Online lief bisher Sprint 3 (Flyway V1–V4) -> beim Deployment werden V5, V6, V7 nachgeholt.

## Backend-App – Config Vars
APP_BASE=backend
DATABASE_URL=kommt vom PostgreSQL-Add-on (nicht anfassen)
FRONTEND_URL=echte HTTPS-Frontend-Adresse ohne Pfad und ohne / am Ende
JWT_SECRET=vorhandenes privates Secret (unverändert lassen)
MAVEN_CUSTOM_OPTS=-DskipTests
SPRING_PROFILES_ACTIVE=heroku   <- NEU in Sprint 6
Einmalig für den Admin (danach wieder entfernen): ADMIN_BOOTSTRAP_ENABLED=true,
ADMIN_NAME, ADMIN_EMAIL (neue Adresse), ADMIN_PASSWORD (privat)

Buildpacks: 1. https://github.com/lstoll/heroku-buildpack-monorepo  2. heroku/java
Procfile (backend/Procfile): startet target/backend-0.0.1-SNAPSHOT.jar mit JDBC_DATABASE_*-Variablen.
Eine web-Instanz (Admin-Session und Sperrliste liegen im Prozessspeicher).

## Frontend-App – Config Vars
APP_BASE=frontend
VITE_API_URL=echte HTTPS-Backend-Adresse ohne Pfad und ohne / am Ende
Buildpacks: 1. Monorepo-Buildpack  2. heroku/nodejs
Procfile (frontend/Procfile): web: npx serve -s dist -l tcp://0.0.0.0:$PORT
VITE_API_URL wird beim Build eingebaut -> nach Änderung neu deployen. Keine Secrets im Frontend.

## Durchführung
1. Backup erstellen und ID/Zeitpunkt notieren.
2. Backend: Deploy -> Manual deploy -> main -> Deploy Branch. SHA in Activity vergleichen.
3. Logs: Flyway V5–V7 erfolgreich, keine Validierungsfehler; /health prüfen.
4. **Backend-GraphQL-Live-Smoke-Test:** Nach dem Backend-Deployment die tatsächliche Backend-Adresse über Heroku „Open app“ bzw. Settings -> Domains ermitteln (nicht raten).
    - Mit einem gültigen USER-Token `myProfile` und `currentPlan` über `POST /graphql` aufrufen.
    - Prüfen, dass beide Queries erfolgreich antworten und nur die Daten des angemeldeten Nutzers zurückgeben.
    - Mit einem gültigen ADMIN-Token zusätzlich eine Admin-Query, beispielsweise `adminStats`, aufrufen.
    - Prüfen, dass die Admin-Query erfolgreich antwortet.
    - Backend-Adresse, Zeitpunkt und Ergebnis des Live-Tests im RELEASE-PROTOKOLL festhalten.
5. Admin einmalig anlegen (Bootstrap-Variablen), Logs prüfen, dann ADMIN_BOOTSTRAP_ENABLED und ADMIN_PASSWORD wieder löschen.
6. Frontend: aus demselben main deployen; SHA vergleichen. **Für die GraphQL-Erweiterung allein ist kein Frontend-Deployment erforderlich, da das Frontend nicht geändert wurde.**
7. Online-Abnahme A25/A26 mit eigenen Testkonten.
8. Backend neu starten (More -> Restart all dynos), neu anmelden, Daten vergleichen (A21).
9. Release-IDs, SHA, Migrationsstand und Ergebnisse ins RELEASE-PROTOKOLL.

HTTP 429 der ExerciseDB oder ein nicht erstellbarer Erstplan verhindert die Fertigmeldung.
Heroku bricht Anfragen nach 30 s ab (H12): Logs prüfen, nicht nur Timeouts erhöhen.

## Wenn etwas schiefgeht
Keine weiteren Änderungen raten. Fehler, Logs, Release-ID und Zeitpunkt notieren.
Bei Bedarf in Activity „Roll back to vN“ für die betroffene App (nur wenn zum Schema passend).
Ein App-Rollback setzt die Datenbank NICHT zurück. Keine Migration löschen, kein Schema-Reset,
kein Backup ungeprüft zurückspielen (eigene Teamentscheidung wegen Datenverlust).