package de.bebetter.backend.dto;

import java.time.LocalDate;

// Keine Passwort-Hashes, keine Tokens
public record AdminUserDto(Long userId, String name, String email,
                           LocalDate registeredAt, String role) {}