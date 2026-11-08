package de.bebetter.backend.rules;

import de.bebetter.backend.domain.Gender;
import de.bebetter.backend.domain.FitnessLevel;
import de.bebetter.backend.domain.Goal;
import de.bebetter.backend.domain.Profile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
// KI-Log Nr. 12,31,33
/**
 * Planregeln. Startgewichte sind technische Modellwerte,
 * keine gemessenen Kraftwerte.
 */
public final class PlanRules {

    public static final List<String> LABELS = List.of("A", "B", "C");

    public static final List<String> BODY_PARTS =
            List.of("chest", "back", "upper legs", "shoulders", "upper arms");
    // KI-Log Nr. 31: Progression im Wochenwechsel
    /** Ab so vielen „Done" pro Einheit steigt das Gewicht in der Folgewoche. */
    public static final int PROGRESSION_MIN_DONE = 4;
    private static final BigDecimal PROGRESSION_FACTOR = new BigDecimal("1.05");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("500.0");
    private PlanRules() {}

    /** health: 2 Sätze, sonst 3. */
    public static int sets(Goal goal) {
        return goal == Goal.HEALTH ? 2 : 3;
    }

    /** muscle_gain: 8-12, sonst 12-15. */
    public static String reps(Goal goal) {
        return goal == Goal.MUSCLE_GAIN ? "8-12" : "12-15";
    }

    /** Anfänger: keine Langhantel. */
    public static Set<EquipmentCategory> allowedEquipment(FitnessLevel level) {
        return level == FitnessLevel.ADVANCED
                ? Set.of(EquipmentCategory.BARBELL, EquipmentCategory.DUMBBELL,
                EquipmentCategory.MACHINE, EquipmentCategory.BODYWEIGHT)
                : Set.of(EquipmentCategory.DUMBBELL, EquipmentCategory.MACHINE,
                EquipmentCategory.BODYWEIGHT);
    }

    /** Nur Basiswert nach Fitnesslevel (null = kein Zusatzgewicht). */
    public static BigDecimal startWeight(EquipmentCategory category, FitnessLevel level) {
        boolean advanced = level == FitnessLevel.ADVANCED;
        return switch (category) {
            case BARBELL -> advanced ? new BigDecimal("30.0") : null;
            case DUMBBELL -> advanced ? new BigDecimal("8.0") : new BigDecimal("4.0");
            case MACHINE -> advanced ? new BigDecimal("25.0") : new BigDecimal("10.0");
            case BODYWEIGHT, OTHER -> null;
        };
    }

    // KI-Log Nr. 33
    /** Geschlechtsfaktor: männlich 1,20, divers 1,10, weiblich 1,00. */
    public static BigDecimal genderFactor(Gender gender) {
        if (gender == null) return BigDecimal.ONE;
        return switch (gender) {
            case MALE -> new BigDecimal("1.20");
            case DIVERSE -> new BigDecimal("1.10");
            case FEMALE -> BigDecimal.ONE;
        };
    }

    /**
     * Körperdatenabhängiger Startvorschlag (wird vom PlanGenerator verwendet):
     * Basis × Körperfaktor (BMI/25, begrenzt 0,80–1,20) × Altersfaktor (ab 60: 0,90)
     * × Geschlechtsfaktor (KI-Log Nr. 33), abgerundet auf 0,5 kg, mindestens 0,5 kg.
     */
    public static BigDecimal startWeight(EquipmentCategory category, Profile profile) {
        BigDecimal base = startWeight(category, profile.getFitnessLevel());
        if (base == null) return null;
        BigDecimal meters = BigDecimal.valueOf(profile.getHeightCm(), 2);
        BigDecimal bmi = profile.getWeightKg()
                .divide(meters.multiply(meters), 12, RoundingMode.HALF_UP);
        BigDecimal bodyFactor = bmi.divide(new BigDecimal("25"), 12, RoundingMode.HALF_UP)
                .max(new BigDecimal("0.80")).min(new BigDecimal("1.20"));
        BigDecimal ageFactor = profile.getAge() >= 60 ? new BigDecimal("0.90") : BigDecimal.ONE;
        BigDecimal step = new BigDecimal("0.5");
        return base.multiply(bodyFactor).multiply(ageFactor)
                .multiply(genderFactor(profile.getGender()))
                .divide(step, 0, RoundingMode.FLOOR).multiply(step).max(step);
    }
    // KI-Log Nr. 31
    /** true bei 4 oder 5 von 5 Übungen als „Done". */
    public static boolean progresses(long doneCount) {
        return doneCount >= PROGRESSION_MIN_DONE;
    }

    // KI-Log Nr. 31
    /**
     * Gewicht der Folgewoche: +5 %, auf 0,5 kg gerundet (kaufmännisch), höchstens 500 kg.
     * null (Körpergewicht) bleibt null.
     */
    public static BigDecimal progressedWeight(BigDecimal weight) {
        if (weight == null) return null;
        BigDecimal step = new BigDecimal("0.5");
        return weight.multiply(PROGRESSION_FACTOR)
                .divide(step, 0, RoundingMode.HALF_UP).multiply(step)
                .min(MAX_WEIGHT);
    }
}