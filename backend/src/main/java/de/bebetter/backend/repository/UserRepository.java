package de.bebetter.backend.repository;

import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRole(Role role);
    long countByRole(Role role);
    List<User> findAllByOrderByRegisteredAtDesc();
}