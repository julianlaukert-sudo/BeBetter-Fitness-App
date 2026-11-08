package de.bebetter.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.domain.TrainingSession;
import de.bebetter.backend.dto.MarkRequest;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.service.MarkService;
import de.bebetter.backend.service.TimeProvider;
import de.bebetter.backend.service.WeekChangeService;
import de.bebetter.backend.web.ApiException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// KI-Log Nr. 15, 31, 32
/** Ohne äußere Testtransaktion: Aufrufe committen wie im Betrieb; Aufräumen in @AfterEach. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainingIntegrationTest {

    static final String PASSWORD = "Secure123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TrainingPlanRepository plans;
    @Autowired WeekChangeService weekChange;
    @Autowired MarkService marks;
    @MockitoBean ExerciseDbClient api;
    @MockitoBean TimeProvider time;   // fachliche Zeit steuerbar; JWT bleibt auf echter Uhr

    long user; String token; long plan; long session; long exercise;
    final LocalDate monday = LocalDate.of(2026, 10, 19);

    @BeforeEach void setup() throws Exception {
        day(monday);
        when(api.exercises(anyCollection())).thenReturn(Map.of());
        var a = register("Training Test");
        user = a.get("userId").asLong();
        token = a.get("token").asText();
        mvc.perform(post("/profile").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PlanIntegrationTest.PROFILE))
                .andExpect(status().isOk());
        // Plan direkt anlegen: A/B/C an Mo/Mi/Fr, je 5 Übungen, die 5. ist Körpergewicht
        var p = new TrainingPlan(user, monday, time.now());
        for (int i = 0; i < 3; i++) {
            var s = new TrainingSession(String.valueOf((char) ('A' + i)), i + 1,
                    monday.plusDays(i * 2));
            for (int j = 0; j < 5; j++) {
                s.addExercise(new PlanExercise(j + 1, "exercise-" + i + "-" + j, "back", 3, "8-12",
                        j == 4 ? null : new BigDecimal("3.5")));
            }
            p.addSession(s);
        }
        p = plans.saveAndFlush(p);
        plan = p.getId();
        session = p.getSessions().get(0).getId();
        exercise = p.getSessions().get(0).getExercises().get(0).getId();
    }

    @AfterEach void clean() {
        deleteUser(user);
    }

    // ---- Helfer ----

    void day(LocalDate d) {
        when(time.today()).thenReturn(d);
        when(time.now()).thenReturn(d.atStartOfDay(ZoneId.of("Europe/Berlin")).toOffsetDateTime());
    }

    JsonNode register(String name) throws Exception {
        String email = UUID.randomUUID() + "@example.org";
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(
                        json.writeValueAsString(Map.of("name", name, "email", email, "password", PASSWORD))))
                .andExpect(status().isOk());
        return json.readTree(mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    void deleteUser(long id) {
        jdbc.update("DELETE FROM trainingsplan WHERE nutzer_id = ?", id);
        jdbc.update("DELETE FROM profil WHERE nutzer_id = ?", id);
        jdbc.update("DELETE FROM nutzer WHERE id = ?", id);
    }

    JsonNode postMark(Map<String, Object> body) throws Exception {
        return json.readTree(mvc.perform(post("/marks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    // KI-Log Nr. 32: neues Format, nur done
    JsonNode mark(long s, long e, boolean done) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("sessionId", s);
        body.put("planExerciseId", e);
        body.put("done", done);
        return postMark(body);
    }

    // Altes Format mit weight/reps (wird vom Backend ignoriert)
    JsonNode mark(long s, long e, boolean done, Object weight, int reps) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("sessionId", s);
        body.put("planExerciseId", e);
        body.put("done", done);
        body.put("weight", weight);
        body.put("reps", reps);
        return postMark(body);
    }

    JsonNode read() throws Exception {
        return json.readTree(mvc.perform(get("/plans/current")
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    // ---- Speichern und Korrigieren ----

    @Test void successThenCorrectionKeepsOneRowAndUnchangedTarget() throws Exception {
        var first = mark(session, exercise, true, 4.0, 10);
        var second = mark(session, exercise, false, 2.5, 0);
        assertEquals(first.get("id"), second.get("id"));
        assertFalse(second.get("done").asBoolean());
        assertEquals(0, second.get("reps").asInt());
        assertEquals(1, count("SELECT COUNT(*) FROM markierung WHERE plan_uebung_id = ?", exercise));
        assertEquals(new BigDecimal("3.5"), jdbc.queryForObject(
                "SELECT gewicht_kg FROM plan_uebung WHERE id = ?", BigDecimal.class, exercise));
        var e = read().get("sessions").get(0).get("exercises").get(0);
        assertEquals(second.get("id"), e.get("mark").get("id"));
    }

    @Test void futureSessionRejectedEvenWithValidValues() throws Exception {
        var s = read().get("sessions").get(1);
        mvc.perform(post("/marks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "sessionId", s.get("sessionId").asLong(),
                                "planExerciseId", s.get("exercises").get(0).get("planExerciseId").asLong(),
                                "done", true, "weight", 4, "reps", 10))))
                .andExpect(jsonPath("$.message").value("Session is not editable."));
        assertFalse(s.get("available").asBoolean());
    }

    @Test void sundayStillEditableButMondayIsClosed() throws Exception {
        day(monday.plusDays(6));
        mark(session, exercise, true, 4, 10);
        day(monday.plusWeeks(1));
        mvc.perform(post("/marks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "sessionId", session, "planExerciseId", exercise,
                                "done", false, "weight", 4, "reps", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Session is not editable."));
    }

    @Test void bodyweightStoresNullAndZeroReps() throws Exception {
        var e = read().get("sessions").get(0).get("exercises").get(4);
        var result = mark(session, e.get("planExerciseId").asLong(), false, null, 0);
        assertTrue(result.get("weight").isNull());
        assertEquals(0, result.get("reps").asInt());
    }

    // KI-Log Nr. 32: alte Clients senden weight/reps – angenommen, aber nicht gespeichert
    @Test void legacyWeightAndRepsAreIgnored() throws Exception {
        var result = mark(session, exercise, true, 4.0, 10);
        assertTrue(result.get("done").asBoolean());
        assertTrue(result.get("weight").isNull());
        assertEquals(0, result.get("reps").asInt());
        assertNull(jdbc.queryForObject("SELECT gewicht_kg FROM markierung WHERE plan_uebung_id = ?",
                BigDecimal.class, exercise));
    }

    // KI-Log Nr. 32: neues Format ohne Istwerte
    @Test void doneOnlyPayloadIsAccepted() throws Exception {
        var result = mark(session, exercise, true);
        assertTrue(result.get("done").asBoolean());
        var corrected = mark(session, exercise, false);
        assertEquals(result.get("id"), corrected.get("id"));
        assertFalse(corrected.get("done").asBoolean());
    }

    @Test void invalidOrMissingValuesDoNotWrite() throws Exception {
        for (String payload : List.of(
                "{}",
                "{\"done\":null}",
                "{\"done\":\"maybe\"}",
                "{\"weight\":4,\"reps\":10}")) {
            var body = (ObjectNode) json.readTree(payload);
            body.put("sessionId", session);
            body.put("planExerciseId", exercise);
            mvc.perform(post("/marks").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, count("SELECT COUNT(*) FROM markierung WHERE plan_uebung_id = ?", exercise));
    }

    // ---- Zugriffsschutz ----

    @Test void wrongSessionAndOtherOwnerCannotMarkExercise() throws Exception {
        mvc.perform(post("/marks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "sessionId", session + 99999, "planExerciseId", exercise,
                                "done", true, "weight", 4, "reps", 10))))
                .andExpect(jsonPath("$.message").value("Exercise not found."));

        var other = register("Other");
        long otherId = other.get("userId").asLong();
        try {
            mvc.perform(post("/profile").header("Authorization", "Bearer " + other.get("token").asText())
                            .contentType(MediaType.APPLICATION_JSON).content(PlanIntegrationTest.PROFILE))
                    .andExpect(status().isOk());
            assertThrows(ApiException.class, () -> marks.save(otherId,
                    new MarkRequest(session, exercise, true)));
        } finally {
            deleteUser(otherId);
        }
    }

    @Test void missingTokenIsRejected() throws Exception {
        mvc.perform(post("/marks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token is wrong."));
    }

    @Test void adminCannotUseNormalTrainingEndpoint() throws Exception {
        jdbc.update("UPDATE nutzer SET rolle = 'ADMIN' WHERE id = ?", user);
        String email = jdbc.queryForObject("SELECT email FROM nutzer WHERE id = ?", String.class, user);
        var auth = json.readTree(mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/marks").header("Authorization", "Bearer " + auth.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    // ---- Wochenabschluss ----

    @Test void partialUnitPreservesFalseMarkAndFillsOnlyMissing() throws Exception {
        var existing = mark(session, exercise, false, 3, 6);
        day(monday.plusWeeks(1));
        var current = read();
        assertNotEquals(plan, current.get("planId").asLong());
        String bySession = "SELECT COUNT(*) FROM markierung m JOIN plan_uebung u "
                + "ON u.id = m.plan_uebung_id WHERE u.einheit_id = ?";
        assertEquals(5, count(bySession, session));
        assertEquals(4, count(bySession + " AND m.automatisch", session));
        assertEquals(existing.get("id").asLong(), jdbc.queryForObject(
                "SELECT id FROM markierung WHERE plan_uebung_id = ?", Long.class, exercise));
        assertEquals(0, count("SELECT wiederholungen FROM markierung WHERE plan_uebung_id = ?", exercise)); // KI-Log Nr. 32
        assertFalse(current.get("sessions").get(0).get("carried").asBoolean());
        assertTrue(current.get("sessions").get(1).get("carried").asBoolean());
        for (var s : current.get("sessions")) {
            for (var e : s.get("exercises")) {
                assertTrue(e.get("mark").isNull());
                assertEquals("8-12", e.get("reps").asText());
                assertEquals(3, e.get("sets").asInt());
                if (!e.get("bodyweight").asBoolean()) assertEquals(3.5, e.get("weight").asDouble());
            }
        }
        verify(api, never()).candidates(anyCollection());
    }

    @Test void entirelyUnmarkedUnitIsCarriedWithoutInventedFailures() throws Exception {
        day(monday.plusWeeks(1));
        var current = read();
        assertTrue(current.get("sessions").get(0).get("carried").asBoolean());
        assertEquals(0, count("SELECT COUNT(*) FROM markierung m JOIN plan_uebung u "
                + "ON u.id = m.plan_uebung_id WHERE u.einheit_id = ?", session));
    }

    @Test void fullyMarkedUnitGetsNoAutomaticRows() throws Exception {
        for (var e : read().get("sessions").get(0).get("exercises"))
            mark(session, e.get("planExerciseId").asLong(), true,
                    e.get("bodyweight").asBoolean() ? null : 4, 10);
        assertEquals("DONE", read().get("sessions").get(0).get("status").asText());
        day(monday.plusWeeks(1));
        read();
        assertEquals(0, count("SELECT COUNT(*) FROM markierung m JOIN plan_uebung u "
                + "ON u.id = m.plan_uebung_id WHERE u.einheit_id = ? AND m.automatisch", session));
    }

    @Test void severalMissedWeeksAndRepeatedReadStayIdempotent() throws Exception {
        day(monday.plusWeeks(3));
        var first = read();
        var second = read();
        assertEquals(first.get("planId"), second.get("planId"));
        assertEquals(monday.plusWeeks(3).toString(), first.get("weekStart").asText());
        assertEquals(4, count("SELECT COUNT(*) FROM trainingsplan WHERE nutzer_id = ?", user));
        assertEquals(3, count("SELECT COUNT(*) FROM trainingsplan WHERE nutzer_id = ? AND ausgewertet", user));
    }

    @Test void sundayDoesNotRollover() throws Exception {
        day(monday.plusDays(6));
        assertEquals(plan, read().get("planId").asLong());
    }

    // ---- Progression (KI-Log Nr. 31) ----

    /** Session A: die ersten {done} Übungen „Done", die nächsten {notDone} „Not done", Rest unmarkiert. */
    void markSessionA(int done, int notDone) throws Exception {
        var exercises = read().get("sessions").get(0).get("exercises");
        for (int i = 0; i < done + notDone; i++) {
            var e = exercises.get(i);
            mark(session, e.get("planExerciseId").asLong(), i < done,
                    e.get("bodyweight").asBoolean() ? null : 10, 10);
        }
    }

    /** Wochenwechsel auslösen und die Übungen der neuen Session A liefern. */
    List<JsonNode> sessionANextWeek() throws Exception {
        day(monday.plusWeeks(1));
        var list = new ArrayList<JsonNode>();
        read().get("sessions").get(0).get("exercises").forEach(list::add);
        return list;
    }

    void setSessionAWeights(String kg) {
        jdbc.update("UPDATE plan_uebung SET gewicht_kg = ? WHERE einheit_id = ? AND gewicht_kg IS NOT NULL",
                new BigDecimal(kg), session);
    }

    void assertSessionAWeights(double expected) throws Exception {
        for (var e : sessionANextWeek()) {
            if (e.get("bodyweight").asBoolean()) assertTrue(e.get("weight").isNull());
            else assertEquals(expected, e.get("weight").asDouble());
            assertEquals(3, e.get("sets").asInt());
            assertEquals("8-12", e.get("reps").asText());
        }
    }

    @Test void fourOfFiveDoneRaisesWeightsByFivePercent() throws Exception {
        setSessionAWeights("10.0");
        markSessionA(4, 1);
        assertSessionAWeights(10.5);
        // Session B war unmarkiert: unverändert
        assertEquals(3.5, read().get("sessions").get(1).get("exercises").get(0).get("weight").asDouble());
    }

    @Test void fiveOfFiveDoneRaisesWeightsByFivePercent() throws Exception {
        setSessionAWeights("10.0");
        markSessionA(5, 0);
        assertSessionAWeights(10.5);
    }

    @Test void threeDoneAndTwoNotDoneKeepWeights() throws Exception {
        setSessionAWeights("10.0");
        markSessionA(3, 2);
        assertSessionAWeights(10.0);
    }

    @Test void threeDoneAndTwoUnmarkedKeepWeights() throws Exception {
        setSessionAWeights("10.0");
        markSessionA(3, 0);   // unmarkiert zählt wie „Not done"
        assertSessionAWeights(10.0);
    }
    // ---- Parallelität ----

    @Test void concurrentRolloverCreatesOneSuccessor() throws Exception {
        day(monday.plusWeeks(1));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> weekChange.update(user));
            var b = pool.submit(() -> weekChange.update(user));
            a.get(20, TimeUnit.SECONDS);
            b.get(20, TimeUnit.SECONDS);
            assertEquals(2, count("SELECT COUNT(*) FROM trainingsplan WHERE nutzer_id = ?", user));
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test void concurrentCorrectionsProduceOneMark() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        try {
            var r =new MarkRequest(session, exercise, false);
            var a = pool.submit(() -> marks.save(user, r).id());
            var b = pool.submit(() -> marks.save(user, r).id());
            assertEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // ---- Status ----

    @Test void failedAndMixedStatusesAreDerivedFromAllFiveMarks() throws Exception {
        for (var e : read().get("sessions").get(0).get("exercises"))
            mark(session, e.get("planExerciseId").asLong(), false,
                    e.get("bodyweight").asBoolean() ? null : 3, 0);
        assertEquals("FAILED", read().get("sessions").get(0).get("status").asText());
        mark(session, exercise, true, 3, 10);
        assertEquals("PARTIAL", read().get("sessions").get(0).get("status").asText());
    }
}