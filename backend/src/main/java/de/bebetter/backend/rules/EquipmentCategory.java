package de.bebetter.backend.rules;

import java.util.Locale;
// KI-Log Nr. 12
/** Geräteklasse einer ExerciseDB-Übung. */
public enum EquipmentCategory {
    BARBELL, DUMBBELL, MACHINE, BODYWEIGHT, OTHER;

    public static EquipmentCategory of(String equipment) {
        if (equipment == null) return OTHER;
        return switch (equipment.toLowerCase(Locale.ROOT)) {
            case "barbell", "ez barbell", "olympic barbell" -> BARBELL;
            case "dumbbell" -> DUMBBELL;
            case "leverage machine", "sled machine", "cable", "smith machine" -> MACHINE;
            case "body weight", "bodyweight" -> BODYWEIGHT;
            default -> OTHER;
        };
    }
}