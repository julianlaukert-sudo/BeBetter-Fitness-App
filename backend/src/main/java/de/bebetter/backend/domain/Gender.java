package de.bebetter.backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
//KI-Log Nr. 11
public enum Gender {
    MALE("male"), FEMALE("female"), DIVERSE("diverse");

    private final String code;

    Gender(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static Gender fromCode(String code) {
        for (Gender value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("Unknown gender: " + code);
    }
}