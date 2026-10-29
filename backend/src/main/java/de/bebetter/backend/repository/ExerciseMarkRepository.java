package de.bebetter.backend.repository;

import de.bebetter.backend.domain.ExerciseMark;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseMarkRepository extends JpaRepository<ExerciseMark, Long> {

    // Nur manuelle Markierungen im Zeitraum (automatische zählen nicht)
    long countByDateBetweenAndAutomaticFalse(LocalDate start, LocalDate end);
}