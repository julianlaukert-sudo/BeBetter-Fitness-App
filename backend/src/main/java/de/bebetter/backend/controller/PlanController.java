package de.bebetter.backend.controller;

import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.dto.PlanCreatedResponse;
import de.bebetter.backend.dto.PlanResponse;
import de.bebetter.backend.service.PlanGenerator;
import de.bebetter.backend.service.PlanService;
import de.bebetter.backend.service.WeekChangeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
// KI-Log Nr. 12, 15
@RestController
public class PlanController {

    private final PlanGenerator generator;
    private final PlanService plans;
    private final WeekChangeService weekChange;

    public PlanController(PlanGenerator generator, PlanService plans,
                          WeekChangeService weekChange) {
        this.generator = generator;
        this.plans = plans;
        this.weekChange = weekChange;
    }

    @PostMapping("/plans")
    public PlanCreatedResponse create(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        weekChange.update(userId);   // verpasste Wochen zuerst nachholen
        TrainingPlan plan = generator.create(userId);
        return new PlanCreatedResponse("Plan created successfully.", plan.getId(),
                plan.getWeekStart());
    }

    @GetMapping("/plans/current")
    public PlanResponse current(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        weekChange.update(userId);   // verpasste Wochen zuerst nachholen
        return plans.current(userId);
    }
}