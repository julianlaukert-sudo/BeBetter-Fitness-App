# Sprint 5 – Profilbearbeitung, Administration (MPA) und GraphQL

Datum: noch einzutragen
Geprüfter Commit: noch einzutragen
Person / Betriebssystem: noch einzutragen

## Profil
- [ ] Profilwerte werden geladen und mit denselben Regeln wie beim Anlegen geprüft.
- [ ] Die laufende Planwoche und gespeicherte Istwerte bleiben unverändert.
- [ ] Ziel, Level, Tage und relevante Körperdaten werden ab nächstem Montag neu geplant.
- [ ] Gleiche Werte oder nur Geschlecht lösen keine neue Startgewichtsrechnung aus.
- [ ] Neue Planung ist wiederholbar; ExerciseDB-Fehler lassen Altplan und Vormerkung bestehen.
- [ ] Ohne bisherigen Plan verwendet die erste Erstellung das aktuelle Profil.

## Admin (MPA)
- [ ] Admin-Anlage mit privaten Umgebungsvariablen getestet; kein Passwort im Repository.
- [ ] Neustart setzt das vorhandene Adminpasswort nicht zurück.
- [ ] Login führt den Admin per POST in eine neue Backend-Session.
- [ ] USER und falscher Origin können keine Admin-Session anlegen.
- [ ] Übersicht, Nutzerliste, Löschbestätigung und Abmeldung funktionieren.
- [ ] Keine Löschung per GET oder ohne gültigen CSRF-Code.
- [ ] USER mit Profil, Plänen, Einheiten, Übungen und Markierungen wird vollständig gelöscht.
- [ ] Admin bleibt geschützt; Token eines gelöschten Nutzers wird abgewiesen.
- [ ] Statistik zählt USER, alle Planwochen und manuelle Markierungen heute bis vor sechs Tagen.

## GraphQL (Commit 7)
- [ ] Query adminUsers und adminStats liefern Daten nur mit ADMIN-Token.
- [ ] Mutation deleteUser löscht ein Testkonto; Admin-Konto bleibt geschützt.
- [ ] USER-Token und fehlendes Token werden abgewiesen.

## Gemeinsame Prüfung
    mvn -B -f backend/pom.xml clean verify
    cd frontend
    npm ci
    npm test
    npm run build
    cd ..

- [ ] CI grün, alle Reviews erfolgt; Browserablauf auf Mac und Windows selbst geprüft.
- [ ] Wirkliche Beobachtungen und Fehler dokumentiert. Nicht Geprüftes bleibt offen.

## Grenzen
Kein Kalender, keine prozentuale Leistungsanpassung, kein PB-15 und kein Deployment in Sprint 5.
Der neue Startvorschlag entsteht durch Profiländerung, nicht aus Trainingsergebnissen.
ExerciseDB kann eine vorgemerkte Neuplanung blockieren; nach Behebung erneut laden.
Admin-Session und Token-Sperrliste liegen im laufenden Backendprozess (nach Neustart neu anmelden).

