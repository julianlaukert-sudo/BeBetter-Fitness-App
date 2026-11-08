# BeBetter – Bedienung

Die Oberfläche ist englisch; die Schaltflächen sind unten in Anführungszeichen genannt.

## Start
1. „Sign up“: Name, E-Mail und Passwort eingeben und das Passwort bestätigen.
   Passwort: 8–64 Zeichen, max. 72 UTF-8-Byte, ein Großbuchstabe, eine Zahl, ein Sonderzeichen.
2. „Log in“ mit E-Mail und Passwort.
3. Onboarding in drei Schritten: Körperdaten, Ziel und Fitnesslevel, drei Trainingstage
   mit mindestens einem Ruhetag dazwischen (auch zwischen Sonntag und Montag).
4. „Save profile“: Danach öffnet sich „Training“ und der erste Plan wird erstellt.
   Bei einer Störung der Übungsdatenbank bleibt das Profil gespeichert – später „Try again“.

## Training
- Drei Einheiten (Session A, B, C) mit je fünf Übungen.
- „Exercise details“ zeigt Zielmuskel, Gerät und Bild (falls verfügbar).
- „Training target“ ist ein berechneter Startvorschlag (Modellwert, keine Kraftmessung);
  bei Kurzhanteln gilt das Gewicht pro Hand.
- Pro Übung „Done“ antippen, wenn sie geschafft ist (Häkchen). Erneutes Antippen nimmt das Häkchen weg.
  Kein Häkchen bedeutet „nicht geschafft“. Gewicht und Wiederholungen werden nicht eingetragen.
- Ändern ist vom Trainingstag bis Sonntag möglich.
- Zukünftige Einheiten sind lesbar, aber gesperrt.
- Status auf den Karten: Open, Started, Done, Not done, Partially done.

## Neue Woche
- Ab Montag entsteht automatisch eine neue Woche mit denselben Übungen.
- Wer in einer Einheit mindestens 4 von 5 Übungen als „Done" markiert, bekommt dort in der neuen Woche 5 % mehr Gewicht (auf 0,5 kg gerundet). Sonst bleiben die Vorgaben gleich.
- Nur offene Übungen teilweise markierter Einheiten werden automatisch „nicht geschafft“.
- Ganz unmarkierte Einheiten erscheinen als „Carried over …“.
- Nach einer Pause holt „Refresh plan“ fehlende Wochen nach.
- Kein Kalender.

## Profil bearbeiten („My profile“)
- Aktuelle Werte sind vorbelegt; Änderungen mit dem Häkchen bestätigen und „Save changes“.
- Relevante Änderungen wirken ab dem angezeigten nächsten Montag; die laufende Woche bleibt.
- „Discard changes“ stellt die gespeicherten Werte wieder her.

## Admin
- Ein vorbereiteter Admin meldet sich auf derselben Loginseite an und landet im
  Backend-Adminbereich („Overview“).
- „Users“ zeigt alle Konten; zum Löschen „Review deletion“, Name und E-Mail prüfen,
  dann „Permanently delete user and data“. Das Admin-Konto ist „Protected“.
- „Log out“ beendet die Admin-Session und führt zur Loginseite zurück.

## Wenn etwas nicht klappt
- Nach einem Timeout zuerst neu laden – der Server kann bereits gespeichert haben.
- „Saved, but the view could not be refreshed“ heißt: gespeichert, nur die Anzeige ist veraltet.
- Fehlende Übungsdetails: später erneut versuchen, nicht neu registrieren.
- Nach einem Backend-Neustart erneut anmelden; die Daten bleiben erhalten.

## Wenn die Übungsquelle vorübergehend sperrt
- Nicht mehrfach hintereinander neu laden und nicht viele Tabs gleichzeitig öffnen.
- Eine Anbietersperre kann länger als eine Minute dauern; danach einmal „Try again“.
- Beim ersten Plan kann der Ablauf blockiert sein, bis die Quelle wieder antwortet.
- Bei vorhandenen Plänen bleiben Vorgaben und Markierungen erhalten.
- Profil nicht neu anlegen, Konto nicht löschen, Datenbank nicht zurücksetzen.
- Bleibt der Fehler, Zeitpunkt und sichtbare Meldung an das Team geben.