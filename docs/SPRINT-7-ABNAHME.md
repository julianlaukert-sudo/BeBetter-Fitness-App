
## Gezielte neue Faelle
S7-01: Retry-After als Sekunden: 180 Sekunden, vorher keine neue externe Anfrage.
S7-02: Retry-After als HTTP-Datum: bis zu diesem Zeitpunkt gesperrt.
S7-03: Fehlender/ungueltiger Header: bisherige Mindestpause 60 Sekunden bleibt.
S7-04: Keine Ausweichadresse wird nach 429 sofort angefragt; auch andere IDs warten.
S7-05: Bereits frisch gecachte Details bleiben ohne neue Anfrage nutzbar.
S7-06: Genau am Ablauf der Pause kann ein neuer normaler Aufruf wieder anfragen.
S7-07: Fehlerhinweis im Training nennt keine garantierte Ein-Minuten-Erholung.
S7-08: Bei fehlenden Details bleiben Vorgaben sichtbar; Markierungen bleiben gespeichert.
S7-09: Manueller Live-Test nutzt dieselbe freigegebene Quelle wie die Zielumgebung.
S7-10: Neues Testkonto erhaelt im Browser einen ersten Plan aus echten Uebungsdaten.
S7-11: Neustart, erneuter Login, gleiche Plan-/Markierungs-ID und gleiche gespeicherte Werte.
S7-12: Profil und Adminfunktion bleiben auf beiden Macs und auf Windows bedienbar.
S7-01 bis S7-06 sind mit kontrollierten Antworten reproduzierbare lokale Tests.
Diese Ergebnisse ersetzen S7-09 und S7-10 mit echter Quelle nicht.
S7-07 und S7-08 zusaetzlich im Browser pruefen. Fuer eine simulierte Stoerung nur eine
getrennte Testumgebung verwenden, niemals fremde Daten oder die produktive Quelle manipulieren.
## Gesamte Regression
Alle weiterhin anwendbaren Faelle A01-A25 aus docs/sprint-6-abnahme.md erneut abhaken.
Insbesondere: Registrierung, Passwortregeln, Profil, drei Trainingstage, 15 Uebungen,
Markieren/Korrigieren, keine Zukunftsmarkierung, Wochenende, Vormerkung, Adminrolle,
CSRF, Loeschkaskade und gesperrte JWTs eines geloeschten Kontos.
Kein Kalender, keine Prozentanpassung, kein neues Fachfeature dazu erfinden.
## Fehlerblatt
Fall / Zeitpunkt / Umgebung / beobachtet / erwartet: offen
Zustaendige Person / Korrekturcommit / erneute Pruefung: offen
Ein fehlgeschlagener oder nicht gepruefter Fall bleibt offen.
## Freigabe
[ ] Normale Backend- und Frontendtests sowie Build und statischer Start erfolgreich.
[ ] Manueller Live-Test und erster echter Browserplan erfolgreich.
[ ] Regression und drei Arbeitsplaetze tatsaechlich nachgewiesen.
[ ] Neustart mit erhaltenen Daten nachgewiesen.
[ ] Falls veroeffentlicht: beide Apps, SHA, Releases und Backup dokumentiert.
[ ] Alle drei Personen stimmen zu; keine offene Stoerung des ersten Plans.