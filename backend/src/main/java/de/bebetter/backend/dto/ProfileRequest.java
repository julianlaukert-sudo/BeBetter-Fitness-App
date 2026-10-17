package de.bebetter.backend.dto;

import de.bebetter.backend.domain.FitnessLevel;
import de.bebetter.backend.domain.Gender;
import de.bebetter.backend.domain.Goal;
import de.bebetter.backend.domain.Weekday;
import java.math.BigDecimal;
import java.util.List;

public record ProfileRequest(Personal personal, Training training) {

    public record Personal(Integer age, Gender gender, Integer height, BigDecimal weight) {}

    public record Training(Goal goal, FitnessLevel fitnessLevel, List<Weekday> trainingDays) {}
}