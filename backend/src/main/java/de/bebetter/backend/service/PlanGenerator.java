package de.bebetter.backend.service;

import de.bebetter.backend.domain.PlanExercise;
import de.bebetter.backend.domain.Profile;
import de.bebetter.backend.domain.TrainingPlan;
import de.bebetter.backend.domain.TrainingSession;
import de.bebetter.backend.domain.Weekday;
import de.bebetter.backend.exercisedb.CatalogExercise;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.exercisedb.ExerciseDbException;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.rules.EquipmentCategory;
import de.bebetter.backend.rules.ExerciseSelection;
import de.bebetter.backend.rules.PlanRules;
import de.bebetter.backend.web.ApiException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanGenerator {

    private final ExerciseDbClient api;
    private final ProfileRepository profiles;
    private final TrainingPlanRepository plans;
    private final TimeProvider time;

    public PlanGenerator(ExerciseDbClient api, ProfileRepository profiles,
                         TrainingPlanRepository plans, TimeProvider time) {
        this.api = api;
        this.profiles = profiles;
        this.plans = plans;
        this.time = time;
    }

    public static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Erster Plan eines Nutzers (POST /plans). */
    @Transactional
    public TrainingPlan create(Long userId) {
        Profile profile = profiles.findForPlan(userId)
                .orElseThrow(() -> new ApiException("Profile not found."));
        LocalDate monday = mondayOf(time.today());

        var existing = plans.findByUserIdAndWeekStart(userId, monday);
        if (existing.isPresent()) return existing.get();   // idempotent
        if (plans.existsByUserId(userId))
            throw new ApiException("Plan week is not current.");

        TrainingPlan plan = build(profile, monday);
        profile.setReplanFrom(null);   // erster Plan nutzt bereits das aktuelle Profil
        return plans.saveAndFlush(plan);
    }

    /** Baut einen neuen Plan nach den Sprint-3-Regeln (Erstplan und Profil-Neuplanung). */
    public TrainingPlan build(Profile profile, LocalDate monday) {
        Map<String, List<CatalogExercise>> candidates = api.candidates(PlanRules.BODY_PARTS);
        Map<String, List<CatalogExercise>> selected;
        try {
            selected = ExerciseSelection.select(candidates, profile.getFitnessLevel(), 3);
        } catch (ExerciseSelection.NotEnoughExercisesException ex) {
            throw new ExerciseDbException("Not enough suitable exercises.", ex);
        }

        TrainingPlan plan = new TrainingPlan(profile.getUserId(), monday, time.now());
        // Tage nach Wochentag sortieren, damit A/B/C in Wochenreihenfolge liegen
        List<Weekday> days = profile.getTrainingDayList().stream().sorted().toList();
        for (int i = 0; i < 3; i++) {
            LocalDate date = monday.plusDays(days.get(i).toDayOfWeek().getValue() - 1);
            var session = new TrainingSession(PlanRules.LABELS.get(i), i + 1, date);
            int position = 1;
            for (String part : PlanRules.BODY_PARTS) {
                CatalogExercise u = selected.get(part).get(i);
                session.addExercise(new PlanExercise(position++, u.exerciseId(), part,
                        PlanRules.sets(profile.getGoal()), PlanRules.reps(profile.getGoal()),
                        PlanRules.startWeight(EquipmentCategory.of(u.firstEquipment()), profile)));
            }
            plan.addSession(session);
        }
        return plan;
    }
}