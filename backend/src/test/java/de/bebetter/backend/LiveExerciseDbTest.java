package de.bebetter.backend;

import de.bebetter.backend.config.ExerciseDbConfig;
import de.bebetter.backend.domain.FitnessLevel;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.rules.ExerciseSelection;
import de.bebetter.backend.rules.PlanRules;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;
// KI-Log Nr. 12
/** Echter ExerciseDB-Dienst – nur mit -Dbebetter.live=true; Quelle optional über BEBETTER_LIVE_URL. */
@EnabledIfSystemProperty(named = "bebetter.live", matches = "true")
class LiveExerciseDbTest {

    static final String DEFAULT_URL = "https://oss.exercisedb.dev/api/v1";

    /** Prüft nur die Form: eine öffentliche HTTPS-Basis-URL ohne Zugangsdaten, Query, Fragment oder Liste. */
    static String source(String configured) {
        String value = configured == null || configured.isBlank() ? DEFAULT_URL : configured.trim();
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null
                || uri.getFragment() != null || value.contains(","))
            throw new IllegalArgumentException("Please provide one public HTTPS base URL.");
        return value.replaceAll("/+$", "");
    }

    @Test void catalogCanSupplyFifteenRealExerciseIds() {
        String url = source(System.getenv("BEBETTER_LIVE_URL"));
        var api = new ExerciseDbClient(url, new ExerciseDbConfig().exerciseBuilder(), Clock.systemUTC());
        try {
            var selected = ExerciseSelection.select(api.candidates(PlanRules.BODY_PARTS),
                    FitnessLevel.BEGINNER, 3);
            assertEquals(15, selected.values().stream().mapToInt(List::size).sum());
            var first = selected.get("chest").get(0);
            assertNotNull(api.exercise(first.exerciseId()).name());
        } finally { api.close(); }
    }
}