package de.bebetter.backend.exercisedb;

public class ExerciseDbException extends RuntimeException {

    public ExerciseDbException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExerciseDbException(String message) {
        super(message);
    }
}