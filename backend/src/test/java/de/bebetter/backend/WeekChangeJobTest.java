package de.bebetter.backend;

import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.service.WeekChangeJob;
import de.bebetter.backend.service.WeekChangeService;
import java.util.List;
import org.junit.jupiter.api.Test;
// KI-Log Nr. 15
import static org.mockito.Mockito.*;

class WeekChangeJobTest {

    @Test void oneFailureDoesNotStopOtherAccounts() {
        var plans = mock(TrainingPlanRepository.class);
        var service = mock(WeekChangeService.class);
        when(plans.findAllUserIdsWithPlan()).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("test")).when(service).update(1L);
        new WeekChangeJob(plans, service).run();
        verify(service).update(1L);
        verify(service).update(2L);
    }
}