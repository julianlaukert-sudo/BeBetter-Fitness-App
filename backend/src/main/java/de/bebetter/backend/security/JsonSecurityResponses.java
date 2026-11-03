package de.bebetter.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
//KI-Log Nr. 11
// JSON-Antworten für fehlende/ungültige Tokens und fehlende Rechte (Konvention: HTTP 400 + message)
public final class JsonSecurityResponses {

    private static final ObjectMapper JSON = new ObjectMapper();

    private JsonSecurityResponses() {
    }

    public static AuthenticationEntryPoint invalidToken() {
        return (request, response, exception) -> write(response, "Token is wrong.");
    }

    public static AccessDeniedHandler accessDenied() {
        return (request, response, exception) -> write(response, "Access denied.");
    }

    private static void write(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(Map.of("message", message)));
    }
}