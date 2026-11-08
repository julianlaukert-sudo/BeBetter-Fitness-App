# API Design – BeBetter Fitness Web App

**Team:** BeBetter  
**Projekt:** BeBetter – Fitness Web App  
**API-Version:** Sprint 2–5  
**Datenformat:** JSON (REST und GraphQL)

---

## 1. Health

### Endpoint: `/health`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| GET | `/health` | Prüft, ob das Backend erreichbar ist. | Kein Request Body | `{"status":"ok","message":"BeBetter backend is reachable"}` |

**Response – HTTP 200**

```json
{
  "status": "ok",
  "message": "BeBetter backend is reachable"
}
```

---

## 2. Benutzerregistrierung und Authentifizierung

### Endpoint: `/users`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| POST | `/users` | Registriert einen neuen Benutzer. | `{"name":"Julian","email":"julian@example.com","password":"Password123!"}` | `{"message":"User created successfully.","userId":1,"name":"Julian","email":"julian@example.com","role":"USER"}` |

**Request – HTTP POST**

```json
{
  "name": "Julian",
  "email": "julian@example.com",
  "password": "Password123!"
}
```

**Response – HTTP 200**

```json
{
  "message": "User created successfully.",
  "userId": 1,
  "name": "Julian",
  "email": "julian@example.com",
  "role": "USER"
}
```

**Validierungsregeln:**
- Name: 2–50 Zeichen.
- E-Mail: gültiges Format, maximal 255 Zeichen und eindeutig.
- Groß- und Kleinschreibung der E-Mail werden bei der Eindeutigkeitsprüfung ignoriert.
- Passwort: 8–64 Zeichen, maximal 72 UTF-8-Bytes.
- Das Passwort muss mindestens einen Großbuchstaben, eine Zahl und ein Sonderzeichen enthalten.

### Endpoint: `/auth`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| POST | `/auth` | Meldet einen registrierten Benutzer an und erstellt ein JWT. | `{"email":"julian@example.com","password":"Password123!"}` | `{"token":"<JWT>","tokenType":"Bearer","expiresIn":3600,"userId":1,"name":"Julian","role":"USER","profileCompleted":true}` |
| DELETE | `/auth` | Meldet einen Benutzer ab und sperrt das verwendete Token bis zu dessen Ablauf. | Authorization: Bearer Token | `{"message":"User logged off."}` |

**POST /auth – Request**

```json
{
  "email": "julian@example.com",
  "password": "Password123!"
}
```

**POST /auth – Response**

```json
{
  "token": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 1,
  "name": "Julian",
  "role": "USER",
  "profileCompleted": true
}
```

**DELETE /auth – Response**

```json
{
  "message": "User logged off."
}
```

**Authentifizierung:**

- Registrierung und Login sind öffentlich zugänglich.
- Nach erfolgreichem Login wird ein JWT (JSON Web Token) erzeugt.
- Das Token ist 3600 Sekunden (eine Stunde) gültig.
- Geschützte REST-Endpunkte benötigen das Token im HTTP-Authorization-Header.
- Beim Logout wird genau das verwendete Token bis zum Ablauf gesperrt.

Beispiel:

```http
Authorization: Bearer <JWT>
```

---

## 3. Benutzerprofil

### Endpoint: `/profile`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| GET | `/profile` | Ruft das eigene Benutzerprofil ab. | Authorization: Bearer Token | `ProfileResponse` als JSON |
| POST | `/profile` | Erstellt ein neues Benutzerprofil. | JSON mit `personal` und `training` | `ProfileResponse` als JSON |
| PUT | `/profile` | Aktualisiert das bestehende Benutzerprofil. | JSON mit `personal` und `training` | `ProfileResponse` mit `nextPlanFrom` |

**POST /profile – Request**

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
    "trainingDays": [
      "MON",
      "WED",
      "FRI"
    ]
  }
}
```

**GET /profile – Response (ProfileResponse)**

```json
{
  "profileId": 1,
  "personal": {
    "age": 23,
    "gender": "male",
    "height": 180,
    "weight": 78.5
  },
  "training": {
    "goal": "health",
    "fitnessLevel": "beginner",
    "trainingDays": [
      "MON",
      "WED",
      "FRI"
    ]
  },
  "createdAt": "2026-10-17",
  "updatedAt": "2026-10-17",
  "planExists": false,
  "nextPlanFrom": null
}
```

**PUT /profile – Response**

Die Antwort verwendet dieselbe ProfileResponse-Struktur. Zusätzlich enthält sie das Feld `nextPlanFrom`, das das Datum einer gegebenenfalls vorgemerkten Neuplanung angibt. Ohne vorgemerkte Neuplanung ist der Wert `null`.

**Beschreibung der Datenfelder:**

| Bereich | Feld | Bedeutung |
|---|---|---|
| Personal | age | Alter des Benutzers |
| Personal | gender | Geschlecht |
| Personal | height | Körpergröße in cm |
| Personal | weight | Körpergewicht in kg |
| Training | goal | Trainingsziel |
| Training | fitnessLevel | Trainingsniveau |
| Training | trainingDays | Gewählte Trainingstage |
| Response | profileId | Eindeutige Profil-ID |
| Response | createdAt | Erstellungsdatum |
| Response | updatedAt | Datum der letzten Änderung |
| Response | planExists | Gibt an, ob bereits ein Trainingsplan existiert |
| Response | nextPlanFrom | Datum einer vorgemerkten Neuplanung oder `null` |

**Validierungsregeln:**

- Alter: 16–99 Jahre.
- Größe: 120–230 cm.
- Gewicht: 30–250 kg, maximal eine Nachkommastelle.
- Geschlecht: `male`, `female` oder `diverse`.
- Trainingsziel: `muscle_gain`, `fat_loss` oder `health`.
- Fitnesslevel: `beginner` oder `advanced`.
- Es müssen genau drei Trainingstage ausgewählt werden.
- Zwischen den Trainingstagen muss mindestens ein Ruhetag liegen. Sonntag und Montag gelten ebenfalls als benachbarte Tage.

**Berechtigungen:**
- GET, POST und PUT benötigen ein gültiges Bearer Token.
- Die Endpunkte sind ausschließlich für Benutzer mit der Rolle `USER` vorgesehen.
- Die Benutzer-ID wird automatisch aus dem JWT ermittelt.
- Ein Benutzer kann nur sein eigenes Profil abrufen und bearbeiten.

---

## 4. Trainingsplan

### Endpoint: `/plans`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| POST | `/plans` | Erstellt einen individuellen Trainingsplan auf Grundlage des Benutzerprofils. | Authorization: Bearer Token, kein Request Body | `{"message":"Plan created successfully.","planId":1,"weekStart":"2026-10-12"}` |

**Request:**

Kein JSON-Request-Body erforderlich. Die Benutzerinformationen werden anhand des Bearer Tokens und des gespeicherten Profils ermittelt.

**Response – HTTP 200**

```json
{
  "message": "Plan created successfully.",
  "planId": 1,
  "weekStart": "2026-10-12"
}
```

**Funktionsweise:**
- Der Trainingsplan wird ab Montag der aktuellen Woche erstellt.
- Die Zeitzone ist `Europe/Berlin`.
- Wiederholte Aufrufe innerhalb derselben Woche liefern dieselbe `planId`.
- Voraussetzung ist ein bereits angelegtes Benutzerprofil.

### Endpoint: `/plans/current`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| GET | `/plans/current` | Ruft den aktuellen wöchentlichen Trainingsplan mit Trainingseinheiten und Übungen ab. | Authorization: Bearer Token | `PlanResponse` mit Sessions und Exercises als JSON |

**Response – HTTP 200 (PlanResponse)**

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
      "status": "OPEN",
      "available": true,
      "carried": false,
      "exercises": [
        {
          "planExerciseId": 1,
          "exerciseId": "abc123",
          "name": "Example Exercise",
          "bodyPart": "chest",
          "targetMuscle": "pectorals",
          "equipment": "dumbbell",
          "imageUrl": "https://example.com/exercise.jpg",
          "sets": 2,
          "reps": "12-15",
          "weight": 3.5,
          "bodyweight": false,
          "mark": null
        }
      ]
    }
  ]
}
```

**Beschreibung der Response-Felder:**

| Feld | Bedeutung |
|---|---|
| planId | Eindeutige ID des Trainingsplans |
| weekStart | Beginn der Trainingswoche |
| weekEnd | Ende der Trainingswoche |
| today | Aktuelles Datum |
| exerciseDataAvailable | Gibt an, ob Übungsdetails verfügbar sind |
| sessions | Liste der Trainingseinheiten |
| sessionId | Eindeutige ID einer Trainingseinheit |
| label | Bezeichnung der Trainingseinheit |
| date | Geplantes Trainingsdatum |
| status | Status der Trainingseinheit |
| available | Gibt an, ob die Trainingseinheit verfügbar ist |
| carried | Gibt an, ob die Einheit aus einer vorherigen Woche übernommen wurde |
| exercises | Liste der Übungen |
| planExerciseId | Eindeutige ID der Planübung |
| exerciseId | ID der Übung aus ExerciseDB |
| name | Name der Übung |
| bodyPart | Trainierter Körperbereich |
| targetMuscle | Zielmuskel |
| equipment | Benötigtes Trainingsgerät |
| imageUrl | Bild-URL der Übung |
| sets | Anzahl der Sätze |
| reps | Vorgesehene Wiederholungen |
| weight | Vorgesehenes Gewicht |
| bodyweight | Gibt an, ob mit dem eigenen Körpergewicht trainiert wird |
| mark | Zugehörige Trainingsmarkierung oder `null` |

**Mögliche Session-Statuswerte:**

- `OPEN`
- `IN_PROGRESS`
- `DONE`
- `FAILED`
- `PARTIAL`

**Zusätzliche Informationen:**

- Übungsdetails wie Name, Zielmuskel, Equipment und Bild-URL werden aus ExerciseDB geladen.
- Diese Felder können `null` sein.
- `exerciseDataAvailable: false` bedeutet, dass der Trainingsplan existiert, aber die Übungsdetails nicht verfügbar sind.
- Die Felder `weight` und `reps` innerhalb einer Exercise beschreiben die Trainingsvorgaben.
- Das Feld `mark` enthält die tatsächliche Erledigungsmarkierung.

---

## 5. Trainingsfortschritt und Markierungen

### Endpoint: `/marks`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| POST | `/marks` | Markiert eine Übung als erledigt oder nicht erledigt und speichert den Trainingsfortschritt. | `{"sessionId":101,"planExerciseId":501,"done":true}` | `{"id":1,"date":"2026-10-23","done":true,"weight":null,"reps":0,"automatic":false}` |

**Request – HTTP POST**

```json
{
  "sessionId": 101,
  "planExerciseId": 501,
  "done": true
}
```

**Response – HTTP 200**

```json
{
  "id": 1,
  "date": "2026-10-23",
  "done": true,
  "weight": null,
  "reps": 0,
  "automatic": false
}
```

**Beschreibung der Response-Felder:**

| Feld | Bedeutung |
|---|---|
| id | Eindeutige ID der Markierung |
| date | Datum der Markierung |
| done | Gibt an, ob die Übung erledigt wurde |
| weight | Veraltet, bleibt aus Kompatibilitätsgründen `null` |
| reps | Veraltet, bleibt aus Kompatibilitätsgründen `0` |
| automatic | Gibt an, ob die Markierung automatisch erstellt wurde |

**Regeln und Berechtigungen:**

- `done` ist ein Pflichtfeld und muss `true` oder `false` sein.
- Gewicht und Wiederholungen werden nicht mehr über diesen Endpunkt erfasst.
- Ältere Clients können `weight` und `reps` mitsenden; diese Werte werden ignoriert.
- Markierungen können vom Trainingstag bis zum Sonntag derselben Woche bearbeitet werden.
- Die Berechnung erfolgt anhand des Backenddatums in `Europe/Berlin`.
- Wird dieselbe Planübung erneut markiert, bleibt die bestehende Markierungs-ID erhalten.
- Der Endpunkt benötigt ein gültiges Bearer Token und die Rolle `USER`.

### Automatischer Wochenwechsel

Die API verarbeitet veraltete Trainingswochen automatisch.

**Regeln:**

- Teilweise markierte Trainingseinheiten: Offene Übungen werden automatisch als nicht geschafft markiert.
- Vollständig unmarkierte Einheiten: Sie werden ohne automatische Fehlmarkierungen in die nächste Woche übernommen.
- Übernommene Einheiten erhalten `carried: true`.
- Die übernommenen Übungen behalten ihre Vorgaben, erhalten aber neue IDs und keine Markierungen.

**Trainingsprogression:**

- Werden vier oder fünf von fünf Übungen einer Trainingseinheit erfolgreich erledigt, erhöht sich das Gewicht aller Übungen dieser Einheit um 5 %.
- Das neue Gewicht wird auf 0,5 kg gerundet.
- Das maximale Gewicht beträgt 500 kg.
- Körpergewichtsübungen behalten `weight: null`.
- Bei drei oder weniger erfolgreich erledigten Übungen bleibt das Gewicht unverändert.
- Automatisch nicht erledigte Übungen zählen ebenfalls als nicht erledigt.

Zusätzlich läuft montags um 00:05 Uhr (Europe/Berlin) ein Hintergrundjob, solange das Backend aktiv ist.

---

## 6. Administration – JSON-API

Die Admin-JSON-Endpunkte sind ausschließlich für angemeldete Benutzer mit der Rolle `ADMIN` zugänglich.

### Endpoint: `/admin/users`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| GET | `/admin/users` | Ruft eine Übersicht aller registrierten Benutzer ab. | ADMIN Bearer Token, kein Request Body | JSON-Array mit `userId`, `name`, `email`, `registeredAt` und `role` |
| DELETE | `/admin/users/{id}` | Löscht einen bestimmten Benutzer anhand seiner ID. | ADMIN Bearer Token, Benutzer-ID in der URI | `{"message":"User deleted.","userId":42}` |

**GET /admin/users – Response**

```json
[
  {
    "userId": 1,
    "name": "Julian",
    "email": "julian@example.com",
    "registeredAt": "2026-10-17",
    "role": "USER"
  },
  {
    "userId": 2,
    "name": "Andrii",
    "email": "andrii@example.com",
    "registeredAt": "2026-10-18",
    "role": "USER"
  }
]
```

**DELETE /admin/users/{id} – Beispiel**

Request:

```http
DELETE /admin/users/42
Authorization: Bearer <ADMIN-JWT>
```

Response:

```json
{
  "message": "User deleted.",
  "userId": 42
}
```

### Endpoint: `/admin/stats`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| GET | `/admin/stats` | Ruft administrative Statistiken der Anwendung ab. | ADMIN Bearer Token, kein Request Body | `{"users":25,"plans":18,"marksLastSevenDays":120}` |

**Response – HTTP 200**

```json
{
  "users": 25,
  "plans": 18,
  "marksLastSevenDays": 120
}
```

Die Zahlen sind beispielhafte Werte.

**Berechtigungen:**

- Alle drei Endpunkte benötigen ein gültiges JWT mit der Rolle `ADMIN`.
- Benutzer mit der Rolle `USER` haben keinen Zugriff.
- Beim Löschen eines Benutzers wird die angegebene Benutzer-ID über den URI-Pfad übergeben.
- Ein Administrator kann nicht gelöscht werden.

---

## 7. GraphQL-API

### Endpoint: `/graphql`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| POST | `/graphql` | Ermöglicht den Zugriff auf Profil-, Trainingsplan- und administrative Daten über GraphQL. | JSON mit `query` oder `mutation` sowie Bearer Token | JSON mit `data` und gegebenenfalls `errors` |

**HTTP-Request**

```http
POST /graphql
Authorization: Bearer <JWT>
Content-Type: application/json
```

**Request-Format**

```json
{
  "query": "..."
}
```

Das GraphQL-Schema befindet sich unter:

`backend/src/main/resources/graphql/schema.graphqls`

### 7.1 USER-Queries

Normale Benutzer können ausschließlich ihre eigenen Profildaten und ihren Trainingsplan abrufen.

**Query: myProfile**

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

**Beispiel für die Response**

```json
{
  "data": {
    "myProfile": {
      "personal": {
        "age": 23,
        "gender": "male",
        "height": 180,
        "weight": 78.5
      },
      "training": {
        "goal": "health",
        "fitnessLevel": "beginner",
        "trainingDays": [
          "MON",
          "WED",
          "FRI"
        ]
      }
    }
  }
}
```

**Query: currentPlan**

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

**Beschreibung:**

- `myProfile` ruft das eigene Benutzerprofil ab.
- `currentPlan` ruft den aktuellen Trainingsplan ab.
- Es wird keine Benutzer-ID als GraphQL-Argument übergeben.
- Die Identität wird ausschließlich anhand des JWT ermittelt.
- Beim Abruf von `currentPlan` wird auch die Wochenwechsel-Verarbeitung ausgeführt.

### 7.2 ADMIN-Queries und Mutations

Administratoren erhalten Zugriff auf zusätzliche Verwaltungsfunktionen.

**Query: adminStats und adminUsers**

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

**Beispiel für die Response**

```json
{
  "data": {
    "adminStats": {
      "users": 25,
      "plans": 18,
      "marksLastSevenDays": 120
    },
    "adminUsers": [
      {
        "userId": 1,
        "name": "Julian",
        "email": "julian@example.com",
        "registeredAt": "2026-10-17",
        "role": "USER"
      }
    ]
  }
}
```

**Mutation: deleteUser**

```graphql
mutation {
  deleteUser(id: "42") {
    message
    userId
  }
}
```

**Beispiel für die Response**

```json
{
  "data": {
    "deleteUser": {
      "message": "User deleted.",
      "userId": 42
    }
  }
}
```

**GraphQL-Berechtigungen:**

| Rolle | Operation | Beschreibung |
|---|---|---|
| USER | myProfile | Eigenes Profil abrufen |
| USER | currentPlan | Eigenen Trainingsplan abrufen |
| ADMIN | adminUsers | Alle Benutzer abrufen |
| ADMIN | adminStats | Administrative Statistiken abrufen |
| ADMIN | deleteUser | Benutzer löschen |

**Weitere Informationen:**

- GraphQL verwendet ausschließlich HTTP POST.
- Jede Anfrage benötigt ein gültiges Bearer Token.
- Die Berechtigungen werden serverseitig anhand der Benutzerrolle geprüft.
- Profiländerungen und Trainingsmarkierungen werden weiterhin über REST-Endpunkte ausgeführt.
- Es werden keine GraphQL-Mutations für Profiländerungen oder Trainingsmarkierungen bereitgestellt.
- GraphQL-Introspection ist für authentifizierte Nutzer möglich.
- GraphiQL ist im Deployment nicht aktiviert.

---

## 8. Admin-Webseiten (MPA)

Die Admin-Webseiten unterscheiden sich von der JSON-API. Sie verwenden serverseitig gerenderte HTML-Seiten mit Thymeleaf, Admin-Sessions und CSRF-Schutz.

### Endpoint: `/admin`

| Method | URI | Description | Data In | Data Out |
|---|---|---|---|---|
| POST | `/admin/sso` | Meldet einen Administrator über Single Sign-on an. | Formularfeld `token`, Origin-Header | Neue Admin-Session, Redirect |
| GET | `/admin/overview` | Zeigt die administrative Übersicht. | Admin-Session | HTML-Seite |
| GET | `/admin/accounts` | Zeigt die Benutzerverwaltung. | Admin-Session | HTML-Seite |
| GET | `/admin/accounts/{id}/delete` | Zeigt die Löschbestätigung eines Benutzers. | Admin-Session, Benutzer-ID | HTML-Bestätigungsseite |
| POST | `/admin/accounts/{id}/delete` | Löscht einen Benutzer nach Bestätigung. | Admin-Session, Benutzer-ID, CSRF-Token | Redirect |
| POST | `/admin/logout` | Meldet den Administrator ab. | Admin-Session, CSRF-Token | Redirect zur Login-Seite |

**Besonderheiten:**

- Die Anmeldung erfolgt über `/admin/sso`.
- Der übergebene Origin muss mit `FRONTEND_URL` übereinstimmen.
- Nach erfolgreicher Anmeldung wird eine Admin-Session erstellt.
- Die Session wird für den Zugriff auf die Admin-Webseiten verwendet.
- Änderungen und Löschaktionen benötigen einen gültigen CSRF-Token.
- Die Löschbestätigung über GET führt noch keine Löschung durch.
- Die eigentliche Löschung erfolgt erst über POST.
- Nicht angemeldete Benutzer werden zur Login-Seite des Frontends weitergeleitet.
- Falscher Origin, falsche Rolle oder ungültiger CSRF-Token führen zu HTTP 403.
- JWTs werden zusätzlich gegen die Datenbank geprüft.

Die Admin-Webseiten liefern HTML und Redirects statt der normalen JSON-Responses der REST-API.

---

## 9. Fehlerbehandlung und API-Konventionen

### Allgemeine HTTP-Konventionen

| HTTP-Status | Bedeutung |
|---|---|
| 200 OK | Anfrage erfolgreich |
| 400 Bad Request | Ungültige Eingabe, fehlendes oder ungültiges Token oder fachlicher Fehler |
| 403 Forbidden | Zugriff durch falsche Rolle, Origin oder CSRF-Schutz verweigert (insbesondere Admin-Webseiten) |

**Allgemeine Fehlerstruktur der REST-API**

```json
{
  "message": "Invalid input.",
  "errors": {
    "email": "Invalid email address."
  }
}
```

Das Feld `errors` wird nur bei Eingabefehlern ergänzt.

### Häufige Fehlermeldungen

| Fehlermeldung | Bedeutung |
|---|---|
| `Invalid input.` | Übergebene Daten sind ungültig. |
| `User already exists.` | Benutzer ist bereits registriert. |
| `Email or password is wrong.` | Anmeldedaten sind falsch. |
| `Token is wrong.` | Token fehlt, ist ungültig, abgelaufen oder gesperrt. |
| `Access denied.` | Benutzer besitzt nicht die erforderliche Rolle. |
| `Profile not found.` | Benutzerprofil wurde nicht gefunden. |
| `Profile already exists.` | Es existiert bereits ein Profil. |
| `No plan found.` | Es existiert kein Trainingsplan. |
| `Exercise not found.` | Übung wurde nicht gefunden. |
| `Session is not editable.` | Trainingseinheit kann nicht mehr bearbeitet werden. |

### Authentifizierung und Sicherheit

- REST-Endpunkte verwenden JWT-basierte Authentifizierung.
- Das JWT wird über `Authorization: Bearer <JWT>` übermittelt.
- Die Benutzer-ID wird aus dem Token ermittelt.
- Benutzerrollen sind `USER` und `ADMIN`.
- Administrative REST-Endpunkte benötigen die Rolle `ADMIN`.
- Die Admin-Webseiten verwenden Sessions und CSRF-Schutz.
- Passwörter werden nicht im Klartext gespeichert.
- Geschützte persönliche Daten können nur vom angemeldeten Benutzer selbst abgerufen werden, sofern keine administrative Berechtigung vorgesehen ist.

---

## 10. Zusammenfassung aller API-Endpunkte

| Bereich | Method | URI | Zugriff |
|---|---|---|---|
| Health | GET | `/health` | Öffentlich |
| Registrierung | POST | `/users` | Öffentlich |
| Login | POST | `/auth` | Öffentlich |
| Logout | DELETE | `/auth` | USER / ADMIN |
| Profil | GET | `/profile` | USER |
| Profil | POST | `/profile` | USER |
| Profil | PUT | `/profile` | USER |
| Trainingsplan | POST | `/plans` | USER |
| Aktueller Plan | GET | `/plans/current` | USER |
| Trainingsmarkierung | POST | `/marks` | USER |
| Benutzerverwaltung | GET | `/admin/users` | ADMIN |
| Benutzer löschen | DELETE | `/admin/users/{id}` | ADMIN |
| Statistiken | GET | `/admin/stats` | ADMIN |
| GraphQL | POST | `/graphql` | USER / ADMIN, je nach Operation |
| Admin SSO | POST | `/admin/sso` | SSO |
| Admin Übersicht | GET | `/admin/overview` | Admin-Session |
| Benutzerverwaltung (MPA) | GET | `/admin/accounts` | Admin-Session |
| Löschbestätigung | GET | `/admin/accounts/{id}/delete` | Admin-Session |
| Benutzer löschen (MPA) | POST | `/admin/accounts/{id}/delete` | Admin-Session + CSRF |
| Admin Logout | POST | `/admin/logout` | Admin-Session + CSRF |

**Hinweis:** Die JSON-Beispiele dienen der Dokumentation des API-Vertrags. Die konkreten Werte wie Namen, IDs, Datumsangaben und Statistiken sind Beispieldaten. Die tatsächlichen Antworten werden vom Backend erzeugt.