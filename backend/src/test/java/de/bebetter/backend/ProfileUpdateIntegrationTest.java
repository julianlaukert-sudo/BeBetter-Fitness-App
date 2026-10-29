package de.bebetter.backend;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bebetter.backend.exercisedb.ExerciseDbException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileUpdateIntegrationTest extends Sprint5Support {

    @Test void changedProfileKeepsCurrentPlanAndItsResult() throws Exception {
        var mark = result(call(post("/marks"), token, Map.of("sessionId", session,
                "planExerciseId", exercise, "done", true, "weight", 4, "reps", 10)));
        var profile = update(changedProfile());
        assertEquals(monday.plusWeeks(1).toString(), profile.get("nextPlanFrom").asText());
        var p = current();
        assertEquals(plan, p.get("planId").asLong());
        var e = p.get("sessions").get(0).get("exercises").get(0);
        assertEquals(mark.get("id"), e.get("mark").get("id"));
        assertEquals(3.5, e.get("weight").asDouble());
        assertEquals(2, e.get("sets").asInt());
    }

    @Test void nextWeekUsesNewDaysGoalLevelAndBodyData() throws Exception {
        update(changedProfile());
        day(monday.plusWeeks(1));
        var next = current();
        assertNotEquals(plan, next.get("planId").asLong());
        assertEquals(monday.plusWeeks(1).plusDays(1).toString(),   // Dienstag
                next.get("sessions").get(0).get("date").asText());
        for (var s : next.get("sessions")) {
            for (var e : s.get("exercises")) {
                assertEquals(3, e.get("sets").asInt());
                assertEquals("8-12", e.get("reps").asText());
                assertTrue(e.get("mark").isNull());
            }
        }
        assertTrue(next.get("sessions").get(0).get("exercises").get(0).get("weight").asDouble() > 3.5);
        assertNull(profiles.findById(user).orElseThrow().getReplanFrom());
        current();
        verify(api, times(1)).candidates(anyCollection());
    }

    @Test void identicalProfileDoesNotRequestNewSelection() throws Exception {
        assertTrue(update(json.readTree(PlanIntegrationTest.PROFILE)).get("nextPlanFrom").isNull());
        day(monday.plusWeeks(1));
        current();
        verify(api, never()).candidates(anyCollection());
    }

    @Test void genderOnlyChangeDoesNotInventAWeightMultiplier() throws Exception {
        var body = json.readTree(PlanIntegrationTest.PROFILE);
        ((ObjectNode) body.get("personal")).put("gender", "female");
        assertTrue(update(body).get("nextPlanFrom").isNull());
    }

    @Test void invalidProfileLeavesStoredValuesUntouched() throws Exception {
        var body = changedProfile();
        ((ObjectNode) body.get("training")).putArray("trainingDays").add("MON").add("TUE").add("FRI");
        call(put("/profile"), token, body).andExpect(status().isBadRequest());
        assertEquals(new BigDecimal("78.5"), profiles.findById(user).orElseThrow().getWeightKg());
        assertNull(profiles.findById(user).orElseThrow().getReplanFrom());
    }

    @Test void offlineReplacementRollsBackCompletionAndCanBeRetried() throws Exception {
        result(call(post("/marks"), token, Map.of("sessionId", session,
                "planExerciseId", exercise, "done", false, "weight", 3, "reps", 6)));
        update(changedProfile());
        day(monday.plusWeeks(1));
        doThrow(new ExerciseDbException("offline")).when(api).candidates(anyCollection());
        call(get("/plans/current"), token, null).andExpect(status().isBadRequest());
        assertFalse(jdbc.queryForObject("SELECT ausgewertet FROM trainingsplan WHERE id = ?",
                Boolean.class, plan));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM trainingsplan WHERE nutzer_id = ?",
                Integer.class, user));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM markierung WHERE plan_uebung_id IN "
                + "(SELECT u.id FROM plan_uebung u JOIN einheit e ON e.id = u.einheit_id "
                + "WHERE e.plan_id = ?)", Integer.class, plan));
        doReturn(PlanRulesTest.candidates()).when(api).candidates(anyCollection());
        assertNotEquals(plan, current().get("planId").asLong());
    }

    @Test void lateEditDoesNotApplyToAlreadyMissedWeeks() throws Exception {
        day(monday.plusWeeks(2));
        var saved = update(changedProfile());
        assertEquals(monday.plusWeeks(3).toString(), saved.get("nextPlanFrom").asText());
        var e = current().get("sessions").get(0).get("exercises").get(0);
        assertEquals(3.5, e.get("weight").asDouble());
        verify(api, never()).candidates(anyCollection());
    }

    @ParameterizedTest
    @CsvSource({"age,65", "height,170", "weight,90"})
    void eachBodyFactorAloneSchedulesNewStartValues(String field, int value) throws Exception {
        var body = json.readTree(PlanIntegrationTest.PROFILE);
        ((ObjectNode) body.get("personal")).put(field, value);
        assertEquals(monday.plusWeeks(1).toString(), update(body).get("nextPlanFrom").asText());
        assertEquals(3.5, current().get("sessions").get(0).get("exercises").get(0)
                .get("weight").asDouble());
    }

    @Test void editBeforeFirstPlanIsUsedImmediatelyForFirstGeneration() throws Exception {
        jdbc.update("DELETE FROM trainingsplan WHERE nutzer_id = ?", user);
        assertTrue(update(changedProfile()).get("nextPlanFrom").isNull());
        result(call(post("/plans"), token, null));
        assertEquals(3, current().get("sessions").get(0).get("exercises").get(0)
                .get("sets").asInt());
    }
}