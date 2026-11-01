package de.bebetter.backend;

import de.bebetter.backend.exercisedb.RetryAfterPolicy;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryAfterPolicyTest {

    private final Instant now = Instant.parse("2026-10-30T10:00:00Z");

    @Test void missingAndBlankUseExistingMinimum() {
        assertEquals(now.plusSeconds(60), RetryAfterPolicy.until(null, now));
        assertEquals(now.plusSeconds(60), RetryAfterPolicy.until(" ", now));
    }

    @Test void longerDeltaIsRespected() {
        assertEquals(now.plusSeconds(180), RetryAfterPolicy.until(" 180 ", now));
    }

    @Test void shortOrZeroDeltaKeepsMinimum() {
        assertEquals(now.plusSeconds(60), RetryAfterPolicy.until("0", now));
        assertEquals(now.plusSeconds(60), RetryAfterPolicy.until("10", now));
    }

    @Test void futureHttpDateIsRespected() {
        assertEquals(now.plusSeconds(300), RetryAfterPolicy.until("Fri, 30 Oct 2026 10:05:00 GMT", now));
    }

    @Test void pastDateKeepsMinimum() {
        assertEquals(now.plusSeconds(60), RetryAfterPolicy.until("Fri, 30 Oct 2026 09:00:00 GMT", now));
    }

    @Test void invalidAndNegativeValuesUseMinimum() {
        for (String value : new String[]{"-1", "1.5", "tomorrow", "+120"})
            assertEquals(now.plusSeconds(60), RetryAfterPolicy.until(value, now));
    }

    @Test void enormousDeltaDoesNotOverflowIntoAnEarlyRetry() {
        assertEquals(Instant.MAX, RetryAfterPolicy.until("999999999999999999999", now));
        assertEquals(Instant.MAX, RetryAfterPolicy.until("9223372036854775807", now));
    }
}