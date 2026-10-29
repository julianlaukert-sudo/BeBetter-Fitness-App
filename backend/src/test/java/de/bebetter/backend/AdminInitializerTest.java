package de.bebetter.backend;

import de.bebetter.backend.config.AdminInitializer;
import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminInitializerTest {

    final UserRepository users = mock(UserRepository.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);

    AdminInitializer initializer(String password) {
        return new AdminInitializer(users, passwords, clock, "Admin Test", " ADMIN@example.org ",
                password);
    }

    @Test void createsOnlyAdminWithNormalizedEmailAndHash() {
        when(passwords.encode("Secure123!")).thenReturn("encoded");
        initializer("Secure123!").run(new DefaultApplicationArguments());
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertEquals("admin@example.org", captor.getValue().getEmail());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
        assertEquals("encoded", captor.getValue().getPasswordHash());
    }

    @Test void restartNeverResetsExistingAdminPassword() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(true);
        initializer("").run(new DefaultApplicationArguments());
        verifyNoInteractions(passwords);
        verify(users, never()).saveAndFlush(any());
    }

    @Test void collisionOrInvalidPasswordDoesNotPromoteAUser() {
        assertThrows(IllegalStateException.class,
                () -> initializer("bad").run(new DefaultApplicationArguments()));
        when(users.existsByEmailIgnoreCase("admin@example.org")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> initializer("Secure123!").run(new DefaultApplicationArguments()));
        verify(users, never()).saveAndFlush(any());
    }
}