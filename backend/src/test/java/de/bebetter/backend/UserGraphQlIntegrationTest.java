
package de.bebetter.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;

// KI-Log Nr. 28
class UserGraphQlIntegrationTest extends Sprint5Support {

    @Autowired
    ApplicationContext context;

    record Account(long id, String token) {}

    private static final String PROFILE = """
            {
              "personal": {
                "age": 23,
                "gender": "male",
                "height": 180,
                "weight": 78.5
              },
              "training": {
                "goal": "health",
                "fitnessLevel": "beginner",
                "trainingDays": ["MON", "WED", "FRI"]
              }
            }
            """;

    private static final String PROFILE_QUERY = """
            {
              myProfile {
                profileId
                personal {
                  age
                  gender
                  height
                  weight
                }
                training {
                  goal
                  fitnessLevel
                  trainingDays
                }
                createdAt
                updatedAt
                planExists
                nextPlanFrom
              }
            }
            """;

    private static final String PLAN_QUERY = """
            {
              currentPlan {
                planId
                weekStart
                weekEnd
                today
                exerciseDataAvailable
                sessions {
                  sessionId
                  label
                  date
                  status
                  available
                  carried
                  exercises {
                    planExerciseId
                    exerciseId
                    name
                    bodyPart
                    targetMuscle
                    equipment
                    imageUrl
                    sets
                    reps
                    weight
                    bodyweight
                    mark {
                      id
                      date
                      done
                      weight
                      reps
                      automatic
                    }
                  }
                }
              }
            }
            """;

    /*
     * GraphQL-Aufruf mit Unterstützung für synchrone
     * und asynchrone MockMvc-Ausführung.
     */
    private JsonNode graphql(String bearer, String query) throws Exception {

        MvcResult first = mvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("query", query)
                        )))
                .andReturn();

        MvcResult done = first.getRequest().isAsyncStarted()
                ? mvc.perform(asyncDispatch(first)).andReturn()
                : first;

        assertEquals(200, done.getResponse().getStatus());

        return json.readTree(
                done.getResponse().getContentAsString()
        );
    }

    /*
     * Zweiten Benutzer wie in PlanIntegrationTest anlegen.
     * Der Benutzer wird in cleanup der Sprint5Support-Basisklasse
     * eingetragen und nach dem Test gelöscht.
     */
    private Account createSecondUser(boolean withProfile) throws Exception {

        String email = UUID.randomUUID() + "@example.org";

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "name", "GraphQL Test",
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk());

        JsonNode auth = json.readTree(
                mvc.perform(post("/auth")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(Map.of(
                                        "email", email,
                                        "password", PASSWORD
                                ))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        Account account = new Account(
                auth.get("userId").asLong(),
                auth.get("token").asText()
        );

        cleanup.add(account.id());

        if (withProfile) {
            mvc.perform(post("/profile")
                            .header("Authorization", "Bearer " + account.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PROFILE))
                    .andExpect(status().isOk());
        }

        return account;
    }

    /*
     * Für den zweiten Benutzer einen eigenen Trainingsplan erzeugen.
     */
    private JsonNode createPlan(Account account) throws Exception {

        String response = mvc.perform(post("/plans")
                        .header("Authorization", "Bearer " + account.token()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(response);
    }

    /*
     * Vergleicht REST- und GraphQL-Ergebnisse rekursiv.
     * Zahlen werden numerisch verglichen, damit beispielsweise
     * 3 und 3.0 nicht fälschlich als unterschiedlich gelten.
     */
    private void assertJsonEquivalent(JsonNode expected, JsonNode actual) {

        if (expected == null || expected.isNull()) {
            assertTrue(actual == null || actual.isNull());
            return;
        }

        if (expected.isNumber() && actual.isNumber()) {
            assertEquals(
                    0,
                    expected.decimalValue().compareTo(actual.decimalValue())
            );
            return;
        }

        if (expected.isObject()) {
            assertTrue(actual.isObject());

            expected.fieldNames().forEachRemaining(field -> {
                assertTrue(actual.has(field), "Feld fehlt: " + field);
                assertJsonEquivalent(expected.get(field), actual.get(field));
            });

            return;
        }

        if (expected.isArray()) {
            assertTrue(actual.isArray());
            assertEquals(expected.size(), actual.size());

            for (int i = 0; i < expected.size(); i++) {
                assertJsonEquivalent(expected.get(i), actual.get(i));
            }

            return;
        }

        assertEquals(expected.asText(), actual.asText());
    }

    /*
     * T1 – Nutzer liest sein eigenes Profil.
     * GraphQL-Ergebnis wird mit REST GET /profile verglichen.
     */
    @Test
    void userCanReadOwnProfileAndGraphQlMatchesRest() throws Exception {

        JsonNode rest = json.readTree(
                mvc.perform(get("/profile")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        JsonNode graphQl = graphql(token, PROFILE_QUERY);

        assertFalse(graphQl.has("errors"), "GraphQL enthält Fehler");
        assertTrue(graphQl.has("data"));
        assertTrue(graphQl.get("data").has("myProfile"));

        JsonNode profile = graphQl.at("/data/myProfile");

        assertFalse(profile.isMissingNode());

        assertEquals(
                String.valueOf(user),
                profile.get("profileId").asText()
        );

        assertJsonEquivalent(rest, profile);
    }

    /*
     * T2 – Nutzer liest seinen eigenen Trainingsplan.
     * GraphQL-Ergebnis wird mit REST GET /plans/current verglichen.
     */
    @Test
    void userCanReadOwnCurrentPlanAndGraphQlMatchesRest() throws Exception {

        JsonNode rest = json.readTree(
                mvc.perform(get("/plans/current")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );

        JsonNode graphQl = graphql(token, PLAN_QUERY);

        assertFalse(graphQl.has("errors"), "GraphQL enthält Fehler");
        assertTrue(graphQl.has("data"));
        assertTrue(graphQl.get("data").has("currentPlan"));

        JsonNode currentPlan = graphQl.at("/data/currentPlan");

        assertFalse(currentPlan.isMissingNode());

        assertEquals(
                String.valueOf(plan),
                currentPlan.get("planId").asText()
        );

        assertJsonEquivalent(rest, currentPlan);
    }

    /*
     * T3 – Zwei Nutzer erhalten ausschließlich ihre eigenen Daten.
     */
    @Test
    void twoUsersCanOnlyReadTheirOwnProfileAndPlan() throws Exception {

        Account second = createSecondUser(true);
        JsonNode secondPlan = createPlan(second);

        JsonNode firstProfileResult = graphql(token, PROFILE_QUERY);
        JsonNode secondProfileResult = graphql(second.token(), PROFILE_QUERY);

        JsonNode firstPlanResult = graphql(token, PLAN_QUERY);
        JsonNode secondPlanResult = graphql(second.token(), PLAN_QUERY);

        assertFalse(firstProfileResult.has("errors"));
        assertFalse(secondProfileResult.has("errors"));
        assertFalse(firstPlanResult.has("errors"));
        assertFalse(secondPlanResult.has("errors"));

        String firstProfileId = firstProfileResult
                .at("/data/myProfile/profileId").asText();

        String secondProfileId = secondProfileResult
                .at("/data/myProfile/profileId").asText();

        String firstPlanId = firstPlanResult
                .at("/data/currentPlan/planId").asText();

        String secondPlanId = secondPlanResult
                .at("/data/currentPlan/planId").asText();

        assertEquals(String.valueOf(user), firstProfileId);
        assertEquals(String.valueOf(second.id()), secondProfileId);

        assertEquals(String.valueOf(plan), firstPlanId);
        assertEquals(secondPlan.get("planId").asText(), secondPlanId);

        assertNotEquals(firstProfileId, secondProfileId);
        assertNotEquals(firstPlanId, secondPlanId);
    }

    /*
     * T4 – ADMIN darf die Nutzer-Queries nicht aufrufen.
     */
    @Test
    void adminCannotReadUserProfileOrCurrentPlan() throws Exception {

        String admin = adminToken();

        JsonNode profileResult = graphql(
                admin,
                "{ myProfile { profileId } }"
        );

        JsonNode planResult = graphql(
                admin,
                "{ currentPlan { planId } }"
        );

        assertTrue(
                profileResult.has("errors"),
                "ADMIN darf myProfile nicht aufrufen"
        );

        assertTrue(
                planResult.has("errors"),
                "ADMIN darf currentPlan nicht aufrufen"
        );
    }

    /*
     * T5 – USER darf keine Admin-Funktion ausführen.
     * Zusätzlich wird geprüft, dass deleteUser den Datensatz
     * tatsächlich nicht löscht.
     */
    @Test
    void userCannotCallAdminQueriesOrDeleteUsers() throws Exception {

        long countBefore = users.count();
        assertTrue(users.existsById(user));

        JsonNode usersResult = graphql(
                token,
                "{ adminUsers { userId name email } }"
        );

        JsonNode statsResult = graphql(
                token,
                "{ adminStats { users plans marksLastSevenDays } }"
        );

        JsonNode deleteResult = graphql(
                token,
                """
                mutation {
                  deleteUser(id: "%s") {
                    message
                    userId
                  }
                }
                """.formatted(user)
        );

        assertTrue(usersResult.has("errors"));
        assertTrue(statsResult.has("errors"));
        assertTrue(deleteResult.has("errors"));

        assertTrue(
                users.existsById(user),
                "Der Testbenutzer darf nicht gelöscht worden sein"
        );

        assertEquals(
                countBefore,
                users.count(),
                "Die Anzahl der Benutzer darf sich nicht ändern"
        );
    }

    /*
     * T6 – Ohne Token bleibt die vorhandene Security-Antwort bestehen.
     * Dieser Request wird vor dem GraphQL-Resolver abgefangen.
     */
    @Test
    void missingTokenReturnsExistingSecurityResponse() throws Exception {

        MvcResult result = mvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"{ myProfile { profileId } }"}
                                """))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());

        JsonNode body = json.readTree(
                result.getResponse().getContentAsString()
        );

        assertEquals("Token is wrong.", body.get("message").asText());
    }

    /*
     * T7 – Ein angemeldeter Nutzer ohne Trainingsplan.
     */
    @Test
    void userWithoutPlanGetsGraphQlError() throws Exception {

        Account second = createSecondUser(true);

        JsonNode result = graphql(
                second.token(),
                "{ currentPlan { planId } }"
        );

        assertTrue(result.has("errors"));

        String errors = result.get("errors").toString();

        assertTrue(
                errors.contains("No plan found."),
                "Die Fehlermeldung muss 'No plan found.' enthalten. Tatsächlich: "
                        + errors
        );

        // currentPlan ist im Schema non-null.
        // Der Fehler kann deshalb bis zum Root-Feld data propagieren.
        assertTrue(
                result.get("data") == null || result.get("data").isNull()
                        || result.get("data").isObject()
        );
    }

    /*
     * T8 – Körpergewichtsübung mit weight = null.
     */
    @Test
    void bodyweightExerciseCanHaveNullWeight() throws Exception {

        jdbc.update(
                "UPDATE plan_uebung SET gewicht_kg = NULL WHERE id = ?",
                exercise
        );

        JsonNode result = graphql(
                token,
                """
                {
                  currentPlan {
                    sessions {
                      exercises {
                        planExerciseId
                        weight
                        bodyweight
                      }
                    }
                  }
                }
                """
        );

        assertFalse(
                result.has("errors"),
                "Nullable weight darf keinen GraphQL-Fehler verursachen: "
                        + result.get("errors")
        );

        JsonNode exercises = result.at(
                "/data/currentPlan/sessions/0/exercises"
        );

        assertTrue(exercises.isArray());
        assertFalse(exercises.isEmpty());

        JsonNode firstExercise = exercises.get(0);

        assertTrue(
                firstExercise.get("weight").isNull(),
                "weight muss null sein"
        );

        assertTrue(
                firstExercise.get("bodyweight").asBoolean(),
                "bodyweight muss true sein"
        );
    }

    /*
     * T9 – Jede GraphQL-Mapping-Methode muss durch
     * @PreAuthorize auf Klassen- oder Methodenebene geschützt sein.
     */
    @Test
    void everyGraphQlMappingHasMethodOrClassAuthorization() {

        boolean foundMapping = false;

        for (Object bean : context.getBeansWithAnnotation(Controller.class).values()) {

            Class<?> type = AopUtils.getTargetClass(bean);

            boolean classProtected = AnnotatedElementUtils
                    .hasAnnotation(type, PreAuthorize.class);

            for (Method method : ReflectionUtils.getAllDeclaredMethods(type)) {

                boolean graphQlMapping =
                        AnnotatedElementUtils.hasAnnotation(
                                method, QueryMapping.class)
                                || AnnotatedElementUtils.hasAnnotation(
                                method, MutationMapping.class)
                                || AnnotatedElementUtils.hasAnnotation(
                                method, SchemaMapping.class);

                if (!graphQlMapping) {
                    continue;
                }

                foundMapping = true;

                boolean methodProtected = AnnotatedElementUtils
                        .hasAnnotation(method, PreAuthorize.class);

                assertTrue(
                        classProtected || methodProtected,
                        () -> type.getName() + "#"
                                + method.getName()
                                + " hat keine @PreAuthorize-Absicherung"
                );
            }
        }

        assertTrue(
                foundMapping,
                "Es wurden keine GraphQL-Mapping-Methoden gefunden"
        );
    }
}
