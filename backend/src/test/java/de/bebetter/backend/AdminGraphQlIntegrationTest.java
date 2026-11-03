package de.bebetter.backend;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// KI-Log Nr. 18
class AdminGraphQlIntegrationTest extends Sprint5Support {

    // GraphQL antwortet ggf. asynchron -> bei Bedarf asyncDispatch
    JsonNode graphql(String bearer, String query) throws Exception {
        MvcResult first = mvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("query", query))))
                .andReturn();
        MvcResult done = first.getRequest().isAsyncStarted()
                ? mvc.perform(asyncDispatch(first)).andReturn()
                : first;
        assertEquals(200, done.getResponse().getStatus());
        return json.readTree(done.getResponse().getContentAsString());
    }

    @Test void userTokenIsRejected() throws Exception {
        call(post("/graphql"), token, Map.of("query", "{ adminStats { users } }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test void missingTokenIsRejected() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ adminStats { users } }\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token is wrong."));
    }

    @Test void adminCanQueryUsersAndStats() throws Exception {
        var result = graphql(adminToken(),
                "{ adminStats { users plans marksLastSevenDays } adminUsers { userId email role } }");
        assertTrue(result.path("errors").isMissingNode() || result.path("errors").isEmpty());
        assertTrue(result.at("/data/adminStats/users").isNumber());
        boolean found = false;
        for (var u : result.at("/data/adminUsers")) {
            assertFalse(u.has("passwordHash"));
            if (u.get("userId").asLong() == user) found = true;
        }
        assertTrue(found);
    }

    @Test void deleteMutationUsesSameRules() throws Exception {
        String admin = adminToken();
        long adminId = cleanup.getLast();
        var deleted = graphql(admin, "mutation { deleteUser(id: \"" + user + "\") { message userId } }");
        assertEquals("User deleted.", deleted.at("/data/deleteUser/message").asText());
        assertFalse(users.existsById(user));
        var protectedAdmin = graphql(admin, "mutation { deleteUser(id: \"" + adminId + "\") { message } }");
        assertEquals("Admin cannot be deleted.", protectedAdmin.at("/errors/0/message").asText());
        assertTrue(users.existsById(adminId));
    }
}