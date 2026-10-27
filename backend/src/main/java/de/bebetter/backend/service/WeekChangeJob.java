package de.bebetter.backend.service;

import de.bebetter.backend.repository.TrainingPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeekChangeJob {

    private static final Logger LOG = LoggerFactory.getLogger(WeekChangeJob.class);

    private final TrainingPlanRepository plans;
    private final WeekChangeService weekChange;

    public WeekChangeJob(TrainingPlanRepository plans, WeekChangeService weekChange) {
        this.plans = plans;
        this.weekChange = weekChange;
    }

    @Scheduled(cron = "${training.week-change-cron:0 5 0 * * MON}", zone = "Europe/Berlin")
    public void run() {
        for (Long userId : plans.findAllUserIdsWithPlan()) {
            try {
                weekChange.update(userId);
            } catch (RuntimeException ex) {
                // Ein Fehler bei einem Nutzer stoppt nicht die anderen
                LOG.warn("Week change failed for user {}", userId, ex);
            }
        }
    }
}