package de.bebetter.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
// KI-Log Nr. 12, 15
public record PlanResponse(Long planId, LocalDate weekStart, LocalDate weekEnd,
                           LocalDate today, boolean exerciseDataAvailable,
                           List<Session> sessions) {

    // status: OPEN | IN_PROGRESS | DONE | FAILED | PARTIAL
    // available: serverseitig bearbeitbar beim Laden; carried: aus unmarkierter Vorwoche
    public record Session(Long sessionId, String label, LocalDate date,
                          String status, boolean available, boolean carried,
                          List<Exercise> exercises) {}

    // weight/reps = Trainingsvorgaben; mark.weight/mark.reps = erfasste Istwerte
    public record Exercise(Long planExerciseId, String exerciseId, String name, String bodyPart,
                           String targetMuscle, String equipment, String imageUrl,
                           int sets, String reps, BigDecimal weight, boolean bodyweight,
                           MarkResponse mark) {}
}