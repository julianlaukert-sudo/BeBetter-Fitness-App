package de.bebetter.backend.dto;

import de.bebetter.backend.domain.ExerciseMark;
import java.math.BigDecimal;
import java.time.LocalDate;
// KI-Log Nr. 15, 32 – weight ist seit Nr. 32 immer null, reps immer 0 (Feld bleibt für API-Kompatibilität)
public record MarkResponse(Long id, LocalDate date, boolean done,
                           BigDecimal weight, int reps, boolean automatic) {

    public static MarkResponse of(ExerciseMark m) {
        return m == null ? null : new MarkResponse(m.getId(), m.getDate(), m.isDone(),
                m.getWeightKg(), m.getReps(), m.isAutomatic());
    }
}