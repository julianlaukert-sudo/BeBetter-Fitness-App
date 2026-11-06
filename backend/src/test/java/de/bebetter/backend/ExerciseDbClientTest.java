package de.bebetter.backend;

import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.exercisedb.ExerciseDbException;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
// KI-Log Nr. 12
/** Kontrollierte HTTP-Antworten, kein Internet nötig. */
class ExerciseDbClientTest {

    static final String ITEM = """
        {"exerciseId":"x1","name":"dumbbell press","bodyParts":["chest"],
         "targetMuscles":["pectorals"],"equipments":["dumbbell"],"gifUrl":null}
        """;

    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-15T10:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @Test void pagesCacheAndExactSixHourExpiry() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var clock = new TestClock();
        var api = new ExerciseDbClient("https://one.test", builder, clock);
        server.expect(requestTo("https://one.test/exercises?bodyParts=chest&limit=25"))
                .andRespond(withSuccess("{\"data\":[" + ITEM
                        + "],\"meta\":{\"nextCursor\":\"x1\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://one.test/exercises?bodyParts=chest&limit=25&after=x1"))
                .andRespond(withSuccess("{\"data\":[],\"meta\":{}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withSuccess("{\"data\":" + ITEM + "}", MediaType.APPLICATION_JSON));
        try {
            assertEquals(1, api.byBodyPart("chest").size());
            assertEquals(1, api.byBodyPart("chest").size());       // aus dem Cache
            assertEquals("x1", api.exercise("x1").exerciseId());   // Detailcache aus Liste
            clock.now = clock.now.plus(Duration.ofHours(6));       // genau abgelaufen
            assertEquals("x1", api.exercise("x1").exerciseId());   // neuer Aufruf
            server.verify();
        } finally { api.close(); }
    }

    @Test void failedPrimaryUsesSecondAddress() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var api = new ExerciseDbClient("https://one.test,https://two.test", builder,
                new TestClock());
        server.expect(requestTo("https://one.test/exercises/x1")).andRespond(withServerError());
        server.expect(requestTo("https://two.test/exercises/x1"))
                .andRespond(withSuccess("{\"data\":" + ITEM + "}", MediaType.APPLICATION_JSON));
        try { assertEquals("x1", api.exercise("x1").exerciseId()); server.verify(); }
        finally { api.close(); }
    }

    @Test void partialDetailsDoNotLoseAvailableExercise() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        var api = new ExerciseDbClient("https://one.test", builder, new TestClock());
        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withSuccess("{\"data\":" + ITEM + "}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://one.test/exercises/missing"))
                .andRespond(withServerError());
        try {
            var result = api.exercises(List.of("x1", "missing"));
            assertEquals(1, result.size());
            assertTrue(result.containsKey("x1"));
            server.verify();
        } finally { api.close(); }
    }

    @Test void malformedDetailIsNotAccepted() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var api = new ExerciseDbClient("https://one.test", builder, new TestClock());
        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withSuccess("{\"data\":{}}", MediaType.APPLICATION_JSON));
        try { assertThrows(ExerciseDbException.class, () -> api.exercise("x1")); server.verify(); }
        finally { api.close(); }
    }

    @Test void repeatedCursorStopsPagination() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var api = new ExerciseDbClient("https://one.test", builder, new TestClock());
        String response = "{\"data\":[" + ITEM + "],\"meta\":{\"nextCursor\":\"x1\"}}";
        server.expect(requestTo("https://one.test/exercises?bodyParts=chest&limit=25"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://one.test/exercises?bodyParts=chest&limit=25&after=x1"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        try { assertEquals(1, api.byBodyPart("chest").size()); server.verify(); }
        finally { api.close(); }
    }

    @Test void rateLimitStopsFurtherRequestsForSixtySeconds() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var clock = new TestClock();
        var api = new ExerciseDbClient("https://one.test", builder, clock);
        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withSuccess("{\"data\":" + ITEM + "}", MediaType.APPLICATION_JSON));
        try {
            assertThrows(ExerciseDbException.class, () -> api.exercise("x1"));
            assertThrows(ExerciseDbException.class, () -> api.exercise("x1")); // Sperrfrist
            clock.now = clock.now.plusSeconds(60);
            assertEquals("x1", api.exercise("x1").exerciseId());
            server.verify();
        } finally { api.close(); }
    }
    // KI-Log Nr. 29: Prüft die gemeinsame Sperrfrist bei HTTP 429.
    @Test void clientHonorsRetryAfterOnBothAddresses() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var clock = new TestClock();
        var api = new ExerciseDbClient("https://one.test,https://two.test", builder, clock);

        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "120"));
        server.expect(requestTo("https://one.test/exercises/x1"))
                .andRespond(withSuccess("{\"data\":" + ITEM + "}", MediaType.APPLICATION_JSON));

        try {
            assertThrows(ExerciseDbException.class, () -> api.exercise("x1"));
            clock.now = clock.now.plusSeconds(119);
            assertThrows(ExerciseDbException.class, () -> api.exercise("x1"));
            clock.now = clock.now.plusSeconds(1);
            assertEquals("x1", api.exercise("x1").exerciseId());
            server.verify();
        } finally {
            api.close();
        }
    }
}