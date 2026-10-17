package de.bebetter.backend.dto;

import java.time.LocalDate;

public record ProfileResponse(Long profileId,
                              ProfileRequest.Personal personal,
                              ProfileRequest.Training training,
                              LocalDate createdAt,
                              LocalDate updatedAt,
                              boolean planExists) {}