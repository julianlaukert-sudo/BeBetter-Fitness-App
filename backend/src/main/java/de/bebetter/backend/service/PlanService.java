package de.bebetter.backend.service;

import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.domain.TrainingSession;
import de.bebetter.backend.dto.MarkResponse;
import de.bebetter.backend.dto.PlanResponse;
import de.bebetter.backend.exercisedb.CatalogExercise;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.web.ApiException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

    private final TrainingPlanRepository plans;
    private final ExerciseDbClient api;
    private final TimeProvider time;

    public PlanService(TrainingPlanRepository plans, ExerciseDbClient api, TimeProvider time) {
        this.plans = plans;
        this.api = api;
        this.time = time;
    }

    /** Einheit ist vom eigenen Tag bis Sonntag ihrer (nicht abgeschlossenen) Woche bearbeitbar. */
    public static boolean isEditable(TrainingPlan plan, TrainingSession session, LocalDate today) {
        return !plan.isEvaluated() && plan.contains(today) && !today.isBefore(session.getDate());
    }

    @Transactional(readOnly = true)
    public PlanResponse current(Long userId) {
        LocalDate today = time.today();
        var plan = plans.findByUserIdAndWeekStart(userId, PlanGenerator.mondayOf(today))
                .orElseThrow(() -> new ApiException("No plan found."));
        var ids = new LinkedHashSet<String>();
        plan.getSessions().forEach(s -> s.getExercises().forEach(e -> ids.add(e.getExerciseId())));
        var data = api.exercises(ids);   // fehlende Details -> null-Felder, Plan bleibt
        var sessions = plan.getSessions().stream().map(s -> new PlanResponse.Session(
                s.getId(), s.getLabel(), s.getDate(), status(s),
                isEditable(plan, s, today), s.isCarried(),
                s.getExercises().stream()
                        .map(e -> exercise(e, data.get(e.getExerciseId()))).toList())).toList();
        return new PlanResponse(plan.getId(), plan.getWeekStart(), plan.getWeekEnd(),
                today, data.keySet().containsAll(ids), sessions);
    }

    // Reine Anzeige – ändert die Abschlussregel nicht
    private String status(TrainingSession s) {
        long marked = s.markedCount();
        if (marked == 0) return "OPEN";
        if (marked < s.getExercises().size()) return "IN_PROGRESS";
        long done = s.getExercises().stream().filter(e -> e.getMark().isDone()).count();
        if (done == marked) return "DONE";
        return done == 0 ? "FAILED" : "PARTIAL";
    }

    private PlanResponse.Exercise exercise(PlanExercise e, CatalogExercise x) {
        return new PlanResponse.Exercise(e.getId(), e.getExerciseId(),
                x == null ? null : x.name(), e.getBodyPart(),
                x == null ? null : x.firstTargetMuscle(), x == null ? null : x.firstEquipment(),
                x == null ? null : x.gifUrl(), e.getSets(), e.getReps(),
                e.getWeightKg(), e.isBodyweight(), MarkResponse.of(e.getMark()));
    }
}