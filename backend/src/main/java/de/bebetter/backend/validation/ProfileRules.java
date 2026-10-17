package de.bebetter.backend.validation;

import de.bebetter.backend.domain.Weekday;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reine Profilprüfung ohne Datenbankzugriff */
public final class ProfileRules {

    public static final int AGE_MIN = 16;
    public static final int AGE_MAX = 99;
    public static final int HEIGHT_MIN = 120;
    public static final int HEIGHT_MAX = 230;
    public static final BigDecimal WEIGHT_MIN = new BigDecimal("30");
    public static final BigDecimal WEIGHT_MAX = new BigDecimal("250");

    private ProfileRules() {}

    public static String checkAge(Integer age) {
        if (age == null) return "Please enter your age.";
        if (age < AGE_MIN || age > AGE_MAX) return "Allowed are 16 to 99 years.";
        return null;
    }

    public static String checkHeight(Integer cm) {
        if (cm == null) return "Please enter your height.";
        if (cm < HEIGHT_MIN || cm > HEIGHT_MAX) return "Allowed are 120 to 230 cm.";
        return null;
    }

    public static String checkWeight(BigDecimal kg) {
        if (kg == null) return "Please enter your weight.";
        if (kg.compareTo(WEIGHT_MIN) < 0 || kg.compareTo(WEIGHT_MAX) > 0)
            return "Allowed are 30 to 250 kg.";
        if (kg.stripTrailingZeros().scale() > 1)
            return "Please use at most one decimal place.";
        return null;
    }

    public static String checkTrainingDays(List<Weekday> days) {
        if (days == null || days.size() != 3 || days.stream().anyMatch(Objects::isNull)
                || new HashSet<>(days).size() != 3) {
            return "Please select exactly three training days.";
        }
        List<Integer> idx = days.stream().map(Weekday::index).sorted().toList();
        for (int i = 0; i < idx.size(); i++) {
            int a = idx.get(i);
            int b = idx.get((i + 1) % idx.size());
            // zyklisch: auch SUN -> MON zählt als benachbart
            if (Math.floorMod(b - a, 7) == 1) {
                return "Please leave a rest day between two training days.";
            }
        }
        return null;
    }

    public static Map<String, String> check(Integer age, Object gender, Integer height,
                                            BigDecimal weight, Object goal, Object fitnessLevel,
                                            List<Weekday> trainingDays) {
        Map<String, String> errors = new LinkedHashMap<>();
        RegistrationRules.putIfPresent(errors, "age", checkAge(age));
        if (gender == null) errors.put("gender", "Please select a gender.");
        RegistrationRules.putIfPresent(errors, "height", checkHeight(height));
        RegistrationRules.putIfPresent(errors, "weight", checkWeight(weight));
        if (goal == null) errors.put("goal", "Please select a goal.");
        if (fitnessLevel == null) errors.put("fitnessLevel", "Please select a fitness level.");
        RegistrationRules.putIfPresent(errors, "trainingDays", checkTrainingDays(trainingDays));
        return errors;
    }
}