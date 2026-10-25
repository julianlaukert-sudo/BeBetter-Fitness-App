package de.bebetter.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Fachliche Zeit (heute/jetzt in Europe/Berlin) – in Tests ersetzbar.
 * JWT-Prüfung und Ablaufzeit bleiben auf der echten Uhr.
 */
@Component
public class TimeProvider {

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() { return LocalDate.now(clock); }

    public OffsetDateTime now() { return OffsetDateTime.now(clock); }
}