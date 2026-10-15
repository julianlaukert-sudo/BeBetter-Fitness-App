package de.bebetter.backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Goal {
    MUSCLE_GAIN("muscle_gain"), FAT_LOSS("fat_loss"), HEALTH("health");

    private final String code;

    Goal(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static Goal fromCode(String code) {
        for (Goal value : values()) {
            if (value.code.equalsIgnoreCase(code)) return value;
        }
        throw new IllegalArgumentException("Unknown goal: " + code);
    }
}