package de.bebetter.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.security.TokenService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// KI-Log Nr. 11
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired TokenService tokens;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    static final String PASSWORD = "Secure123!";
    static final String PROFILE = """
        {"personal":{"age":23,"gender":"male","height":180,"weight":78.5},
         "training":{"goal":"health","fitnessLevel":"beginner",
                     "trainingDays":["MON","WED","FRI"]}}
        """;

    // ---- Helfer ----

    String email() { return UUID.randomUUID() + "@example.org"; }

    void register(String email) throws Exception {
        // "role":"ADMIN" im Request muss ignoriert werden
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Tester", "email", email,
                                "password", PASSWORD, "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    JsonNode login(String email) throws Exception {
        String result = mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(result);
    }

    String newToken() throws Exception {
        String email = email();
        register(email);
        return login(email).get("token").asText();
    }

    void save(String token, String body) throws Exception {
        mvc.perform(post("/profile").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planExists").value(false));
    }

    // ---- Registrierung und Login ----

    @Test void registrationStoresHashAndNormalizesEmail() throws Exception {
        String email = email();
        register(" " + email.toUpperCase() + " ");
        User user = users.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(email, user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertTrue(passwords.matches(PASSWORD, user.getPasswordHash()));
    }

    @Test void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        String email = email();
        register(email);
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Tester",
                                "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already exists."));
    }

    @Test void weakPasswordHasFieldError() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tester\",\"email\":\"a@b.de\",\"password\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").isString());
    }

    @Test void wrongAndUnknownLoginHaveSameMessage() throws Exception {
        String email = email();
        register(email);
        for (String address : new String[]{email, email()}) {
            mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", address, "password", "wrong"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email or password is wrong."));
        }
    }

    // ---- Token und Logout ----

    @Test void missingTokenIsRejected() throws Exception {
        mvc.perform(get("/profile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token is wrong."));
    }

    @Test void malformedAndExpiredTokensAreRejected() throws Exception {
        var claims = JwtClaimsSet.builder().issuer("bebetter").subject("1")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600)).build();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        for (String token : new String[]{"wrong", expired}) {
            mvc.perform(get("/profile").header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Token is wrong."));
        }
    }

    @Test void logoutBlocksOnlyTheUsedToken() throws Exception {
        String email = email();
        register(email);
        String a = login(email).get("token").asText();
        String b = login(email).get("token").asText();
        mvc.perform(delete("/auth").header("Authorization", "Bearer " + a))
                .andExpect(status().isOk());
        mvc.perform(get("/profile").header("Authorization", "Bearer " + a))
                .andExpect(jsonPath("$.message").value("Token is wrong."));
        mvc.perform(get("/profile").header("Authorization", "Bearer " + b))
                .andExpect(jsonPath("$.message").value("Profile not found."));
    }

    // ---- Profil ----

    @Test void profileCanBeCreatedAndReadAndLoginReflectsIt() throws Exception {
        String email = email();
        register(email);
        assertFalse(login(email).get("profileCompleted").asBoolean());
        String token = login(email).get("token").asText();
        save(token, PROFILE);
        mvc.perform(get("/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personal.weight").value(78.5))
                .andExpect(jsonPath("$.training.trainingDays[2]").value("FRI"));
        assertTrue(login(email).get("profileCompleted").asBoolean());
    }

    @Test void duplicateProfileDoesNotOverwrite() throws Exception {
        String token = newToken();
        save(token, PROFILE);
        mvc.perform(post("/profile").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE.replace("78.5", "90.0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Profile already exists."));
        mvc.perform(get("/profile").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.personal.weight").value(78.5));
    }

    @Test void otherUsersIdCannotSelectTheirProfile() throws Exception {
        String a = newToken();
        String b = newToken();
        save(a, PROFILE);
        save(b, PROFILE.replace("78.5", "90.0"));
        mvc.perform(get("/profile?userId=1").header("Authorization", "Bearer " + b))
                .andExpect(jsonPath("$.personal.weight").value(90.0));
    }

    @Test void invalidBoundsAndSundayMondayAreRejected() throws Exception {
        String token = newToken();
        String[] invalid = {
                PROFILE.replace("23", "15"), PROFILE.replace("180", "231"),
                PROFILE.replace("78.5", "78.55"), PROFILE.replace("FRI", "SUN") };
        for (String body : invalid) {
            mvc.perform(post("/profile").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid input."));
        }
    }

    @Test void nullAndInvalidEnumAreRejected() throws Exception {
        String token = newToken();
        for (String body : new String[]{"null", "{}", PROFILE.replace("beginner", "elite")}) {
            mvc.perform(post("/profile").header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---- Rolle, CORS, Migrationen ----

    @Test void adminRoleComesFromDatabase() throws Exception {
        String email = email();
        // Nur innerhalb dieser Testtransaktion – kein Admin-Initialisierer für den Betrieb
        User admin = users.saveAndFlush(new User("Admin Test", email,
                passwords.encode(PASSWORD), Role.ADMIN, OffsetDateTime.now()));
        assertEquals("ADMIN", login(email).get("role").asText());
        mvc.perform(post("/profile").header("Authorization", "Bearer " + tokens.create(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test void corsAllowsFrontendAuthorizationHeader() throws Exception {
        mvc.perform(options("/profile").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test void migrationsHaveRun() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history "
                + "WHERE version IN ('2','3') AND success = true", Integer.class);
        assertEquals(2, count);
    }
}