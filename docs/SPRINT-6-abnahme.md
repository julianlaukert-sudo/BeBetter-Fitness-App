# Sprint 6 - Gesamtabnahme

Jeden Fall als bestanden, fehlgeschlagen oder nicht geprueft dokumentieren.
Zu jedem Ergebnis kurze Beobachtung und bei Fehler eine zugeordnete Korrektur nennen.
## Pflichtfaelle
- A01 Frische Registrierung und Login; keine Adminrolle aus Registrierungsdaten.
- A02 Passwortgrenzen, falsches Passwort, ungueltige Eingaben und Logout.
- A03 Onboarding mit drei getrennten Tagen einschliesslich Sonntag/Montag-Regel.
- A04 Erster echter Plan mit drei Einheiten und fuenf Uebungen; 15 verschiedene IDs.
- A05 Koerperdaten beeinflussen Startvorschlaege innerhalb Modellgrenzen und Rundung.
- A06 Uebungsdetails beziehungsweise sichtbarer Hinweis bei fehlenden Details.
- A07 Faellige Uebung markieren, ausdrueckliches false und null Wiederholungen.
- A08 Korrektur behaelt die Markierungs-ID; Vorgabe bleibt unveraendert.
- A09 Zukunft, fremde Einheiten und geschlossene Wochen abweisen.
- A10 Teilmarkierung: nur offene Uebungen automatisch nicht geschafft.
- A11 Ganz unmarkierte Einheit bleibt ohne alte Markierung; carried in der Folgewoche.
- A12 Wiederholter und paralleler Wochenwechsel erzeugt keine Duplikate.
- A13 Profil aendern: aktuelle Woche bleibt, neue Vorgaben ab Folgemontag.
- A14 API-Fehler waehrend Neugenerierung behaelt Altplan und Vormerkung; Wiederholung.
- A15 Adminlogin wechselt in Session; USER und fremder Origin werden abgewiesen.
- A16 Adminliste und Zaehler; manuelle Ergebnisse heute bis vor sechs Tagen, ohne Automatik.
- A17 Loeschbestaetigung per GET loescht nichts; POST ohne CSRF wird abgewiesen.
- A18 Bewusst angelegtes Testkonto mit Profil, Plan und Markierung vollstaendig loeschen.
- A19 Admin bleibt geschuetzt; Token des geloeschten Nutzers wird spaeter abgewiesen.
- A20 Adminlogout und direkter Seitenaufruf danach; neue Anmeldung erforderlich.
- A21 Backendneustart: neue Anmeldung, dieselben Plan- und Markierungs-IDs aus der Datenbank.
- A22 Frontend-Produktionsbuild startet mit npm start auf PORT und liefert JavaScript.
- A23 API-Fehler/HTML-Proxyfehler sind verstaendlich; Eingaben gehen nicht unbemerkt verloren.
- A24 Frischer Start auf zwei Macs und Windows mit derselben committed Lockdatei.
- A25 Beide Heroku-Apps aus Kandidat-SHA; HTTPS, passende Origins, Cookie und echte Datenquelle.

## Abnahme der Gewichtsprogression
- [ ] Bei 4 oder 5 von 5 als „Done“ markierten Übungen steigen die Gewichtsvorgaben dieser Einheit in der Folgewoche um 5 %.
- [ ] Bei höchstens 3 als „Done“ markierten Übungen bleiben die Gewichtsvorgaben unverändert.
- [ ] Die neuen Gewichte werden kaufmännisch auf 0,5 kg gerundet: beispielsweise 10 kg → 10,5 kg und 25 kg → 26,5 kg.
- [ ] Kleine Gewichte können durch die Rundung unverändert bleiben: beispielsweise 3,5 kg → 3,5 kg.
- [ ] Das neue Gewicht beträgt höchstens 500 kg.
- [ ] Übungen ohne Zusatzgewicht bleiben ohne Zusatzgewicht.
- [ ] Die aktuelle Planwoche bleibt unverändert; die Steigerung gilt erst für die Folgewoche.
- [ ] Bei einer fälligen Neuplanung nach Profiländerung gelten die Startgewichtsregeln statt der Progression des bisherigen Plans.

Ergebnis, Datum, geprüften Commit und kurze Beobachtung dokumentieren. Nicht geprüfte Punkte bleiben offen.

## Freigabe
[ ] Normaler Backendlauf, Frontendtests, Build und statische Serverprobe gruen.
[ ] Reale ExerciseDB-Erstellung und Wiederholung erfolgreich nachgewiesen.
[ ] Keine offenen Fehler im Kernablauf. Alle echten Nachweise verlinkt.
[ ] Drei Arbeitsplaetze und Onlinepruefung tatsaechlich durchgefuehrt.
[ ] Gemeinsame Freigabe durch Person 1, Person 2 und Person 3.
Am 30.10.2026 schlug die externe Katalogprobe mit HTTP 429 fehl.
Das bleibt bis zu einer erfolgreichen neuen Live-Abnahme ein offener Freigabepunkt.
Zeitfaelle koennen automatisiert simuliert werden; das ist kein echter Wochenwechsel im Browser.
Kalender, PB-15 und PB-19 sind nicht Bestandteil der Fertigmeldung. Die feste Gewichtsprogression beim regulären 
Wochenwechsel ist Bestandteil der Abnahme.