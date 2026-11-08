package de.bebetter.backend.service;

import de.bebetter.backend.domain.ExerciseMark;
import de.bebetter.backend.dto.MarkRequest;
import de.bebetter.backend.dto.MarkResponse;
import de.bebetter.backend.repository.PlanExerciseRepository;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// KI-Log Nr. 15, 32
@Service
public class MarkService {

    private final ProfileRepository profiles;
    private final PlanExerciseRepository exercises;
    private final TimeProvider time;

    public MarkService(ProfileRepository profiles, PlanExerciseRepository exercises,
                       TimeProvider time) {
        this.profiles = profiles;
        this.exercises = exercises;
        this.time = time;
    }

    @Transactional
    public MarkResponse save(Long userId, MarkRequest r) {
        // KI-Log Nr. 32: nur noch geschafft / nicht geschafft, keine Istwerte mehr
        if (r == null || r.sessionId() == null || r.sessionId() < 1
                || r.planExerciseId() == null || r.planExerciseId() < 1
                || r.done() == null)
            throw new ApiException("Invalid training values.");

        // Dieselbe Profilsperre wie Planerstellung/Wochenwechsel: parallele Anfragen warten
        profiles.findForPlan(userId)
                .orElseThrow(() -> new ApiException("Profile not found."));

        // Fremde oder nicht passende Übung -> dieselbe Meldung wie unbekannte
        var exercise = exercises.findOwn(r.planExerciseId(), r.sessionId(), userId)
                .orElseThrow(() -> new ApiException("Exercise not found."));
        var session = exercise.getSession();
        var plan = session.getPlan();
        var today = time.today();
        if (!PlanService.isEditable(plan, session, today))
            throw new ApiException("Session is not editable.");

        // Spalten gewicht_kg/wiederholungen bleiben (V5 unverändert): immer null bzw. 0
        if (exercise.getMark() == null) {
            exercise.setMark(new ExerciseMark(today, r.done(), null, 0, false, time.now()));
        } else {
            exercise.getMark().update(today, r.done(), null, 0); // gleiche ID
        }
        exercises.flush();
        return MarkResponse.of(exercise.getMark());
    }
}