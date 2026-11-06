Java-/Spring-Boot-Backend und API.

### ExerciseDB-Katalog und Planerstellung

Beim Start lädt ein Hintergrundjob die Übungen für die fünf Körperregionen in den Cache. Zehn Minuten nach Ende jedes Durchlaufs prüft er erneut, welche Regionen fehlen oder in höchstens 45 Minuten ablaufen. Nur diese Regionen werden nachgeladen. Ein Eintrag ist sechs Stunden gültig. Schlägt das Nachladen fehl, bleibt die bisherige Liste bis zu ihrem ursprünglichen Ablauf nutzbar.

Bei warmem Cache wählt die App die Übungen für einen neuen Plan direkt aus den gespeicherten Listen aus. Fehlt eine gültige Liste, kann die Plananfrage sie synchron nachladen. Das kann zum Beispiel direkt nach einem Neustart passieren. Hintergrundjob und Plananfragen teilen sich pro Backend-Instanz den Mindestabstand von 500 ms zwischen externen Aufrufen. Bei HTTP 429 berücksichtigen beide die vom Anbieter vorgegebene Wartezeit (`Retry-After`).

Die Auswahl der Übungen und das Speichern des Plans bleiben synchron. Die Auswahl arbeitet bei warmem Cache ausschließlich mit lokalen Daten. Durch das synchrone Speichern erhält der Nutzer die Erfolgsmeldung erst, wenn sein Plan in der Datenbank gespeichert ist und geöffnet werden kann.
