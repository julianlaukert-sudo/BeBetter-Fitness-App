package de.bebetter.backend.dto;
//KI-Log Nr. 11
public record RegistrationResponse(String message, Long userId, String name, String email, String role) {
}