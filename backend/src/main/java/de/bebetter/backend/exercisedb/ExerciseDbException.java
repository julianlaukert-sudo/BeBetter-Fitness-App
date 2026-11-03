package de.bebetter.backend.exercisedb;
// KI-Log Nr. 12
public class ExerciseDbException extends RuntimeException {

    public ExerciseDbException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExerciseDbException(String message) {
        super(message);
    }
}