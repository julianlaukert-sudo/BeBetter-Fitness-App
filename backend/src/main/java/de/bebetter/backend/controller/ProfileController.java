package de.bebetter.backend.controller;

import de.bebetter.backend.dto.ProfileRequest;
import de.bebetter.backend.dto.ProfileResponse;
import de.bebetter.backend.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    // Die Nutzer-ID kommt ausschließlich aus dem geprüften JWT (Subject).
    @GetMapping("/profile")
    public ProfileResponse load(@AuthenticationPrincipal Jwt jwt) {
        return profiles.load(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/profile")
    public ProfileResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @RequestBody ProfileRequest request) {
        return profiles.create(Long.valueOf(jwt.getSubject()), request);
    }
}