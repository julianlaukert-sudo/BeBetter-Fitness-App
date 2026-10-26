package de.bebetter.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.exercisedb.CatalogExercise;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.exercisedb.ExerciseDbException;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.service.PlanGenerator;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired TrainingPlanRepository plans;
    @MockitoBean ExerciseDbClient api;   // nur der externe Katalog ist simuliert

    record Account(long id, String token) {}

    static final String PASSWORD = "Secure123!";
    static final String PROFILE = """
        {"personal":{"age":23,"gender":"male","height":180,"weight":78.5},
         "training":{"goal":"health","fitnessLevel":"beginner",
          "trainingDays":["MON","WED","FRI"]}}
        """;

    @BeforeEach void catalog() {
        when(api.candidates(anyCollection())).thenReturn(PlanRulesTest.candidates());
        when(api.exercises(anyCollection())).thenAnswer(call -> {
            Collection<String> ids = call.getArgument(0);
            var result = new HashMap<String, CatalogExercise>();
            PlanRulesTest.candidates().values().stream().flatMap(List::stream)
                    .filter(u -> ids.contains(u.exerciseId()))
                    .forEach(u -> result.put(u.exerciseId(), u));
            return result;
        });
    }

    // ---- Helfer ----

    Account account(boolean withProfile) throws Exception {
        String email = UUID.randomUUID() + "@example.org";
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(
                        json.writeValueAsString(Map.of("name", "Plan Test", "email", email,
                                "password", PASSWORD))))
                .andExpect(status().isOk());
        var auth = json.readTree(mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var a = new Account(auth.get("userId").asLong(), auth.get("token").asText());
        if (withProfile) mvc.perform(post("/profile").header("Authorization", "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk());
        return a;
    }

    JsonNode create(Account a) throws Exception {
        return json.readTree(mvc.perform(post("/plans")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    JsonNode read(Account a) throws Exception {
        return json.readTree(mvc.perform(get("/plans/current")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    // ---- Tests ----

    @Test void threeSessionsFiveExercisesAndPersistentModelValues() throws Exception {
        var a = account(true);
        create(a);
        var p = read(a);
        assertEquals(3, p.get("sessions").size());
        var ids = new HashSet<String>();
        LocalDate monday = PlanGenerator.mondayOf(LocalDate.now(ZoneId.of("Europe/Berlin")));
        for (int i = 0; i < 3; i++) {
            var s = p.get("sessions").get(i);
            assertEquals(5, s.get("exercises").size());
            assertEquals(monday.plusDays(i * 2).toString(), s.get("date").asText()); // MON/WED/FRI
            for (var e : s.get("exercises")) {
                assertTrue(ids.add(e.get("exerciseId").asText()));
                assertEquals(2, e.get("sets").asInt());
                if (e.get("equipment").asText().equals("dumbbell"))
                    assertEquals(3.5, e.get("weight").asDouble());
                if (e.get("bodyweight").asBoolean()) assertTrue(e.get("weight").isNull());
            }
        }
        assertEquals(15, ids.size());
        // Übungsnamen/Bilder werden NICHT in der Datenbank gespeichert
        List<String> columns = jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_name = 'plan_uebung'", String.class);
        assertFalse(columns.contains("name"));
        assertFalse(columns.contains("gif_url"));
        mvc.perform(get("/profile").header("Authorization", "Bearer " + a.token()))
                .andExpect(jsonPath("$.planExists").value(true));
    }

    @Test void repeatedPostReturnsSamePlanWithoutAnotherApiSelection() throws Exception {
        var a = account(true);
        assertEquals(create(a).get("planId"), create(a).get("planId"));
        verify(api, times(1)).candidates(anyCollection());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failureKeepsProfileAndRetryCanSucceed() throws Exception {
        var a = account(true);
        try {
            doThrow(new ExerciseDbException("offline")).when(api).candidates(anyCollection());
            mvc.perform(post("/plans").header("Authorization", "Bearer " + a.token()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Exercises could not be loaded."));
            assertFalse(plans.existsByUserId(a.id()));
            mvc.perform(get("/profile").header("Authorization", "Bearer " + a.token()))
                    .andExpect(status().isOk());
            doReturn(PlanRulesTest.candidates()).when(api).candidates(anyCollection());
            create(a);
        } finally {
            clean(a);
        }
    }

    @Test void missingDetailsPreserveSavedInstructions() throws Exception {
        var a = account(true);
        create(a);
        when(api.exercises(anyCollection())).thenReturn(Map.of());
        var p = read(a);
        assertFalse(p.get("exerciseDataAvailable").asBoolean());
        var e = p.get("sessions").get(0).get("exercises").get(0);
        assertTrue(e.get("name").isNull());
        assertTrue(e.hasNonNull("exerciseId"));
        assertEquals(2, e.get("sets").asInt());
    }

    @Test
    void otherUsersPlanCannotBeSelectedByQueryOrBody() throws Exception {
        var a = account(true);
        var b = account(true);
        var first = create(a);

        mvc.perform(get("/plans/current?userId=" + a.id())
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No plan found."));

        var response = mvc.perform(post("/plans")
                        .header("Authorization", "Bearer " + b.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + a.id() + "}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotEquals(
                first.get("planId"),
                json.readTree(response).get("planId")
        );
    }

    @Test void insufficientCandidatesLeaveNoPartialPlan() throws Exception {
        var a = account(true);
        when(api.candidates(anyCollection())).thenReturn(Map.of());
        mvc.perform(post("/plans").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isBadRequest());
        assertFalse(plans.existsByUserId(a.id()));
    }

    @Test void missingProfileAndMissingTokenAreRejected() throws Exception {
        var a = account(false);
        mvc.perform(post("/plans").header("Authorization", "Bearer " + a.token()))
                .andExpect(jsonPath("$.message").value("Profile not found."));
        mvc.perform(get("/plans/current"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token is wrong."));
        verify(api, never()).candidates(anyCollection());
    }

    // Sprint 4: ein alter Plan wird jetzt in die aktuelle Woche kopiert – ohne neue Auswahl
    @Test void oldPlanNowRollsOverWithoutAnotherSelection() throws Exception {
        var a = account(true);
        var monday = PlanGenerator.mondayOf(LocalDate.now(ZoneId.of("Europe/Berlin")));
        plans.saveAndFlush(new TrainingPlan(a.id(), monday.minusWeeks(1), OffsetDateTime.now()));
        mvc.perform(post("/plans").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(monday.toString()));
        verify(api, never()).candidates(anyCollection());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousPostsCreateOnlyOnePlan() throws Exception {
        Account a = account(true);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> create(a).get("planId").asLong());
            var second = pool.submit(() -> create(a).get("planId").asLong());
            assertEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trainingsplan WHERE nutzer_id = ?", Integer.class, a.id()));
            verify(api, times(1)).candidates(anyCollection());
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
            clean(a);
        }
    }

    // Räumt nur die selbst angelegten Testdaten auf (für die Tests ohne Rollback)
    private void clean(Account a) {
        jdbc.update("DELETE FROM trainingsplan WHERE nutzer_id = ?", a.id());
        jdbc.update("DELETE FROM profil WHERE nutzer_id = ?", a.id());
        jdbc.update("DELETE FROM nutzer WHERE id = ?", a.id());
    }
}
