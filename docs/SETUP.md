# Lokales Setup

## Voraussetzungen
- Git, JDK 21, Maven 3.9.x, Node 22, Docker Desktop (muss laufen)
- Repository nicht in iCloud/OneDrive ablegen

## 1. Datenbank starten (Hauptordner)
    docker compose up -d
    docker compose ps          # db: running (healthy)

Lokale Übungswerte: Datenbank, Benutzer und Passwort jeweils `bebetter`, Port 5433 (im Container 5432).

## 2. Backend starten (Ordner backend)
    mvn spring-boot:run

## 3. Frontend starten (Ordner frontend)
    npm ci
    npm run dev

## Datenbank stoppen
    docker compose down        # Daten bleiben erhalten

## Hinweise für Windows

Getestet unter Windows 11 mit PowerShell im IntelliJ-Terminal.

- **Docker Desktop** benötigt WSL 2 und eingeschaltete Virtualisierung.
  Prüfen mit `wsl --status` → „Standardversion: 2“ ohne Fehlermeldung.
  Nach der Installation von WSL ist ein Neustart nötig.
- Docker Desktop muss laufen („Engine running“), bevor `docker compose up -d` funktioniert.
- **Repository nicht in OneDrive** ablegen (z. B. `C:\Users\<Name>\Projekte`).
- **curl** in PowerShell immer als `curl.exe` aufrufen:
  `curl.exe -i http://localhost:8080/health`
- Beim ersten Backend-Start kann ein **Firewall-Fenster** erscheinen. „Abbrechen“ genügt,
  lokal funktioniert alles trotzdem.
- Ein laufendes Backend/Frontend im Terminal mit **Ctrl + C** beenden
  (bei „Batchvorgang abbrechen (J/N)?“ mit `J` bestätigen).

## Häufige Probleme

- **„Connection to localhost:5433 refused“**: Die Datenbank läuft nicht.
  Im Hauptordner `docker compose up -d` ausführen.
- **„Migration checksum mismatch for migration version 1“**: Die lokale Datenbank wurde
  mit einer veränderten Migration angelegt. Lokal zurücksetzen mit `docker compose down -v`
  und danach `docker compose up -d` (löscht nur lokale Übungsdaten).
  Bereits gemergte Migrationen nie ändern.