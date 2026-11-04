package de.bebetter.backend;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// KI-Log Nr. 19
/** Zusammenhängende Abschlussfälle über alle Funktionen. */
class ReleaseFlowIntegrationTest extends Sprint5Support {

    JsonNode login(String email) throws Exception {
        return json.readTree(mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test void freshAccountToTrainingProfileChangeAndAdminDeletion() throws Exception {
        // Registrieren und anmelden
        String email = UUID.randomUUID() + "@example.org";
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(
                        json.writeValueAsString(Map.of("name", "Release Flow", "email", email,
                                "password", PASSWORD))))
                .andExpect(status().isOk());
        JsonNode auth = login(email);
        user = auth.get("userId").asLong();
        cleanup.add(user);
        token = auth.get("token").asText();
        assertFalse(auth.get("profileCompleted").asBoolean());

        // Profil und erster Plan
        result(call(post("/profile"), token, json.readTree(PlanIntegrationTest.PROFILE)));
        result(call(post("/plans"), token, null));
        var first = current();
        long oldPlan = first.get("planId").asLong();
        assertEquals(3, first.get("sessions").size());
        var s = first.get("sessions").get(0);
        assertEquals(5, s.get("exercises").size());
        var e = s.get("exercises").get(0);

        // Markieren und korrigieren (gleiche Markierungs-ID)
        var body = new LinkedHashMap<String, Object>();
        body.put("sessionId", s.get("sessionId").asLong());
        body.put("planExerciseId", e.get("planExerciseId").asLong());
        body.put("done", true);
        body.put("weight", 4);
        body.put("reps", 10);
        var mark = result(call(post("/marks"), token, body));
        body.put("done", false);
        body.put("reps", 6);
        assertEquals(mark.get("id"), result(call(post("/marks"), token, body)).get("id"));

        // Profiländerung: aktuelle Woche bleibt, neue Woche nach neuem Profil
        update(changedProfile());
        assertEquals(oldPlan, current().get("planId").asLong());
        day(monday.plusWeeks(1));
        var next = current();
        assertNotEquals(oldPlan, next.get("planId").asLong());
        assertEquals(monday.plusWeeks(1).plusDays(1).toString(),
                next.get("sessions").get(0).get("date").asText());

        // Admin löscht – altes Token ist danach ungültig
        var admin = adminToken();
        call(delete("/admin/users/" + user), admin, null).andExpect(status().isOk());
        call(get("/profile"), token, null).andExpect(jsonPath("$.message").value("Token is wrong."));
    }

    @Test void loginAgainKeepsPlanAndCorrectedValues() throws Exception {
        var mark = result(call(post("/marks"), token, Map.of("sessionId", session,
                "planExerciseId", exercise, "done", false, "weight", 3, "reps", 0)));
        result(call(delete("/auth"), token, null));                       // Logout
        call(get("/plans/current"), token, null).andExpect(status().isBadRequest());
        String email = users.findById(user).orElseThrow().getEmail();
        token = login(email).get("token").asText();                       // neu anmelden
        var stored = current();
        assertEquals(plan, stored.get("planId").asLong());
        assertEquals(mark.get("id"),
                stored.get("sessions").get(0).get("exercises").get(0).get("mark").get("id"));
    }

    @Test void browserPreflightAllowsProfilePutOnlyFromConfiguredOrigin() throws Exception {
        mvc.perform(options("/profile").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/profile").header("Origin", "https://other.example.org")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isForbidden());
    }
}