package de.bebetter.backend.controller;

import de.bebetter.backend.dto.MarkRequest;
import de.bebetter.backend.dto.MarkResponse;
import de.bebetter.backend.service.MarkService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarkController {

    private final MarkService marks;

    public MarkController(MarkService marks) {
        this.marks = marks;
    }

    // Nutzer-ID nur aus dem Token
    @PostMapping("/marks")
    public MarkResponse save(@AuthenticationPrincipal Jwt jwt, @RequestBody MarkRequest request) {
        return marks.save(Long.valueOf(jwt.getSubject()), request);
    }
}