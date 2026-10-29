package de.bebetter.backend.config;

import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.validation.RegistrationRules;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt EINMALIG einen Admin an – nur wenn ADMIN_BOOTSTRAP_ENABLED=true gesetzt ist.
 * Existiert schon ein ADMIN, passiert nichts (kein Passwort-Reset).
 * Eine bereits registrierte USER-Adresse wird NICHT hochgestuft.
 */
@Component
@ConditionalOnProperty(name = "admin.bootstrap.enabled", havingValue = "true")
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final String name;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository users, PasswordEncoder passwords, Clock clock,
                            @Value("${ADMIN_NAME:BeBetter Admin}") String name,
                            @Value("${ADMIN_EMAIL:}") String email,
                            @Value("${ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.clock = clock;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) return;
        String normalized = RegistrationRules.normalizeEmail(email);
        if (!RegistrationRules.check(name, normalized, password).isEmpty())
            throw new IllegalStateException("Check ADMIN_NAME, ADMIN_EMAIL and ADMIN_PASSWORD.");
        if (users.existsByEmailIgnoreCase(normalized))
            throw new IllegalStateException("ADMIN_EMAIL is already registered; no automatic promotion.");
        users.saveAndFlush(new User(name.trim(), normalized, passwords.encode(password),
                Role.ADMIN, OffsetDateTime.now(clock)));
    }
}