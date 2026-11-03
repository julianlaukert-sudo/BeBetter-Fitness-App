package de.bebetter.backend.dto;
//KI-Log Nr. 11
public record LoginResponse(String token, String tokenType, long expiresIn, Long userId, String name,
                            String role, boolean profileCompleted) {
}