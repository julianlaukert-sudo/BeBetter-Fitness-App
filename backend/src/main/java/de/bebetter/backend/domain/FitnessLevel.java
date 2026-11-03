package de.bebetter.backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
//KI-Log Nr. 11
public enum FitnessLevel {
    BEGINNER("beginner"), ADVANCED("advanced");

    private final String code;

    FitnessLevel(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static FitnessLevel fromCode(String code) {
        for (FitnessLevel value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("Unknown fitness level: " + code);
    }
}