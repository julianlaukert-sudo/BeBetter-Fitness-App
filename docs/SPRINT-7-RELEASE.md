# Sprint 7 - Nachbesserung veroeffentlichen
Dieser Sprint ist eine Fortsetzung nach Sprint 6, kein urspruenglich eingeplanter Feature-Sprint.
Noch keine Veroeffentlichung oder erfolgreiche Onlineabnahme behaupten.
Keine neue Datenbankmigration, keine neue Bibliothek und keine neuen Funktionen einplanen.
## Wann ein Release sinnvoll ist
Ist der Abschlussrelease aus Sprint 6 noch nicht erfolgt, diesen geprueften Stand als
Kandidat fuer den bereits geplanten Abschlussrelease verwenden.
Ist Sprint 6 bereits deployed, nur eine gemeinsame Nachbesserung veroeffentlichen.
Nicht nach jedem der sechs Arbeitscommits deployen. Automatische Deployments bleiben aus.
Die vorhandene Anleitung docs/RELEASE.md und das Sprint-6-Backupverfahren weiter verwenden.
## Vorher
- Alle sechs Branches geprueft und nacheinander gemergt; CI fuer Kandidat-SHA gruen.
- Echte Datenquelle und Berechtigung geklaert, Wartezeit abgelaufen.
- Manueller Live-Test fuer diese Quelle erfolgreich; sonst keine Fertigmeldung.
- Beide HTTPS-Origins, die bisherigen Release-IDs und PostgreSQL-Migrationen notiert.
- Erfolgreiches aktuelles Backup vor Veroeffentlichung bestaetigt.
- EXERCISEDB_URLS nur nach bewusster Betreiber-/Vertragspruefung aendern.
- BEBETTER_LIVE_URL ist nur die Testvariable und aendert die Backendkonfiguration nicht.
## Durchfuehren
1. main nach git pull --ff-only einfrieren und vollen git rev-parse HEAD notieren.
2. Backend manuell aus genau diesem Stand deployen. Logs und /health/database pruefen.
3. Frontend manuell aus demselben Stand deployen und Build/Start pruefen.
4. Echten neuen Browserplan, Profil, Markierung, Korrektur und Adminablauf durchspielen.
5. Backend neu starten und nach erneutem Login dieselben gespeicherten IDs vergleichen.
6. Beide Source-SHAs und Release-IDs sowie Nachweise im Uebergabeblatt festhalten.
7. Tatsaechliche Ergebnisse danach in eigenem Dokumentationscommit ergaenzen.
   Flyway bleibt auf V1-V7. Keine V8 anlegen oder vorhandene Migration umschreiben.
   Ein App-Rollback stellt PostgreSQL nicht zurueck. Keine automatische Datenbankwiederherstellung.
   Ein Restart ist nur die Persistenzprobe, keine Methode zum Umgehen einer Anbietersperre.
   Ein gruener Healthcheck beweist keinen funktionsfaehigen ExerciseDB-Abruf.
## Wenn die Quelle weiter nicht verfuegbar ist
Ergebnis, Zeitpunkt und naechste zustaendige Person offen dokumentieren.
Keine Domainrotation, keine Demo-Uebungen als echte Daten und keine Endlosschleife einbauen.
Fuer eine andere Quelle mit neuem Datenvertrag erst einen eigenen Arbeitsauftrag festlegen.
Eine lokale Vorfuehrung mit kontrollierten Testdaten ausdruecklich so benennen.
Der Projektabschluss fuer den echten Erstplan bleibt bis zum Nachweis offen.