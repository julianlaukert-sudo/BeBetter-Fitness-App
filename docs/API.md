
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
