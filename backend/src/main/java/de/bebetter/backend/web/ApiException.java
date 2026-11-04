package de.bebetter.backend.web;
//KI-Log Nr. 11
// Fachlicher Fehler mit Meldung für den Client (z. B. "User already exists.")
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}