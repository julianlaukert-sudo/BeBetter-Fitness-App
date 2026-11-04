package de.bebetter.backend.domain;

import java.time.DayOfWeek;
//KI-Log Nr. 11
public enum Weekday {
    MON, TUE, WED, THU, FRI, SAT, SUN;

    public int index() {
        return ordinal();
    }

    public DayOfWeek toDayOfWeek() {
        return DayOfWeek.of(ordinal() + 1);
    }
}