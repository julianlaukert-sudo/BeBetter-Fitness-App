package de.bebetter.backend.repository;

import de.bebetter.backend.domain.PlanExercise;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// KI-Log Nr. 15
public interface PlanExerciseRepository extends JpaRepository<PlanExercise, Long> {

    // Nur die EIGENE Planübung, die auch zur angegebenen Einheit gehört
    @Query("select e from PlanExercise e join e.session s join s.plan p "
            + "where e.id = :id and s.id = :session and p.userId = :user")
    Optional<PlanExercise> findOwn(@Param("id") Long id,
                                   @Param("session") Long session,
                                   @Param("user") Long user);
}