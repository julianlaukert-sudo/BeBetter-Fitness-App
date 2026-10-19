package de.bebetter.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlanResponse(Long planId, LocalDate weekStart, LocalDate weekEnd,
                           LocalDate today, boolean exerciseDataAvailable,
                           List<Session> sessions) {

    public record Session(Long sessionId, String label, LocalDate date,
                          List<Exercise> exercises) {}

    // name, targetMuscle, equipment, imageUrl kommen aus ExerciseDB und dürfen fehlen
    public record Exercise(Long planExerciseId, String exerciseId, String name, String bodyPart,
                           String targetMuscle, String equipment, String imageUrl,
                           int sets, String reps, BigDecimal weight, boolean bodyweight) {}
}