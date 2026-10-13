# Lokales Setup

## Voraussetzungen
Git, JDK 21, Maven 3.9.x, Node 22, Docker Desktop (muss laufen).
Repository nicht in Cloud-Synchronisationsordnern (iCloud/OneDrive) ablegen.

## Datenbank starten (Hauptordner)
```bash
docker compose up -d
docker compose ps          # db: running (healthy)