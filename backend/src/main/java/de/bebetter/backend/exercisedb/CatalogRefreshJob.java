package de.bebetter.backend.exercisedb;

import de.bebetter.backend.rules.PlanRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "exercisedb.refresh.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CatalogRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(CatalogRefreshJob.class);
    private final ExerciseDbClient client;

    public CatalogRefreshJob(ExerciseDbClient client) {
        this.client = client;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "PT10M")
    public void refresh() {
        for (String part : PlanRules.BODY_PARTS) {
            try {
                client.refreshBodyPartIfDue(part);
            } catch (ExerciseDbException ex) {
                log.warn("Katalog für {} konnte nicht aktualisiert werden: {}",
                        part, ex.getMessage());
            }
        }
    }
}