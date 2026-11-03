package de.bebetter.backend.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
//KI-Log Nr. 11
// Eingabefehler pro Feld, z. B. { "password": "..." }
public class InvalidInputException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidInputException(Map<String, String> fieldErrors) {
        super("Invalid input.");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}