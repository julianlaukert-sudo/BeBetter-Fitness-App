package de.bebetter.backend.repository;

import de.bebetter.backend.domain.Profile;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
//KI-Log Nr. 11, 12
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    // Zeilensperre: zwei gleichzeitige POST /plans erzeugen nur einen Plan (ab Commit 3 genutzt)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.userId = :id")
    Optional<Profile> findForPlan(@Param("id") Long id);
}