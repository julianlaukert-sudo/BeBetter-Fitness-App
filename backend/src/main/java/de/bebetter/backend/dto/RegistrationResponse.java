package de.bebetter.backend.dto;

public record RegistrationResponse(String message, Long userId, String name, String email, String role) {
}