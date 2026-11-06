package de.bebetter.backend;

import de.bebetter.backend.domain.Profile;
import de.bebetter.backend.domain.Weekday;
import de.bebetter.backend.service.PlanGenerator;
import de.bebetter.backend.service.TimeProvider;
import java.time.LocalDate;
import java.util.List;
import de.bebetter.backend.exercisedb.CatalogRefreshJob;
import de.bebetter.backend.exercisedb.ExerciseDbClient;
import de.bebetter.backend.rules.PlanRules;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
// KI-Log Nr. 29
class CatalogRefreshTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ExerciseDbClientTest.TestClock clock = new ExerciseDbClientTest.TestClock();
    private final ExerciseDbClient api = new ExerciseDbClient("https://api.test", builder, clock);
    private final CatalogRefreshJob job = new CatalogRefreshJob(api);

    @AfterEach
    void close() {
        api.close();
    }

    private void expectCatalog(String version) {
        for (String part : PlanRules.BODY_PARTS) {
            String url = "https://api.test/exercises?bodyParts="
                    + part.replace(" ", "%20") + "&limit=25";
            String response = """
                    {"data":[{"exerciseId":"%s-%s","name":"press","bodyParts":["%s"]}]}
                    """.formatted(version, part, part);
            server.expect(requestTo(url))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        }
    }

    @Test
    void refreshLoadsMissingRegionsAndUpdatesThemWhenDue() {
        expectCatalog("first");
        job.refresh();
        server.verify();

        // Ohne neue erwartete Antworten würde jeder zusätzliche HTTP-Aufruf fehlschlagen.
        server.reset();
        clock.now = clock.now.plus(Duration.ofMinutes(10));
        job.refresh();
        assertEquals("first-chest", api.byBodyPart("chest").get(0).exerciseId());

        // Insgesamt 5 h 15 min vergangen: Der Cache ist noch genau 45 min gültig.
        clock.now = clock.now.plus(Duration.ofHours(5)).plus(Duration.ofMinutes(5));
        expectCatalog("second");
        job.refresh();
        server.verify();

        for (String part : PlanRules.BODY_PARTS) {
            assertEquals("second-" + part, api.byBodyPart(part).get(0).exerciseId());
        }
    }
    @Test
    void buildsThreeDayPlanFromWarmCacheWithoutFurtherHttpRequests() {
        // Für drei Trainingstage benötigt der Plan drei verschiedene Übungen je Region.
        for (String part : PlanRules.BODY_PARTS) {
            String url = "https://api.test/exercises?bodyParts="
                    + part.replace(" ", "%20") + "&limit=25";
            String response = """
                    {"data":[
                      {"exerciseId":"%1$s-1","name":"press one","bodyParts":["%1$s"],"equipments":["body weight"]},
                      {"exerciseId":"%1$s-2","name":"press two","bodyParts":["%1$s"],"equipments":["body weight"]},
                      {"exerciseId":"%1$s-3","name":"press three","bodyParts":["%1$s"],"equipments":["body weight"]}
                    ]}
                    """.formatted(part);
            server.expect(requestTo(url))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        }

        job.refresh();
        server.verify();
        server.reset(); // Ab hier lässt jeder weitere HTTP-Aufruf den Test fehlschlagen.

        var time = new TimeProvider(clock);
        var profile = new Profile(1L, time.now());
        profile.setTrainingDayList(List.of(Weekday.MON, Weekday.WED, Weekday.FRI));

        // build() erstellt den Plan im Speicher und benötigt keine Datenbank.
        var generator = new PlanGenerator(api, null, null, time);
        var plan = generator.build(profile, LocalDate.of(2026, 11, 2));

        assertEquals(3, plan.getSessions().size());
        plan.getSessions().forEach(session ->
                assertEquals(5, session.getExercises().size()));
    }
}