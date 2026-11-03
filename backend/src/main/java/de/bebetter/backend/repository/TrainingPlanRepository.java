package de.bebetter.backend.repository;

import de.bebetter.backend.domain.TrainingPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
// KI-Log Nr. 12, 15
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    Optional<TrainingPlan> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    boolean existsByUserId(Long userId);

    // Neueste Woche eines Nutzers (Ausgangspunkt für den Wechsel)
    Optional<TrainingPlan> findFirstByUserIdOrderByWeekStartDesc(Long userId);

    // Alle Nutzer mit Plan (für den Montagsjob)
    @Query("select distinct p.userId from TrainingPlan p")
    List<Long> findAllUserIdsWithPlan();
}