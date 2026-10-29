package de.bebetter.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.domain.TrainingSession;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.security.TokenService;
import de.bebetter.backend.service.TimeProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Gemeinsame Testbasis für Sprint 5 (ohne äußere Testtransaktion, Aufräumen in @AfterEach). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class Sprint5Support {

    static final String PASSWORD = "Secure123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired ProfileRepository profiles;
    @Autowired TrainingPlanRepository plans;
    @Autowired PasswordEncoder passwords;
    @Autowired TokenService tokens;
    @MockitoBean ExerciseDbClient api;
    @MockitoBean TimeProvider time;

    final LocalDate monday = LocalDate.of(2026, 10, 26);
    final List<Long> cleanup = new ArrayList<>();
    long user, plan, session, exercise;
    String token;

    @BeforeEach void setup() throws Exception {
        day(monday);
        when(api.candidates(anyCollection())).thenReturn(PlanRulesTest.candidates());
        when(api.exercises(anyCollection())).thenReturn(Map.of());

        User u = users.saveAndFlush(new User("Sprint Five", UUID.randomUUID() + "@example.org",
                passwords.encode(PASSWORD), Role.USER, OffsetDateTime.now()));
        user = u.getId();
        cleanup.add(user);
        token = tokens.create(u);
        call(post("/profile"), token, json.readTree(PlanIntegrationTest.PROFILE))
                .andExpect(status().isOk());

        // Aktueller Plan: A/B/C an Mo/Mi/Fr, je 5 Übungen mit 2 Sätzen, 12-15, 3.5 kg
        var p = new TrainingPlan(user, monday, time.now());
        for (int i = 0; i < 3; i++) {
            var s = new TrainingSession(String.valueOf((char) ('A' + i)), i + 1,
                    monday.plusDays(i * 2));
            for (int j = 0; j < 5; j++) {
                s.addExercise(new PlanExercise(j + 1, "exercise-" + i + "-" + j, "back", 2,
                        "12-15", new BigDecimal("3.5")));
            }
            p.addSession(s);
        }
        p = plans.saveAndFlush(p);
        plan = p.getId();
        session = p.getSessions().get(0).getId();
        exercise = p.getSessions().get(0).getExercises().get(0).getId();
    }

    void day(LocalDate d) {
        when(time.today()).thenReturn(d);
        when(time.now()).thenReturn(d.atStartOfDay(ZoneId.of("Europe/Berlin")).toOffsetDateTime());
    }

    String adminToken() {
        User admin = users.saveAndFlush(new User("Admin Test", UUID.randomUUID() + "@example.org",
                passwords.encode(PASSWORD), Role.ADMIN, OffsetDateTime.now()));
        cleanup.add(admin.getId());
        return tokens.create(admin);
    }

    ResultActions call(MockHttpServletRequestBuilder req, String bearer, Object body)
            throws Exception {
        req.header("Authorization", "Bearer " + bearer);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(req);
    }

    JsonNode result(ResultActions action) throws Exception {
        return json.readTree(action.andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    JsonNode current() throws Exception {
        return result(call(get("/plans/current"), token, null));
    }

    JsonNode update(JsonNode body) throws Exception {
        return result(call(put("/profile"), token, body));
    }

    /** Geändertes Profil: 90 kg, Muskelaufbau, Fortgeschritten, Di/Do/Sa. */
    ObjectNode changedProfile() throws Exception {
        var body = (ObjectNode) json.readTree(PlanIntegrationTest.PROFILE);
        ((ObjectNode) body.get("personal")).put("weight", 90);
        var training = (ObjectNode) body.get("training");
        training.put("goal", "muscle_gain");
        training.put("fitnessLevel", "advanced");
        training.putArray("trainingDays").add("TUE").add("THU").add("SAT");
        return body;
    }

    // Vor V7 (Commit 3) noch ausdrücklich von unten nach oben löschen
    @AfterEach void clean() {
        for (Long id : cleanup) {
            jdbc.update("DELETE FROM trainingsplan WHERE nutzer_id = ?", id);
            jdbc.update("DELETE FROM profil WHERE nutzer_id = ?", id);
            jdbc.update("DELETE FROM nutzer WHERE id = ?", id);
        }
    }
}