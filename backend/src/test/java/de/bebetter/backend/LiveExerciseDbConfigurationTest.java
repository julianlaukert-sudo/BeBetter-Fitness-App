package de.bebetter.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Läuft im normalen Testlauf – sendet keine Anfrage, prüft nur die URL-Form. */
class LiveExerciseDbConfigurationTest {

    @Test void blankKeepsDocumentedDefault() {
        assertEquals("https://oss.exercisedb.dev/api/v1", LiveExerciseDbTest.source(null));
        assertEquals(LiveExerciseDbTest.source(null), LiveExerciseDbTest.source(" "));
    }

    @Test void publicHttpsBaseIsNormalized() {
        assertEquals("https://catalog.example.org/api/v1",
                LiveExerciseDbTest.source(" https://catalog.example.org/api/v1/ "));
    }

    @Test void credentialsQueriesFragmentsListsAndHttpAreRejected() {
        for (String value : new String[]{"http://catalog.test/api", "https://u:p@catalog.test",
                "https://catalog.test?token=secret", "https://catalog.test#section",
                "https://one.test,https://two.test", "/api/v1"})
            assertThrows(IllegalArgumentException.class, () -> LiveExerciseDbTest.source(value));
    }
}