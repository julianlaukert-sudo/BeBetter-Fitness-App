package de.bebetter.backend.validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Muss zu frontend/src/utils/validation.js passen.
public final class RegistrationRules {

    public static final int NAME_MIN = 2;
    public static final int NAME_MAX = 50;
    public static final int EMAIL_MAX = 255;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s.]{2,}$");

    private RegistrationRules() {
    }

    public static String checkName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) return "Please enter your name.";
        if (value.length() < NAME_MIN || value.length() > NAME_MAX) {
            return "Name must be " + NAME_MIN + " to " + NAME_MAX + " characters long.";
        }
        return null;
    }

    public static String checkEmail(String email) {
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) return "Please enter your email address.";
        if (value.length() > EMAIL_MAX || !EMAIL.matcher(value).matches()) {
            return "Please enter a valid email address.";
        }
        return null;
    }

    public static String checkPassword(String password) {
        if (password == null || password.isEmpty()) return "Please enter a password.";
        List<PasswordViolation> violations = PasswordPolicy.check(password);
        if (violations.isEmpty()) return null;
        return violations.stream().map(PasswordViolation::getMessage).collect(Collectors.joining(" "));
    }

    public static Map<String, String> check(String name, String email, String password) {
        Map<String, String> errors = new LinkedHashMap<>();
        putIfPresent(errors, "name", checkName(name));
        putIfPresent(errors, "email", checkEmail(email));
        putIfPresent(errors, "password", checkPassword(password));
        return errors;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static void putIfPresent(Map<String, String> errors, String field, String message) {
        if (message != null) errors.put(field, message);
    }
}