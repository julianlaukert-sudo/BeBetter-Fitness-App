package de.bebetter.backend.controller;

import de.bebetter.backend.dto.LoginRequest;
import de.bebetter.backend.dto.LoginResponse;
import de.bebetter.backend.dto.MessageResponse;
import de.bebetter.backend.dto.RegistrationRequest;
import de.bebetter.backend.dto.RegistrationResponse;
import de.bebetter.backend.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users")
    public RegistrationResponse register(@RequestBody(required = false) RegistrationRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth")
    public LoginResponse login(@RequestBody(required = false) LoginRequest request) {
        return authService.login(request);
    }

    @DeleteMapping("/auth")
    public MessageResponse logout(@AuthenticationPrincipal Jwt jwt) {
        return authService.logout(jwt);
    }
}