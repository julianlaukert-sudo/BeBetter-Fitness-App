package de.bebetter.backend;

import de.bebetter.backend.config.ExerciseDbConfig;
import de.bebetter.backend.domain.FitnessLevel;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.rules.ExerciseSelection;
import de.bebetter.backend.rules.PlanRules;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/** Echter ExerciseDB-Dienst – nur mit -Dbebetter.live=true, nicht in der normalen CI. */
@EnabledIfSystemProperty(named = "bebetter.live", matches = "true")
class LiveExerciseDbTest {

    @Test void catalogCanSupplyFifteenRealExerciseIds() {
        var api = new ExerciseDbClient("https://oss.exercisedb.dev/api/v1",
                new ExerciseDbConfig().exerciseBuilder(), Clock.systemUTC());
        try {
            var selected = ExerciseSelection.select(api.candidates(PlanRules.BODY_PARTS),
                    FitnessLevel.BEGINNER, 3);
            assertEquals(15, selected.values().stream().mapToInt(List::size).sum());
            var first = selected.get("chest").get(0);
            assertNotNull(api.exercise(first.exerciseId()).name());
        } finally { api.close(); }
    }
}