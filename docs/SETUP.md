# Lokales Setup

## Voraussetzungen
- Git, JDK 21, Maven 3.9.x, Node 22, Docker Desktop (muss laufen)
- Repository nicht in iCloud/OneDrive ablegen

## 1. Datenbank starten (Hauptordner)
    docker compose up -d
    docker compose ps          # db: running (healthy)

Lokale Übungswerte: Datenbank, Benutzer und Passwort jeweils `bebetter`, Port 5432.

## 2. Backend starten (Ordner backend)
    mvn spring-boot:run

## 3. Frontend starten (Ordner frontend)
    npm ci
    npm run dev

## Datenbank stoppen
    docker compose down        # Daten bleiben erhalten