package de.bebetter.backend.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
//KI-Log Nr. 11
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String message, Map<String, String> errors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Map.of());
    }
}