# API-Vertrag (Sprint 2)

Konvention (wie Referenz): Erfolg = HTTP 200. Fehler = HTTP 400 mit
`{ "message": "...", "errors": { "feld": "..." } }` (`errors` nur bei Eingabefehlern).
Geschützte Endpunkte brauchen `Authorization: Bearer <token>`. Die Nutzer-ID kommt immer aus dem Token.

## POST /users (öffentlich)
Request: `{ "name": "...", "email": "...", "password": "..." }`
Antwort: `{ "message": "User created successfully.", "userId", "name", "email", "role": "USER" }`
Regeln: name 2–50 Zeichen; email gültig, max. 255, eindeutig (Groß-/Kleinschreibung egal);
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

## GET /profile, POST /profile
Folgt in Commit 4 (siehe Sprint-2-Anleitung, Profilwerte und Fehler).