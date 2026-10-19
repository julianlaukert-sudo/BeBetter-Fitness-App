package de.bebetter.backend;

import de.bebetter.backend.domain.FitnessLevel;
import de.bebetter.backend.domain.Gender;
import de.bebetter.backend.domain.Goal;
import de.bebetter.backend.domain.Profile;
import de.bebetter.backend.exercisedb.CatalogExercise;
import de.bebetter.backend.rules.EquipmentCategory;
import de.bebetter.backend.rules.ExerciseSelection;
import de.bebetter.backend.rules.PlanRules;
import de.bebetter.backend.service.PlanGenerator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanRulesTest {

    static Profile profile(int age, int height, String weight) {
        var p = new Profile(1L, OffsetDateTime.now());
        p.setAge(age);
        p.setHeightCm(height);
        p.setWeightKg(new BigDecimal(weight));
        p.setFitnessLevel(FitnessLevel.BEGINNER);
        p.setGender(Gender.MALE);
        p.setGoal(Goal.HEALTH);
        return p;
    }

    static Map<String, List<CatalogExercise>> candidates() {
        var map = new LinkedHashMap<String, List<CatalogExercise>>();
        for (String part : PlanRules.BODY_PARTS) {
            var list = new ArrayList<CatalogExercise>();
            String[] equipment = {"dumbbell", "cable", "bodyweight", "barbell"};
            for (int i = 0; i < 4; i++) list.add(new CatalogExercise(part.replace(" ", "") + i,
                    equipment[i] + " press", List.of(part), List.of("muscle" + i),
                    List.of(equipment[i]), null));
            map.put(part, list);
        }
        return map;
    }

    static BigDecimal weight(int age, int height, String weight) {
        return PlanRules.startWeight(EquipmentCategory.DUMBBELL, profile(age, height, weight));
    }

    @Test void bodyDataChangesTheStartSuggestion() {
        assertEquals(0, new BigDecimal("3.5").compareTo(weight(23, 180, "78.5")));
        assertEquals(0, new BigDecimal("4.0").compareTo(weight(23, 180, "90.0")));
        assertEquals(0, new BigDecimal("4.0").compareTo(weight(23, 170, "78.5")));
        assertEquals(0, new BigDecimal("3.0").compareTo(weight(65, 180, "78.5")));
    }

    @Test void factorsAreClampedAndGenderHasNoInventedMultiplier() {
        assertEquals(0, new BigDecimal("3.0").compareTo(weight(23, 230, "30.0")));
        assertEquals(0, new BigDecimal("4.5").compareTo(weight(23, 120, "250.0")));
        var p = profile(23, 180, "78.5");
        var before = PlanRules.startWeight(EquipmentCategory.MACHINE, p);
        p.setGender(Gender.FEMALE);
        assertEquals(before, PlanRules.startWeight(EquipmentCategory.MACHINE, p));
    }

    @Test void ageThresholdAndBodyweightAreExplicit() {
        assertEquals(0, new BigDecimal("3.5").compareTo(weight(59, 180, "78.5")));
        assertEquals(0, new BigDecimal("3.0").compareTo(weight(60, 180, "78.5")));
        assertNull(PlanRules.startWeight(EquipmentCategory.BODYWEIGHT, profile(23, 180, "78.5")));
        assertFalse(PlanRules.allowedEquipment(FitnessLevel.BEGINNER)
                .contains(EquipmentCategory.BARBELL));
        assertTrue(PlanRules.allowedEquipment(FitnessLevel.ADVANCED)
                .contains(EquipmentCategory.BARBELL));
    }

    @Test void selectionIsUniqueAllowedAndDeterministic() {
        var data = candidates();
        var result = ExerciseSelection.select(data, FitnessLevel.BEGINNER, 3);
        assertEquals(result, ExerciseSelection.select(data, FitnessLevel.BEGINNER, 3));
        var ids = new HashSet<String>();
        result.values().forEach(list -> {
            assertEquals(3, list.size());
            list.forEach(u -> {
                assertTrue(ids.add(u.exerciseId()));
                assertNotEquals("barbell", u.firstEquipment());
            });
        });
        assertEquals(15, ids.size());
    }

    @Test void tooFewAndMultiEquipmentBodyweightDoNotSlipThrough() {
        var data = candidates();
        data.put("chest", List.of(new CatalogExercise("bad", "test", List.of("chest"),
                List.of("pectorals"), List.of("bodyweight", "barbell"), null)));
        assertThrows(ExerciseSelection.NotEnoughExercisesException.class,
                () -> ExerciseSelection.select(data, FitnessLevel.BEGINNER, 3));
    }

    @Test void mondayCalculationCrossesYearAndWeekBoundary() {
        assertEquals(LocalDate.of(2025, 12, 29), PlanGenerator.mondayOf(LocalDate.of(2026, 1, 4)));
        assertEquals(LocalDate.of(2026, 1, 5), PlanGenerator.mondayOf(LocalDate.of(2026, 1, 5)));
        var berlin = Clock.fixed(Instant.parse("2026-01-04T23:30:00Z"),
                ZoneId.of("Europe/Berlin"));
        assertEquals(LocalDate.of(2026, 1, 5), PlanGenerator.mondayOf(LocalDate.now(berlin)));
        assertEquals(3, PlanRules.sets(Goal.MUSCLE_GAIN));
        assertEquals("8-12", PlanRules.reps(Goal.MUSCLE_GAIN));
        assertEquals(2, PlanRules.sets(Goal.HEALTH));
    }
}