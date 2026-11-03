package de.bebetter.backend.validation;
//KI-Log Nr. 10
/**
 * Mögliche Verstöße gegen die Passwortregeln.
 */
public enum PasswordViolation {

    TOO_SHORT("Password must be at least 8 characters long."),
    TOO_LONG("Password must be at most 64 characters long."),
    MISSING_UPPERCASE("Password must contain at least one uppercase letter."),
    MISSING_DIGIT("Password must contain at least one number."),
    MISSING_SPECIAL_CHARACTER("Password must contain at least one special character."),
    TOO_MANY_BYTES(
            "Password is too long in bytes (max. 72; umlauts and emojis count as several bytes).");

    private final String message;

    PasswordViolation(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
