package de.bebetter.backend.dto;

import java.time.LocalDate;
// KI-Log Nr. 12
public record PlanCreatedResponse(String message, Long planId, LocalDate weekStart) {}