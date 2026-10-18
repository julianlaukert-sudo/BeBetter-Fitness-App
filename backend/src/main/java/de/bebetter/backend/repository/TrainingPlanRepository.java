package de.bebetter.backend.repository;

import de.bebetter.backend.domain.TrainingPlan;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    Optional<TrainingPlan> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

    boolean existsByUserId(Long userId);
}