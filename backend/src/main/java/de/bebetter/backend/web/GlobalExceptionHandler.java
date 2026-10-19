package de.bebetter.backend.web;

import de.bebetter.backend.exercisedb.ExerciseDbException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Projektkonvention wie Referenz: Fehler = HTTP 400 mit "message" (und ggf. "errors")
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ExerciseDB-Ausfall, Rate Limit oder zu wenige Kandidaten
    @ExceptionHandler(ExerciseDbException.class)
    public ResponseEntity<ErrorResponse> exerciseDb(ExerciseDbException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("Exercises could not be loaded."));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(exception.getMessage()));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            InvalidInputException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid input.", exception.getFieldErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(
            HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("Invalid input."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("Data conflict. Please reload."));
    }
}