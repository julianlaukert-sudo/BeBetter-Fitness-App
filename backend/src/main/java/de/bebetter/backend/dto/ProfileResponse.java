package de.bebetter.backend.dto;

import java.time.LocalDate;

// nextPlanFrom: Montag der vorgemerkten Neuplanung oder null
public record ProfileResponse(Long profileId,
                              ProfileRequest.Personal personal,
                              ProfileRequest.Training training,
                              LocalDate createdAt,
                              LocalDate updatedAt,
                              boolean planExists,
                              LocalDate nextPlanFrom) {}