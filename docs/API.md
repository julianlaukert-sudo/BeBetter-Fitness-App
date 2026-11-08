## Übersicht

| Bereich | Endpunkte | Zugriff |
|---|---|---|
| Health | GET /health | offen |
| Konto | POST /users, POST /auth, DELETE /auth | offen / Token |
| Profil | GET/POST/PUT /profile | USER |
| Training | POST /plans, GET /plans/current, POST /marks | USER |
| Admin-JSON | GET /admin/users, GET /admin/stats, DELETE /admin/users/{id} | ADMIN-Bearer |
| GraphQL | POST /graphql | ADMIN-Bearer |
| Adminseiten (MPA) | POST /admin/sso, /admin/overview, /admin/accounts, /admin/accounts/{id}/delete, POST /admin/logout | Admin-Session + CSRF |

Fehler: HTTP 400 mit `message` (und bei Eingabefehlern `errors`).

# API-Vertrag (Sprint 2)

Konvention (wie Referenz): Erfolg = HTTP 200. Fehler = HTTP 400 mit
`{ "message": "...", "errors": { "feld": "..." } }` (`errors` nur bei Eingabefehlern).
Geschützte Endpunkte brauchen `Authorization: Bearer <token>`. Die Nutzer-ID kommt immer aus dem Token.

## POST /users (öffentlich)

Request: `{ "name": "...", "email": "...", "password": "..." }`

Antwort: `{ "message": "User created successfully.", "userId", "name", "email", "role": "USER" }`

Regeln: name 2–50 Zeichen; E-Mail gültig, max. 255, eindeutig (Groß-/Kleinschreibung egal);
password 8–64 Zeichen, max. 72 UTF-8-Byte, Großbuchstabe, Zahl (0–9), Sonderzeichen.

Fehler: `Invalid input.` (mit errors), `User already exists.`

## POST /auth (öffentlich)

Request: `{ "email": "...", "password": "..." }`

Antwort: `{ "token", "tokenType": "Bearer", "expiresIn": 3600, "userId", "name", "role", "profileCompleted" }`

Fehler: `Email or password is wrong.`

## DELETE /auth (angemeldet)

Sperrt genau dieses Token bis zu seinem Ablauf (Speicher des Backendprozesses).

Antwort: `{ "message": "User logged off." }`

## Fehler bei Tokens und Rechten

`Token is wrong.` (fehlt, ungültig, abgelaufen, gesperrt) · `Access denied.` (falsche Rolle)

## GET /profile (Token nötig)

Liefert das eigene Profil. Die Nutzer-ID kommt nur aus dem Token.

- `200`: `ProfileResponse` (siehe unten)
- `400`: `{"message":"Profile not found."}` / `{"message":"Token is wrong."}`

## POST /profile (Token nötig, nur Rolle USER)

Request:

```json
{
  "personal": {
    "age": 23,
    "gender": "male",
    "height": 180,
    "weight": 78.5
  },
  "training": {
    "goal": "health",
    "fitnessLevel": "beginner",
    "trainingDays": ["MON", "WED", "FRI"]
  }
}
```

Regeln:

- `age`: 16–99 (ganze Zahl).
- `height`: 120–230 (ganze Zahl).
- `weight`: 30–250 (max. 1 Nachkommastelle).
- `gender`: `male` | `female` | `diverse`.
- `goal`: `muscle_gain` | `fat_loss` | `health`.
- `fitnessLevel`: `beginner` | `advanced`.
- `trainingDays`: genau 3 Tage aus `MON`–`SUN`, mit je mindestens einem Ruhetag zwischen Trainingstagen. Auch `SUN` und `MON` gelten als benachbart.

Antwort bei Erfolg (`200 ProfileResponse`):

```json
{
  "profileId": 1,
  "personal": {
    "...": "..."
  },
  "training": {
    "...": "..."
  },
  "createdAt": "2026-10-17",
  "updatedAt": "2026-10-17",
  "planExists": false
}
```

Fehler (`400`):

- `Invalid input.` (mit `errors`).
- `Profile already exists.`
- `Access denied.` (ADMIN).
- `Token is wrong.`

## Sprint 3 – Trainingsplan

### POST /plans (Token nötig, nur Rolle USER, kein Body)

Liest das eigene Profil und erstellt den Plan ab Montag der aktuellen Woche (Europe/Berlin).

Wiederholte Aufrufe derselben Woche liefern dieselbe planId.

- 200: `{"message":"Plan created successfully.","planId":1,"weekStart":"2026-10-12"}`
- 400: `Profile not found.`, `Exercises could not be loaded.`, `Week change follows in Sprint 4.`, `Token is wrong.`, `Access denied.`

### GET /plans/current (Token nötig, nur Rolle USER)

- 200:

```json
{
  "planId": 1,
  "weekStart": "2026-10-12",
  "weekEnd": "2026-10-18",
  "today": "2026-10-15",
  "exerciseDataAvailable": true,
  "sessions": [
    {
      "sessionId": 1,
      "label": "A",
      "date": "2026-10-12",
      "exercises": [
        {
          "planExerciseId": 1,
          "exerciseId": "abc123",
          "name": "...",
          "bodyPart": "chest",
          "targetMuscle": "...",
          "equipment": "dumbbell",
          "imageUrl": "...",
          "sets": 2,
          "reps": "12-15",
          "weight": 3.5,
          "bodyweight": false
        }
      ]
    }
  ]
}
```

`name`, `targetMuscle`, `equipment`, `imageUrl` kommen aus ExerciseDB und dürfen null sein.

`exerciseDataAvailable: false` = Plan vorhanden, nur Details fehlen.

- 400: `No plan found.`, `Token is wrong.`, `Access denied.`

## Sprint 4 – Markierungen

### POST /marks  (Token nötig, nur Rolle USER)

```json
{"sessionId":101,"planExerciseId":501,"done":true}
```

Regeln (KI-Log Nr. 32): done Pflicht (true/false). Gewicht und Wiederholungen werden nicht mehr erfasst;
von älteren Clients mitgeschickte weight/reps werden ignoriert.
Bearbeitbar vom Trainingstag bis Sonntag dieser Woche (Backenddatum Europe/Berlin).
Korrektur derselben Planübung behält die id.
- 200: `{"id":1,"date":"2026-10-23","done":true,"weight":null,"reps":0,"automatic":false}`
  (weight/reps bleiben aus Kompatibilitätsgründen im Format, sind immer null bzw. 0)
- 400: `Invalid training values.`, `Invalid input.`, `Exercise not found.`, `Session is not editable.`,
  `Profile not found.`, `Token is wrong.`, `Access denied.`

### GET /plans/current – Ergänzung

Session zusätzlich: `status` (OPEN|IN_PROGRESS|DONE|FAILED|PARTIAL), `available`, `carried`.
Exercise zusätzlich: `mark` (wie Antwort von POST /marks) oder null.
weight/reps der Übung sind die Vorgaben; mark enthält nur done (mark.weight/mark.reps veraltet, null bzw. 0).

### Wochenwechsel

GET /plans/current und POST /plans schließen zuerst veraltete Wochen ab und kopieren sie:
- teilweise markierte Einheiten: offene Übungen -> automatisch nicht geschafft
  (automatic=true, reps=0, weight=null, Datum = Sonntag der alten Woche)
- ganz unmarkierte Einheiten: keine Fehlmarkierungen, Kopie mit carried=true
- Kopie: gleiche Übungen, Sätze und Wiederholungen, neue IDs, keine Markierungen
- Progression (KI-Log Nr. 31): hat eine Einheit 4 oder 5 von 5 Übungen mit done=true,
  steigt das Gewicht jeder Übung dieser Einheit um 5 % (auf 0,5 kg gerundet, max. 500 kg);
  Körpergewicht bleibt null. Bei 3 oder weniger done=true unverändert.
  Unmarkiert und done=false (auch automatisch) zählen gleich. Nach fälliger Neuplanung keine Progression.
  Zusätzlich läuft montags 00:05 (Europe/Berlin) ein Job, solange das Backend läuft.

## Sprint 5 – Profil bearbeiten

### PUT /profile  (Token nötig, nur Rolle USER)

Gleicher Body und gleiche Regeln wie POST /profile. Holt vorher verpasste Wochen mit dem alten Profil nach.
Speichert sofort; die aktuelle Woche bleibt unverändert.
Relevante Änderungen (Alter, Geschlecht, Größe, Gewicht, Ziel, Level, Trainingstage) -> neuer Plan ab nächstem Montag.
Gleiche Werte -> keine Neuplanung. Ohne bisherigen Plan -> keine Vormerkung.
Startgewicht (KI-Log Nr. 33): zusätzlich × Geschlechtsfaktor (male 1,2 · diverse 1,1 · female 1,0).
- 200: ProfileResponse inkl. `nextPlanFrom` (Datum oder null)
- 400: `Invalid input.` + errors, `Profile not found.`, `Access denied.`, `Token is wrong.`
  GET/POST /profile liefern ebenfalls `nextPlanFrom`.

### Administration

### Admin-JSON (Bearer-Token mit Rolle ADMIN)

- GET /admin/users -> [{userId, name, email, registeredAt, role}]
- GET /admin/stats -> {users, plans, marksLastSevenDays}
- DELETE /admin/users/{id} -> {"message":"User deleted.","userId":...}
- 400: `User not found.`, `Admin cannot be deleted.`, `Access denied.`, `Token is wrong.`

### Adminseiten (MPA, Thymeleaf, Session + CSRF)

- POST /admin/sso (Formularfeld `token`, Origin muss FRONTEND_URL sein) -> neue Session, Redirect /admin/overview
- GET /admin/overview, GET /admin/accounts
- GET /admin/accounts/{id}/delete (nur Bestätigung), POST /admin/accounts/{id}/delete (löscht, CSRF)
- POST /admin/logout (CSRF) -> Redirect FRONTEND_URL/login?loggedOut=1
- Nicht angemeldet -> Redirect FRONTEND_URL/login; falscher Origin/Rolle/CSRF -> 403
  Jedes JWT wird zusätzlich gegen die Datenbank geprüft (Konto vorhanden, Rolle passt).

## GraphQL

Die GraphQL-API ist unter `POST /graphql` erreichbar. Alle Anfragen müssen mit einem gültigen Bearer-Token authentifiziert werden.

**Request-Format:**

```http
POST /graphql
Authorization: Bearer <TOKEN>
Content-Type: application/json
```

```json
{
  "query": "..."
}
```

Das GraphQL-Schema ist definiert in:

`backend/src/main/resources/graphql/schema.graphqls`

### Berechtigungen

Die verfügbaren Queries und Mutations hängen von der Rolle des angemeldeten Nutzers ab.

| Rolle | Operationen | Beschreibung |
|---|---|---|
| USER | `myProfile` | Ruft das Profil des angemeldeten Nutzers ab. |
| USER | `currentPlan` | Ruft den aktuellen Trainingsplan des angemeldeten Nutzers ab. |
| ADMIN | `adminUsers` | Ruft die Nutzerübersicht ab. |
| ADMIN | `adminStats` | Ruft die administrativen Statistiken ab. |
| ADMIN | `deleteUser` | Löscht einen Nutzer. |

### USER-Queries

**Eigenes Profil abrufen:**

```graphql
query {
  myProfile {
    personal {
      age
      gender
      height
      weight
    }
    training {
      goal
      fitnessLevel
      trainingDays
    }
  }
}
```

**Aktuellen Trainingsplan abrufen:**

```graphql
query {
  currentPlan {
    planId
    weekStart
    weekEnd
    today
    exerciseDataAvailable
    sessions {
      sessionId
      label
      date
      status
      available
      carried
      exercises {
        planExerciseId
        exerciseId
        name
        bodyPart
        targetMuscle
        equipment
        imageUrl
        sets
        reps
        weight
        bodyweight
        mark {
          id
          date
          done
          weight
          reps
          automatic
        }
      }
    }
  }
}
```

`ExerciseMark.weight` und `ExerciseMark.reps` sind seit KI-Log Nr. 32 `@deprecated` (immer null bzw. 0)

Bei `myProfile` und `currentPlan` wird keine Nutzer-ID als Argument übergeben. Die Identität des Nutzers wird ausschließlich aus dem JWT des Bearer-Tokens ermittelt. Dadurch kann ein Nutzer über diese persönlichen Queries nur auf seine eigenen Daten zugreifen.

Die Profil- und Trainingsplan-Queries sind im API-Vertrag lesend. Beim Abruf von `currentPlan` wird intern derselbe Wochenwechsel-Nachholschritt ausgeführt wie bei REST `GET /plans/current`. Dadurch wird sichergestellt, dass der zurückgegebene Trainingsplan dem aktuellen Wochenstand entspricht.

Änderungen am Profil sowie das Setzen und Ändern von Übungsmarkierungen erfolgen weiterhin über die bestehenden REST-Endpunkte. Dafür werden keine GraphQL-Mutations bereitgestellt.

### ADMIN-Queries und -Mutations

**Nutzerübersicht und Statistiken abrufen:**

```graphql
query {
  adminStats {
    users
    plans
    marksLastSevenDays
  }
  adminUsers {
    userId
    name
    email
    registeredAt
    role
  }
}
```

**Nutzer löschen:**

```graphql
mutation {
  deleteUser(id: "42") {
    message
    userId
  }
}
```

Diese Operationen sind ausschließlich für angemeldete Nutzer mit der Rolle `ADMIN` vorgesehen.

### Fehlerbehandlung und Authentifizierung

**Fehlendes oder ungültiges Token**

Fehlt das Bearer-Token oder ist es ungültig, wird die Anfrage bereits durch die Authentifizierung abgewiesen.

Die API antwortet in diesem Fall mit HTTP `400 Bad Request` und der vorhandenen JSON-Fehlerstruktur:

```json
{
  "message": "Token is wrong."
}
```

**Unzureichende Berechtigung**

Wird eine GraphQL-Operation mit einer gültigen Anmeldung, aber einer unzureichenden Rolle aufgerufen, wird der Zugriff innerhalb des Resolver-Aufrufs durch die Rollenprüfung verweigert.

Beispiel: Ein angemeldeter Nutzer mit der Rolle `USER` versucht, die Admin-Query `adminStats` aufzurufen.

Die GraphQL-Antwort enthält ein nicht leeres `errors`-Array. Die angeforderten Admin-Daten werden nicht zurückgegeben. Das Feld `data` fehlt, ist `null` oder enthält für `adminStats` den Wert `null`.

Der Integrationstest `userTokenIsRejected` prüft dieses Verhalten. Ein bestimmter HTTP-Status oder ein konkreter GraphQL-Fehlercode wird durch diesen Test nicht festgelegt.

**Fachliche Fehler**

Fachliche Fehler, die als `ApiException` auftreten, werden durch den `GraphQlErrorResolver` in GraphQL-Fehler mit der Kategorie `BAD_REQUEST` übersetzt.

Beispiel: Wird `currentPlan` ohne vorhandenen Trainingsplan aufgerufen, kann der Fehler `No plan found.` auftreten.

Da das Root-Feld der Query nicht-null sein kann, kann GraphQL in diesem Fall zusätzlich `data: null` zurückgeben. Der Fehler selbst steht im `errors`-Array.

### Introspection und GraphiQL

GraphQL-Introspection ist für authentifizierte Nutzer möglich. Die verfügbaren Operationen und deren Berechtigungen werden weiterhin serverseitig geprüft.

Die GraphiQL-Browseroberfläche wird im Deployment nicht aktiviert. Der Zugriff auf die API erfolgt über `POST /graphql`, beispielsweise mit einem HTTP-Client oder über die Anwendung.