# BeBetter Fitness App

Fitness-Web-App mit persönlichem Trainingsplan (Projekt Web-Entwicklung, DHBW Mosbach).
React (SPA) ist die Nutzeroberfläche; Spring Boot liefert die JSON-API, eine GraphQL-Admin-API
und serverseitig gerenderte Thymeleaf-Adminseiten (MPA).

## Projektanforderungen – wo sie umgesetzt sind

| Kriterium | Umsetzung im Code |
|---|---|
| API-Design | REST/JSON, einheitliche Fehler (HTTP 400 + `message`, `errors`), Vertrag in `docs/API.md` |
| MPA | Adminbereich mit Thymeleaf: `/admin/overview`, `/admin/accounts`, Löschbestätigung, Logout (Session + CSRF) |
| SPA | React 19 + React Router: Registrierung, Login, Onboarding, Training, Markierungen, Profil |
| Backend | Spring Boot 3.5, JPA, Flyway V1–V7, Spring Security mit JWT, Planlogik, Wochenwechsel-Job |
| GraphQL | `POST /graphql` (nur ADMIN): `adminUsers`, `adminStats`, Mutation `deleteUser` |
| Integration | Externe ExerciseDB (Cache, Rate-Limit-Schutz), Frontend↔Backend, Integrationstests mit PostgreSQL |
| Infrastruktur | Docker-PostgreSQL, GitHub-CI (Backend, Frontend, statische Probe), Live-API-Workflow, zwei Heroku-Apps, Heroku-Profil mit HTTPS-Cookies, Release mit Backup |

## Voraussetzungen
- Java 21, Maven 3.9, Node 22, npm, Git, Docker Desktop, IntelliJ.
- Mac: Terminal. Windows: PowerShell im IntelliJ-Terminal (Befehle siehe docs/SETUP.md).

## Lokal starten
Datenbank (Port 5433):

    docker compose up -d

Backend (eigenes Terminal; `JWT_SECRET` mit mindestens 32 Zeichen privat setzen):

    mvn -f backend/pom.xml spring-boot:run

Health: http://localhost:8080/health. Flyway führt V1–V7 aus, Hibernate validiert danach.

Frontend (zweites Terminal):

    cd frontend
    npm ci
    npm run dev

Browser: http://localhost:5173. Lokal bleibt `VITE_API_URL` leer (Vite-Proxy); die Admin-Übergabe
nutzt dann http://localhost:8080. `FRONTEND_URL` ist lokal http://localhost:5173.

Admin einmalig lokal anlegen: `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_NAME`, `ADMIN_EMAIL`,
`ADMIN_PASSWORD` nur privat im Terminal bzw. in der Run-Konfiguration setzen, einmal starten,
danach `ADMIN_BOOTSTRAP_ENABLED` und `ADMIN_PASSWORD` wieder entfernen.

## Tests
Testdatenbank `bebetter_test` (einmalig angelegt, siehe docs/SETUP.md).

    mvn -B -f backend/pom.xml clean verify
    cd frontend
    npm ci
    npm test
    npm run build
    npm run test:static

Echte ExerciseDB bewusst und einzeln prüfen (nicht in einer Schleife):

    mvn -B -f backend/pom.xml -Dbebetter.live=true -Dtest=LiveExerciseDbTest test

Alternativ in GitHub: Actions -> ExerciseDB Live Check -> Run workflow.
Ein übersprungener Live-Test im normalen Lauf ist kein Verfügbarkeitsnachweis.

## Betrieb (Heroku)
Zwei Apps (Backend mit PostgreSQL, Frontend), Monorepo-Buildpack + heroku/java bzw. heroku/nodejs,
`APP_BASE=backend` bzw. `frontend`, manuelle Deployments. Backend mit `SPRING_PROFILES_ACTIVE=heroku`.
Details: docs/RELEASE.md, Ergebnisse: docs/RELEASE-PROTOKOLL.md.

## Umfang und Grenzen
Kein Kalender, keine leistungsabhängige Prozentanpassung, kein PB-15/PB-19.
Admin-Session und Logout-Sperrliste liegen im Backendprozess (nach Neustart neu anmelden).

## Dokumente
docs/API.md · docs/SETUP.md · docs/NUTZERHANDBUCH.md · docs/sprint-6-abnahme.md ·
docs/RELEASE.md · docs/RELEASE-PROTOKOLL.md

Secrets, Tokens, Passwörter und Datenbankadressen werden nie committed.