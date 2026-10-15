# API-Dokumentation

## POST /users

Registriert einen neuen Nutzer.

**Request:**

```json
{
  "name": "...",
  "email": "...",
  "password": "..."
}
```

**Validierung:**
- `name`: Pflichtfeld, 1–50 Zeichen
- `email`: Pflichtfeld, gültige E-Mail-Adresse, maximal 255 Zeichen und eindeutig. Bei bereits vorhandener E-Mail-Adresse: HTTP 409 Conflict.
- `password`: Regeln gemäß PasswordPolicy: 8–64 Zeichen, Großbuchstabe, Zahl und Sonderzeichen, maximal 72 Bytes.