package de.bebetter.backend.controller;

import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.dto.PlanCreatedResponse;
import de.bebetter.backend.dto.PlanResponse;
import de.bebetter.backend.service.PlanGenerator;
import de.bebetter.backend.service.PlanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlanController {

    private final PlanGenerator generator;
    private final PlanService plans;

    public PlanController(PlanGenerator generator, PlanService plans) {
        this.generator = generator;
        this.plans = plans;
    }

    // Kein Request-Body, keine wählbare Nutzer-ID – nur das Token zählt
    @PostMapping("/plans")
    public PlanCreatedResponse create(@AuthenticationPrincipal Jwt jwt) {
        TrainingPlan plan = generator.create(Long.valueOf(jwt.getSubject()));
        return new PlanCreatedResponse("Plan created successfully.", plan.getId(),
                plan.getWeekStart());
    }

    @GetMapping("/plans/current")
    public PlanResponse current(@AuthenticationPrincipal Jwt jwt) {
        return plans.current(Long.valueOf(jwt.getSubject()));
    }
}