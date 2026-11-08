package de.bebetter.backend.service;

import de.bebetter.backend.rules.PlanRules;
import java.math.BigDecimal;
import de.bebetter.backend.domain.ExerciseMark;
import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.domain.TrainingSession;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.TrainingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// KI-Log Nr. 15, 17, 31
/**
 * Wochenwechsel: alte Woche abschließen, dann kopieren (bei 4–5 „Done" je Einheit mit +5 % Gewicht)
 * oder nach Profiländerung neu planen.
 * Schlägt die Neuplanung fehl (z. B. ExerciseDB), wird alles zurückgerollt.
 */
@Service
public class WeekChangeService {

    private final ProfileRepository profiles;
    private final TrainingPlanRepository plans;
    private final TimeProvider time;
    private final PlanGenerator generator;

    public WeekChangeService(ProfileRepository profiles, TrainingPlanRepository plans,
                             TimeProvider time, PlanGenerator generator) {
        this.profiles = profiles;
        this.plans = plans;
        this.time = time;
        this.generator = generator;
    }

    @Transactional
    public void update(Long userId) {
        var profile = profiles.findForPlan(userId).orElse(null);   // Profilsperre
        if (profile == null) return;

        TrainingPlan last = plans.findFirstByUserIdOrderByWeekStartDesc(userId).orElse(null);
        var monday = PlanGenerator.mondayOf(time.today());

        while (last != null && last.getWeekStart().isBefore(monday)) {
            // 1) Abschluss: nur offene Übungen teilweise markierter Einheiten automatisch "nicht geschafft"
            for (TrainingSession s : last.getSessions()) {
                if (s.markedCount() == 0) continue;
                for (PlanExercise e : s.getExercises()) {
                    if (e.getMark() == null) {
                        e.setMark(new ExerciseMark(last.getWeekEnd(), false, null, 0, true,
                                time.now()));
                    }
                }
            }
            last.setEvaluated(true);

            var start = last.getWeekStart().plusWeeks(1);
            TrainingPlan next;
            if (profile.getReplanFrom() != null && !start.isBefore(profile.getReplanFrom())) {
                // 2a) Profiländerung fällig: neuer Plan nach Erstplanregeln (braucht ExerciseDB)
                next = generator.build(profile, start);
                profile.setReplanFrom(null);
            } else {
                // 2b) Normalfall: Vorgaben kopieren, Gewicht nur bei Progression erhöhen
                next = new TrainingPlan(userId, start, time.now());
                for (TrainingSession s : last.getSessions()) {
                    var copy = new TrainingSession(s.getLabel(), s.getPosition(),
                            s.getDate().plusWeeks(1));
                    copy.setCarried(s.markedCount() == 0);
                    // KI-Log Nr. 31: 4 oder 5 von 5 „Done" -> Gewicht +5 %, sonst unverändert
                    boolean progress = PlanRules.progresses(s.doneCount());
                    for (PlanExercise e : s.getExercises()) {
                        BigDecimal weight = progress
                                ? PlanRules.progressedWeight(e.getWeightKg())
                                : e.getWeightKg();
                        copy.addExercise(new PlanExercise(e.getPosition(), e.getExerciseId(),
                                e.getBodyPart(), e.getSets(), e.getReps(), weight));
                    }
                    next.addSession(copy);
                }
            }
            last = plans.saveAndFlush(next);
        }
    }
}