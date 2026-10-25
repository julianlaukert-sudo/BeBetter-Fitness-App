package de.bebetter.backend.dto;

import java.math.BigDecimal;

public record MarkRequest(Long sessionId, Long planExerciseId,
                          Boolean done, BigDecimal weight, Integer reps) {}