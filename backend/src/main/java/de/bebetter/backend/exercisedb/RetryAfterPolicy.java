package de.bebetter.backend.exercisedb;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Berechnet den frühesten erlaubten nächsten Abruf nach HTTP 429.
 * Retry-After als Sekunden oder RFC-1123-Datum; mindestens 60 Sekunden Pause.
 */
public final class RetryAfterPolicy {

    private static final long MINIMUM_SECONDS = 60;

    private RetryAfterPolicy() {}

    public static Instant until(String header, Instant now) {
        Instant minimum = now.plusSeconds(MINIMUM_SECONDS);
        if (header == null || header.isBlank()) return minimum;
        String value = header.trim();
        Instant requested;
        if (value.matches("[0-9]+")) {
            try {
                requested = now.plusSeconds(Long.parseLong(value));
            } catch (NumberFormatException | DateTimeException | ArithmeticException ex) {
                return Instant.MAX;   // riesige Angabe: vorsichtig sperren statt Überlauf
            }
        } else {
            try {
                requested = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            } catch (DateTimeException ex) {
                return minimum;       // unverständlicher Wert: Mindestpause
            }
        }
        return requested.isAfter(minimum) ? requested : minimum;
    }
}