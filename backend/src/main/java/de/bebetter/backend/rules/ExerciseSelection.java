package de.bebetter.backend.rules;

import de.bebetter.backend.domain.FitnessLevel;
import de.bebetter.backend.exercisedb.CatalogExercise;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Deterministische, abwechslungsreiche Übungsauswahl. */
public final class ExerciseSelection {

    private static final List<String> EXCLUDED = List.of("stretch", "v. 2", "v. 3", "v. 4",
            "(male)", "(female)", "bosu", "stability ball", "exercise ball", "with arm blaster");

    private ExerciseSelection() {}

    public static class NotEnoughExercisesException extends RuntimeException {
        public NotEnoughExercisesException(String bodyPart) {
            super("Not enough suitable exercises for " + bodyPart);
        }
    }

    public static Map<String, List<CatalogExercise>> select(
            Map<String, List<CatalogExercise>> candidates, FitnessLevel level, int count) {
        Set<EquipmentCategory> allowed = PlanRules.allowedEquipment(level);
        Set<String> alreadySelected = new HashSet<>();
        Map<String, List<CatalogExercise>> result = new LinkedHashMap<>();
        for (String part : PlanRules.BODY_PARTS) {
            List<CatalogExercise> all = candidates.getOrDefault(part, List.of());
            List<CatalogExercise> suitable = all.stream()
                    .filter(u -> u.exerciseId() != null && u.equipments() != null
                            && u.equipments().size() == 1)
                    .filter(u -> u.bodyParts() != null && u.bodyParts().contains(part))
                    .filter(u -> !u.exerciseId().isBlank())
                    .filter(u -> allowed.contains(EquipmentCategory.of(u.firstEquipment())))
                    .filter(u -> !excluded(u.name()))
                    .sorted(ranking(level))
                    .toList();
            List<CatalogExercise> selected = selectVaried(suitable, count, alreadySelected);
            if (selected.size() < count) {
                // einfacher Körpergewichts-Ersatz
                List<CatalogExercise> bodyweight = all.stream()
                        .filter(u -> u.exerciseId() != null && !u.exerciseId().isBlank()
                                && u.name() != null && u.equipments() != null
                                && u.equipments().size() == 1
                                && u.bodyParts() != null && u.bodyParts().contains(part))
                        .filter(u -> EquipmentCategory.of(u.firstEquipment())
                                == EquipmentCategory.BODYWEIGHT)
                        .sorted(ranking(level))
                        .toList();
                for (CatalogExercise u : bodyweight) {
                    if (selected.size() >= count) break;
                    if (alreadySelected.add(u.exerciseId())) selected.add(u);
                }
            }
            if (selected.size() < count) {
                throw new NotEnoughExercisesException(part);
            }
            result.put(part, selected);
        }
        return result;
    }

    private static List<CatalogExercise> selectVaried(List<CatalogExercise> suitable, int count,
                                                      Set<String> alreadySelected) {
        List<CatalogExercise> selected = new ArrayList<>();
        Set<String> muscles = new HashSet<>();
        Set<EquipmentCategory> equipment = new HashSet<>();
        while (selected.size() < count) {
            CatalogExercise best = null;
            int bestScore = -1;
            for (CatalogExercise u : suitable) {
                if (alreadySelected.contains(u.exerciseId())) continue;
                int score = (muscles.contains(u.firstTargetMuscle()) ? 0 : 2)
                        + (equipment.contains(EquipmentCategory.of(u.firstEquipment())) ? 0 : 1);
                if (score > bestScore) { // bei Gleichstand gewinnt die frühere (Rangfolge)
                    best = u;
                    bestScore = score;
                }
            }
            if (best == null) break;
            selected.add(best);
            alreadySelected.add(best.exerciseId());
            muscles.add(best.firstTargetMuscle());
            equipment.add(EquipmentCategory.of(best.firstEquipment()));
        }
        return selected;
    }

    private static Comparator<CatalogExercise> ranking(FitnessLevel level) {
        return Comparator
                .comparingInt((CatalogExercise u) ->
                        equipmentRank(EquipmentCategory.of(u.firstEquipment()), level))
                .thenComparingInt(u -> u.name() == null ? 999 : u.name().length())
                .thenComparing(u -> u.name() == null ? "" : u.name())
                .thenComparing(CatalogExercise::exerciseId);
    }

    private static int equipmentRank(EquipmentCategory category, FitnessLevel level) {
        return switch (category) {
            case BARBELL -> 0;
            case DUMBBELL -> 1;
            case MACHINE -> 2;
            case BODYWEIGHT -> 3;
            case OTHER -> 9;
        };
    }

    private static boolean excluded(String name) {
        if (name == null) return true;
        String n = name.toLowerCase(Locale.ROOT);
        return EXCLUDED.stream().anyMatch(n::contains);
    }
}