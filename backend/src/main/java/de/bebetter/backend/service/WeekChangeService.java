package de.bebetter.backend.service;

import de.bebetter.backend.domain.ExerciseMark;
import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.domain.TrainingSession;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.TrainingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class WeekChangeService {

    private final ProfileRepository profiles;
    private final TrainingPlanRepository plans;
    private final TimeProvider time;

    public WeekChangeService(ProfileRepository profiles, TrainingPlanRepository plans,
                             TimeProvider time) {
        this.profiles = profiles;
        this.plans = plans;
        this.time = time;
    }

    @Transactional
    public void update(Long userId) {
        // Dieselbe Profilsperre wie Planerstellung und Markierung
        if (profiles.findForPlan(userId).isEmpty()) return;

        TrainingPlan last = plans.findFirstByUserIdOrderByWeekStartDesc(userId).orElse(null);
        var monday = PlanGenerator.mondayOf(time.today());

        while (last != null && last.getWeekStart().isBefore(monday)) {
            // 1) Abschluss: nur offene Übungen TEILWEISE markierter Einheiten automatisch "nicht geschafft"
            for (TrainingSession s : last.getSessions()) {
                if (s.markedCount() == 0) continue;   // ganz unmarkiert -> keine erfundenen Ergebnisse
                for (PlanExercise e : s.getExercises()) {
                    if (e.getMark() == null) {
                        e.setMark(new ExerciseMark(last.getWeekEnd(), false, null, 0, true,
                                time.now()));
                    }
                }
            }
            last.setEvaluated(true);

            // 2) Kopie: gleiche Vorgaben, neue IDs, keine Markierungen, keine Gewichtsanpassung
            var next = new TrainingPlan(userId, last.getWeekStart().plusWeeks(1), time.now());
            for (TrainingSession s : last.getSessions()) {
                var copy = new TrainingSession(s.getLabel(), s.getPosition(),
                        s.getDate().plusWeeks(1));
                copy.setCarried(s.markedCount() == 0);
                for (PlanExercise e : s.getExercises()) {
                    copy.addExercise(new PlanExercise(e.getPosition(), e.getExerciseId(),
                            e.getBodyPart(), e.getSets(), e.getReps(), e.getWeightKg()));
                }
                next.addSession(copy);
            }
            last = plans.saveAndFlush(next);   // weiter, bis die aktuelle Woche erreicht ist
        }
    }
}