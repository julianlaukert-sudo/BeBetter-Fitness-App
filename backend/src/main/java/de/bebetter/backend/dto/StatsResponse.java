package de.bebetter.backend.dto;

// users = Konten mit Rolle USER; plans = alle Planwochen; marksLastSevenDays = manuelle Markierungen heute bis vor 6 Tagen
public record StatsResponse(long users, long plans, long marksLastSevenDays) {}