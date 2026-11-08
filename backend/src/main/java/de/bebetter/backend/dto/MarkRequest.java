package de.bebetter.backend.dto;

// KI-Log Nr. 15, 32
/** Markierung einer Planübung: nur noch geschafft / nicht geschafft (weight/reps werden ignoriert). */
public record MarkRequest(Long sessionId, Long planExerciseId, Boolean done) {}