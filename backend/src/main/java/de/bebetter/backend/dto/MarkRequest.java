package de.bebetter.backend.dto;

import java.math.BigDecimal;
// KI-Log Nr. 15
public record MarkRequest(Long sessionId, Long planExerciseId,
                          Boolean done, BigDecimal weight, Integer reps) {}