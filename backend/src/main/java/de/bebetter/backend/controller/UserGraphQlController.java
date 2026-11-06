package de.bebetter.backend.controller;

import de.bebetter.backend.dto.MarkResponse;
import de.bebetter.backend.dto.PlanResponse;
import de.bebetter.backend.dto.ProfileRequest;
import de.bebetter.backend.dto.ProfileResponse;
import de.bebetter.backend.service.PlanService;
import de.bebetter.backend.service.ProfileService;
import de.bebetter.backend.service.WeekChangeService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

// KI-Log Nr. 28
@Controller
@PreAuthorize("hasRole('USER')")
public class UserGraphQlController {

    private final ProfileService profiles;
    private final PlanService plans;
    private final WeekChangeService weekChange;

    public UserGraphQlController(
            ProfileService profiles,
            PlanService plans,
            WeekChangeService weekChange) {
        this.profiles = profiles;
        this.plans = plans;
        this.weekChange = weekChange;
    }

    @QueryMapping
    public ProfileResponse myProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return profiles.load(userId);
    }

    @QueryMapping
    public PlanResponse currentPlan(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());

        // Wie GET /plans/current: veraltete Wochen zuerst nachholen.
        weekChange.update(userId);

        return plans.current(userId);
    }

    // REST liefert für diese Java-Enums deren @JsonValue-Code.
    // GraphQL gibt deshalb dieselben String-Werte explizit zurück.
    @SchemaMapping(typeName = "Personal", field = "gender")
    public String gender(ProfileRequest.Personal source) {
        return source.gender().code();
    }

    @SchemaMapping(typeName = "Training", field = "goal")
    public String goal(ProfileRequest.Training source) {
        return source.goal().code();
    }

    @SchemaMapping(typeName = "Training", field = "fitnessLevel")
    public String fitnessLevel(ProfileRequest.Training source) {
        return source.fitnessLevel().code();
    }

    @SchemaMapping(typeName = "Training", field = "trainingDays")
    public List<String> trainingDays(ProfileRequest.Training source) {
        return source.trainingDays().stream().map(Enum::name).toList();
    }

    // LocalDate-Felder explizit als ISO-8601-String ausgeben.
    @SchemaMapping(typeName = "Profile", field = "createdAt")
    public String profileCreatedAt(ProfileResponse source) {
        return source.createdAt().toString();
    }

    @SchemaMapping(typeName = "Profile", field = "updatedAt")
    public String profileUpdatedAt(ProfileResponse source) {
        return source.updatedAt().toString();
    }

    @SchemaMapping(typeName = "Profile", field = "nextPlanFrom")
    public String profileNextPlanFrom(ProfileResponse source) {
        return source.nextPlanFrom() == null
                ? null : source.nextPlanFrom().toString();
    }

    @SchemaMapping(typeName = "TrainingPlan", field = "weekStart")
    public String weekStart(PlanResponse source) {
        return source.weekStart().toString();
    }

    @SchemaMapping(typeName = "TrainingPlan", field = "weekEnd")
    public String weekEnd(PlanResponse source) {
        return source.weekEnd().toString();
    }

    @SchemaMapping(typeName = "TrainingPlan", field = "today")
    public String today(PlanResponse source) {
        return source.today().toString();
    }

    @SchemaMapping(typeName = "TrainingSession", field = "date")
    public String sessionDate(PlanResponse.Session source) {
        return source.date().toString();
    }

    @SchemaMapping(typeName = "ExerciseMark", field = "date")
    public String markDate(MarkResponse source) {
        return source.date().toString();
    }
}