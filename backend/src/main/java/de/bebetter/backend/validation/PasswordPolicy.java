package de.bebetter.backend.validation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Passwortregeln für die Registrierung.
 * Die Grenze von 72 Bytes kommt von BCrypt.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;
    public static final int MAX_BYTES = 72;

    private static final Pattern UPPERCASE =
            Pattern.compile("\\p{Lu}");

    private static final Pattern DIGIT =
            Pattern.compile("[0-9]");

    private static final Pattern SPECIAL_CHARACTER =
            Pattern.compile("[^\\p{L}\\p{N}\\s]");

    private PasswordPolicy() {
        // Nur statische Methoden
    }

    public static List<PasswordViolation> check(String password) {
        String value = password == null ? "" : password;
        List<PasswordViolation> violations = new ArrayList<>();

        if (value.length() < MIN_LENGTH) {
            violations.add(PasswordViolation.TOO_SHORT);
        }

        if (value.length() > MAX_LENGTH) {
            violations.add(PasswordViolation.TOO_LONG);
        }

        if (!UPPERCASE.matcher(value).find()) {
            violations.add(PasswordViolation.MISSING_UPPERCASE);
        }

        if (!DIGIT.matcher(value).find()) {
            violations.add(PasswordViolation.MISSING_DIGIT);
        }

        if (!SPECIAL_CHARACTER.matcher(value).find()) {
            violations.add(PasswordViolation.MISSING_SPECIAL_CHARACTER);
        }

        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            violations.add(PasswordViolation.TOO_MANY_BYTES);
        }

        return List.copyOf(violations);
    }

    public static boolean isValid(String password) {
        return check(password).isEmpty();
    }

    // Für den Login: BCrypt verarbeitet höchstens 72 Byte
    public static boolean exceedsByteLimit(String password) {
        return password != null
                && password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}
