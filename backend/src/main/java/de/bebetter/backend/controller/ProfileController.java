package de.bebetter.backend.controller;

import de.bebetter.backend.dto.ProfileRequest;
import de.bebetter.backend.dto.ProfileResponse;
import de.bebetter.backend.service.ProfileService;
import de.bebetter.backend.service.WeekChangeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
//KI-Log Nr. 11,17
@RestController
public class ProfileController {

    private final ProfileService profiles;
    private final WeekChangeService weekChange;

    public ProfileController(ProfileService profiles, WeekChangeService weekChange) {
        this.profiles = profiles;
        this.weekChange = weekChange;
    }

    @GetMapping("/profile")
    public ProfileResponse load(@AuthenticationPrincipal Jwt jwt) {
        return profiles.load(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/profile")
    public ProfileResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @RequestBody ProfileRequest request) {
        return profiles.create(Long.valueOf(jwt.getSubject()), request);
    }

    // Erst verpasste Wochen mit dem ALTEN Profil nachholen, dann die Änderung speichern
    @PutMapping("/profile")
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                  @RequestBody ProfileRequest request) {
        Long userId = Long.valueOf(jwt.getSubject());
        weekChange.update(userId);
        return profiles.update(userId, request);
    }
}