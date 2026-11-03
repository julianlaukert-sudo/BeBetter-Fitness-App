package de.bebetter.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.security.JsonSecurityResponses;
import de.bebetter.backend.security.TokenBlocklist;
import de.bebetter.backend.security.TokenService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//KI-Log Nr. 11, 18
@Configuration
public class SecurityConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Berlin"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Schlüssel kommt aus der Umgebungsvariable JWT_SECRET (nie im Repository)
    @Bean
    SecretKey jwtKey(@Value("${JWT_SECRET}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes long");
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtKey,
            TokenBlocklist blocklist,
            UserRepository users) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Prüft, ob das JWT auf der Blocklist steht.
        OAuth2TokenValidator<Jwt> notBlocked = jwt ->
                blocklist.isBlocked(jwt.getId())
                        ? OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token"))
                        : OAuth2TokenValidatorResult.success();

        //Konto muss noch existieren und die Rolle zum Token passen.
        OAuth2TokenValidator<Jwt> activeAccount = jwt -> {
            try {
                boolean active = users.findById(
                                Long.valueOf(jwt.getSubject()))
                        .map(u -> u.getRole().name().equals(
                                jwt.getClaimAsString("role")))
                        .orElse(false);

                return active
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token"));

            } catch (NumberFormatException ex) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token"));
            }
        };

        // Standardvalidierung, Blocklist und Kontoprüfung.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(
                                TokenService.ISSUER),
                        notBlocked,
                        activeAccount));

        return decoder;
    }

    // Claim "role" (USER/ADMIN) wird zur Spring-Rolle ROLE_USER/ROLE_ADMIN.
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminPages(
            HttpSecurity http,
            @Value("${FRONTEND_URL:http://localhost:5173}")
            String frontendUrl) throws Exception {

        String frontend = frontendUrl.replaceAll("/+$", "");

        http.securityMatcher(
                        "/admin",
                        "/admin/",
                        "/admin/overview",
                        "/admin/accounts",
                        "/admin/accounts/**",
                        "/admin/sso",
                        "/admin/logout",
                        "/admin/admin.css")
                .securityContext(context -> context
                        .securityContextRepository(
                                new HttpSessionSecurityContextRepository()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/admin/sso",
                                "/admin/admin.css")
                        .permitAll()
                        .anyRequest().hasRole("ADMIN"))
                // CSRF bleibt aktiv; nur /admin/sso ist ausgenommen.
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/admin/sso"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        response.sendRedirect(
                                                frontend + "/login"))
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(403)))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl(
                                frontend + "/login?loggedOut=1")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET, "/health")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST, "/users", "/auth")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/graphql").hasRole("ADMIN")
                        .requestMatchers(
                                "/plans",
                                "/plans/**",
                                "/marks",
                                "/profile")
                        .hasRole("USER")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter))
                        .authenticationEntryPoint(
                                JsonSecurityResponses.invalidToken())
                        .accessDeniedHandler(
                                JsonSecurityResponses.accessDenied()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                JsonSecurityResponses.invalidToken())
                        .accessDeniedHandler(
                                JsonSecurityResponses.accessDenied()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${FRONTEND_URL:http://localhost:5173}")
            String frontendUrl) {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}