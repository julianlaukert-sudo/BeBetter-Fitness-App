package de.bebetter.backend;

import de.bebetter.backend.service.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
// KI-Log Nr. 15
class TimeProviderTest {

    @Test void mondayBeginsAtBerlinMidnightInSummerTime() {
        var instant = Instant.parse("2026-10-18T22:01:00Z");
        var time = new TimeProvider(Clock.fixed(instant, ZoneId.of("Europe/Berlin")));
        assertEquals(LocalDate.of(2026, 10, 19), time.today());
        assertEquals(instant, time.now().toInstant());
    }

    @Test void berlinWinterTimeIsRespectedToo() {
        var time = new TimeProvider(Clock.fixed(Instant.parse("2026-11-01T23:01:00Z"),
                ZoneId.of("Europe/Berlin")));
        assertEquals(LocalDate.of(2026, 11, 2), time.today());
    }
}