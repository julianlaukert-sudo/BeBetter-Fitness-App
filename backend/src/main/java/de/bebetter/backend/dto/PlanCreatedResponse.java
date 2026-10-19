package de.bebetter.backend.dto;

import java.time.LocalDate;

public record PlanCreatedResponse(String message, Long planId, LocalDate weekStart) {}