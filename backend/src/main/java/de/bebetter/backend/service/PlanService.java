package de.bebetter.backend.service;

import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.dto.PlanResponse;
import de.bebetter.backend.exercisedb.CatalogExercise;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.web.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

    private final TrainingPlanRepository plans;
    private final ExerciseDbClient api;
    private final Clock clock;

    public PlanService(TrainingPlanRepository plans, ExerciseDbClient api, Clock clock) {
        this.plans = plans;
        this.api = api;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PlanResponse current(Long userId) {
        LocalDate today = LocalDate.now(clock);
        var plan = plans.findByUserIdAndWeekStart(userId, PlanGenerator.mondayOf(today))
                .orElseThrow(() -> new ApiException("No plan found."));
        var ids = new LinkedHashSet<String>();
        plan.getSessions().forEach(s -> s.getExercises().forEach(e -> ids.add(e.getExerciseId())));
        var data = api.exercises(ids);   // fehlende Details -> null-Felder, Plan bleibt
        var sessions = plan.getSessions().stream().map(s -> new PlanResponse.Session(
                s.getId(), s.getLabel(), s.getDate(), s.getExercises().stream()
                .map(e -> exercise(e, data.get(e.getExerciseId()))).toList())).toList();
        return new PlanResponse(plan.getId(), plan.getWeekStart(), plan.getWeekEnd(),
                today, data.keySet().containsAll(ids), sessions);
    }

    private PlanResponse.Exercise exercise(PlanExercise e, CatalogExercise x) {
        return new PlanResponse.Exercise(e.getId(), e.getExerciseId(),
                x == null ? null : x.name(), e.getBodyPart(),
                x == null ? null : x.firstTargetMuscle(), x == null ? null : x.firstEquipment(),
                x == null ? null : x.gifUrl(), e.getSets(), e.getReps(),
                e.getWeightKg(), e.isBodyweight());
    }
}