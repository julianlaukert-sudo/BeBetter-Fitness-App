package de.bebetter.backend;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// KI-Log Nr. 18
class AdminIntegrationTest extends Sprint5Support {

    static final String FRONTEND = "http://localhost:5173";

    MockHttpSession session(String bearer) throws Exception {
        return (MockHttpSession) mvc.perform(post("/admin/sso").header("Origin", FRONTEND)
                        .param("token", bearer))
                .andExpect(redirectedUrl("/admin/overview"))
                .andReturn().getRequest().getSession(false);
    }

    @Test void normalUserCannotReadApiOrStartAdminSession() throws Exception {
        call(get("/admin/users"), token, null).andExpect(status().isBadRequest());
        call(get("/admin/stats"), token, null)
                .andExpect(jsonPath("$.message").value("Access denied."));
        mvc.perform(get("/admin/accounts").header("Authorization", "Bearer " + token))
                .andExpect(redirectedUrl(FRONTEND + "/login"));
        mvc.perform(post("/admin/sso").header("Origin", FRONTEND).param("token", token))
                .andExpect(status().isForbidden());
    }

    @Test void ssoRequiresTrustedOrigin() throws Exception {
        var admin = adminToken();
        mvc.perform(post("/admin/sso").param("token", admin)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/sso").header("Origin", "https://other.example.org")
                .param("token", admin)).andExpect(status().isForbidden());
    }

    @Test void ssoReplacesOldSessionAndConsumesBearer() throws Exception {
        var admin = adminToken();
        var old = new MockHttpSession();
        var oldId = old.getId();
        var result = mvc.perform(post("/admin/sso").session(old).header("Origin", FRONTEND)
                        .param("token", admin))
                .andExpect(redirectedUrl("/admin/overview")).andReturn();
        var fresh = (MockHttpSession) result.getRequest().getSession(false);
        assertTrue(old.isInvalid());
        assertNotEquals(oldId, fresh.getId());
        mvc.perform(get("/admin/overview").session(fresh)).andExpect(status().isOk());
        // Übergabe-Token ist verbraucht; Session allein autorisiert keine JSON-API
        call(get("/admin/stats"), admin, null).andExpect(jsonPath("$.message").value("Token is wrong."));
        mvc.perform(get("/admin/stats").session(fresh)).andExpect(status().isBadRequest());
    }

    @Test void renderedDeleteFormHasCsrfAndGetDoesNotDelete() throws Exception {
        var s = session(adminToken());
        var html = mvc.perform(get("/admin/accounts/" + user + "/delete").session(s))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), html);
        assertTrue(users.existsById(user));
        mvc.perform(post("/admin/accounts/" + user + "/delete").session(s))
                .andExpect(status().isForbidden());
        assertTrue(users.existsById(user));
        mvc.perform(post("/admin/accounts/" + user + "/delete").session(s).param("_csrf", "wrong"))
                .andExpect(status().isForbidden());
        assertTrue(users.existsById(user));
        mvc.perform(post("/admin/accounts/" + user + "/delete").session(s)
                        .param("_csrf", matcher.group(1)))
                .andExpect(redirectedUrl("/admin/accounts"));
        assertFalse(users.existsById(user));
    }

    @Test void deleteCascadesAndOldUserTokenIsRejected() throws Exception {
        result(call(post("/marks"), token, Map.of("sessionId", session,
                "planExerciseId", exercise, "done", true, "weight", 4, "reps", 10)));
        List<Long> exercises = jdbc.queryForList("SELECT u.id FROM plan_uebung u JOIN einheit e "
                + "ON e.id = u.einheit_id WHERE e.plan_id = ?", Long.class, plan);
        List<Long> sessions = jdbc.queryForList("SELECT id FROM einheit WHERE plan_id = ?",
                Long.class, plan);
        String admin = adminToken();
        call(delete("/admin/users/" + user), admin, null).andExpect(status().isOk());
        assertFalse(users.existsById(user));
        assertFalse(profiles.existsById(user));
        assertFalse(plans.existsById(plan));
        for (Long id : sessions)
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM einheit WHERE id = ?",
                    Integer.class, id));
        for (Long id : exercises) {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM plan_uebung WHERE id = ?",
                    Integer.class, id));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM markierung WHERE plan_uebung_id = ?", Integer.class, id));
        }
        call(get("/profile"), token, null).andExpect(jsonPath("$.message").value("Token is wrong."));
    }

    @Test void adminAndUnknownIdCannotBeDeleted() throws Exception {
        var admin = adminToken();
        long id = cleanup.getLast();
        call(delete("/admin/users/" + id), admin, null)
                .andExpect(jsonPath("$.message").value("Admin cannot be deleted."));
        assertTrue(users.existsById(id));
        call(delete("/admin/users/9223372036854775807"), admin, null)
                .andExpect(jsonPath("$.message").value("User not found."));
    }

    @Test void statisticsExcludeAutomaticOldAndFutureMarks() throws Exception {
        var admin = adminToken();
        var before = result(call(get("/admin/stats"), admin, null));
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM plan_uebung WHERE einheit_id = ? ORDER BY position_nr", Long.class, session);
        // zählt: heute, vor 6 Tagen | zählt nicht: vor 7 Tagen, automatisch, morgen
        var dates = List.of(monday, monday.minusDays(6), monday.minusDays(7), monday, monday.plusDays(1));
        for (int i = 0; i < 5; i++) {
            jdbc.update("INSERT INTO markierung "
                            + "(plan_uebung_id, datum, geschafft, wiederholungen, automatisch, erstellt_am) "
                            + "VALUES (?, ?, false, 0, ?, ?)",
                    ids.get(i), dates.get(i), i == 3, Timestamp.from(time.now().toInstant()));
        }
        var after = result(call(get("/admin/stats"), admin, null));
        assertEquals(before.get("marksLastSevenDays").asLong() + 2,
                after.get("marksLastSevenDays").asLong());
        assertEquals(jdbc.queryForObject("SELECT COUNT(*) FROM nutzer WHERE rolle = 'USER'", Long.class),
                after.get("users").asLong());
        assertEquals(plans.count(), after.get("plans").asLong());
    }

    @Test void templatesEscapeNamesAndProtectAdminButton() throws Exception {
        jdbc.update("UPDATE nutzer SET name = ? WHERE id = ?", "<script>alert(1)</script>", user);
        var s = session(adminToken());
        String html = mvc.perform(get("/admin/accounts").session(s)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("Protected"));
    }

    @Test void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        var s = session(adminToken());
        mvc.perform(post("/admin/logout").session(s)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/overview").session(s)).andExpect(status().isOk());
        mvc.perform(post("/admin/logout").session(s).with(csrf()))
                .andExpect(redirectedUrl(FRONTEND + "/login?loggedOut=1"));
        assertTrue(s.isInvalid());
        mvc.perform(get("/admin/overview")).andExpect(redirectedUrl(FRONTEND + "/login"));
    }
}