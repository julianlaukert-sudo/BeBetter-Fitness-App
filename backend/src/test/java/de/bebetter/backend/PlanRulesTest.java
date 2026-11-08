
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

// KI-Log Nr. 12, 31, 33
class PlanRulesTest {

    static Profile profile(int age, int height, String weight) {
        var p = new Profile(1L, OffsetDateTime.now());
        p.setAge(age);
        p.setHeightCm(height);
        p.setWeightKg(new BigDecimal(weight));
        p.setFitnessLevel(FitnessLevel.BEGINNER);
        p.setGender(Gender.FEMALE);
        p.setGoal(Goal.HEALTH);
        return p;
    }

    static Map<String, List<CatalogExercise>> candidates() {
        var map = new LinkedHashMap<String, List<CatalogExercise>>();

        for (String part : PlanRules.BODY_PARTS) {
            var list = new ArrayList<CatalogExercise>();
            String[] equipment = {
                    "dumbbell", "cable", "bodyweight", "barbell"
            };

            for (int i = 0; i < 4; i++) {
                list.add(new CatalogExercise(
                        part.replace(" ", "") + i,
                        equipment[i] + " press",
                        List.of(part),
                        List.of("muscle" + i),
                        List.of(equipment[i]),
                        null
                ));
            }

            map.put(part, list);
        }

        return map;
    }

    static BigDecimal weight(int age, int height, String weight) {
        return PlanRules.startWeight(
                EquipmentCategory.DUMBBELL,
                profile(age, height, weight)
        );
    }

    @Test
    void bodyDataChangesTheStartSuggestion() {
        assertEquals(
                0,
                new BigDecimal("4.0").compareTo(
                        weight(23, 170, "78.5")
                )
        );

        assertEquals(
                0,
                new BigDecimal("4.0").compareTo(
                        weight(23, 180, "90.0")
                )
        );

        assertEquals(
                0,
                new BigDecimal("3.5").compareTo(
                        weight(23, 180, "78.5")
                )
        );

        assertEquals(
                0,
                new BigDecimal("3.0").compareTo(
                        weight(65, 180, "78.5")
                )
        );
    }

    @Test
    void bodyFactorIsClamped() {
        assertEquals(
                0,
                new BigDecimal("3.0").compareTo(
                        weight(23, 230, "30.0")
                )
        );

        assertEquals(
                0,
                new BigDecimal("4.5").compareTo(
                        weight(23, 120, "250.0")
                )
        );
    }

    // KI-Log Nr. 33
    @Test
    void genderFactorMaleTwentyAndDiverseTenPercent() {
        var p = profile(23, 180, "78.5");

        assertEquals(
                0,
                new BigDecimal("3.5").compareTo(
                        PlanRules.startWeight(EquipmentCategory.DUMBBELL, p)
                )
        );

        assertEquals(
                0,
                new BigDecimal("9.5").compareTo(
                        PlanRules.startWeight(EquipmentCategory.MACHINE, p)
                )
        );

        p.setGender(Gender.DIVERSE);

        assertEquals(
                0,
                new BigDecimal("4.0").compareTo(
                        PlanRules.startWeight(EquipmentCategory.DUMBBELL, p)
                )
        );

        assertEquals(
                0,
                new BigDecimal("10.5").compareTo(
                        PlanRules.startWeight(EquipmentCategory.MACHINE, p)
                )
        );

        p.setGender(Gender.MALE);

        assertEquals(
                0,
                new BigDecimal("4.5").compareTo(
                        PlanRules.startWeight(EquipmentCategory.DUMBBELL, p)
                )
        );

        assertEquals(
                0,
                new BigDecimal("11.5").compareTo(
                        PlanRules.startWeight(EquipmentCategory.MACHINE, p)
                )
        );

        assertNull(
                PlanRules.startWeight(EquipmentCategory.BODYWEIGHT, p)
        );
    }

    @Test
    void genderFactorChangesStartWeight() {
        var male = profile(23, 180, "78.5");
        male.setGender(Gender.MALE);

        var diverse = profile(23, 180, "78.5");
        diverse.setGender(Gender.DIVERSE);

        var female = profile(23, 180, "78.5");
        female.setGender(Gender.FEMALE);

        assertEquals(
                0,
                new BigDecimal("4.5").compareTo(
                        PlanRules.startWeight(
                                EquipmentCategory.DUMBBELL, male
                        )
                )
        );

        assertEquals(
                0,
                new BigDecimal("4.0").compareTo(
                        PlanRules.startWeight(
                                EquipmentCategory.DUMBBELL, diverse
                        )
                )
        );

        assertEquals(
                0,
                new BigDecimal("3.5").compareTo(
                        PlanRules.startWeight(
                                EquipmentCategory.DUMBBELL, female
                        )
                )
        );
    }

    @Test
    void ageThresholdAndBodyweightAreExplicit() {
        assertEquals(
                0,
                new BigDecimal("3.5").compareTo(
                        weight(59, 180, "78.5")
                )
        );

        assertEquals(
                0,
                new BigDecimal("3.0").compareTo(
                        weight(60, 180, "78.5")
                )
        );

        assertNull(
                PlanRules.startWeight(
                        EquipmentCategory.BODYWEIGHT,
                        profile(23, 180, "78.5")
                )
        );

        assertFalse(
                PlanRules.allowedEquipment(FitnessLevel.BEGINNER)
                        .contains(EquipmentCategory.BARBELL)
        );

        assertTrue(
                PlanRules.allowedEquipment(FitnessLevel.ADVANCED)
                        .contains(EquipmentCategory.BARBELL)
        );
    }

    @Test
    void selectionIsUniqueAllowedAndDeterministic() {
        var data = candidates();

        var result = ExerciseSelection.select(
                data, FitnessLevel.BEGINNER, 3
        );

        assertEquals(
                result,
                ExerciseSelection.select(
                        data, FitnessLevel.BEGINNER, 3
                )
        );

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

    @Test
    void tooFewAndMultiEquipmentBodyweightDoNotSlipThrough() {
        var data = candidates();

        data.put(
                "chest",
                List.of(
                        new CatalogExercise(
                                "bad",
                                "test",
                                List.of("chest"),
                                List.of("pectorals"),
                                List.of("bodyweight", "barbell"),
                                null
                        )
                )
        );

        assertThrows(
                ExerciseSelection.NotEnoughExercisesException.class,
                () -> ExerciseSelection.select(
                        data, FitnessLevel.BEGINNER, 3
                )
        );
    }

    @Test
    void mondayCalculationCrossesYearAndWeekBoundary() {
        assertEquals(
                LocalDate.of(2025, 12, 29),
                PlanGenerator.mondayOf(LocalDate.of(2026, 1, 4))
        );

        assertEquals(
                LocalDate.of(2026, 1, 5),
                PlanGenerator.mondayOf(LocalDate.of(2026, 1, 5))
        );

        var berlin = Clock.fixed(
                Instant.parse("2026-01-04T23:30:00Z"),
                ZoneId.of("Europe/Berlin")
        );

        assertEquals(
                LocalDate.of(2026, 1, 5),
                PlanGenerator.mondayOf(LocalDate.now(berlin))
        );

        assertEquals(3, PlanRules.sets(Goal.MUSCLE_GAIN));
        assertEquals("8-12", PlanRules.reps(Goal.MUSCLE_GAIN));
        assertEquals(2, PlanRules.sets(Goal.HEALTH));
    }

    // KI-Log Nr. 31
    @Test
    void progressionNeedsAtLeastFourDone() {
        assertFalse(PlanRules.progresses(0));
        assertFalse(PlanRules.progresses(3));
        assertTrue(PlanRules.progresses(4));
        assertTrue(PlanRules.progresses(5));
    }

    // KI-Log Nr. 31
    @Test
    void progressionAddsFivePercentRoundedToHalfKilogram() {
        assertEquals(
                0,
                new BigDecimal("10.5").compareTo(
                        PlanRules.progressedWeight(new BigDecimal("10.0"))
                )
        );

        assertEquals(
                0,
                new BigDecimal("8.5").compareTo(
                        PlanRules.progressedWeight(new BigDecimal("8.0"))
                )
        );

        assertEquals(
                0,
                new BigDecimal("26.5").compareTo(
                        PlanRules.progressedWeight(new BigDecimal("25.0"))
                )
        );

        assertEquals(
                0,
                new BigDecimal("5.5").compareTo(
                        PlanRules.progressedWeight(new BigDecimal("5.0"))
                )
        );

        // 3,5 × 1,05 = 3,675 -> abgerundet auf 0,5 kg = 3,5
        assertEquals(
                0,
                new BigDecimal("3.5").compareTo(
                        PlanRules.progressedWeight(new BigDecimal("3.5"))
                )
        );

        assertEquals(
                0,
                new BigDecimal("500").compareTo(
                        PlanRules.progressedWeight(new BigDecimal("480.0"))
                )
        );

        assertNull(PlanRules.progressedWeight(null));
    }
}