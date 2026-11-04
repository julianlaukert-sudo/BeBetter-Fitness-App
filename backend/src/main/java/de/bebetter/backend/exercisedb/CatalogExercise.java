package de.bebetter.backend.exercisedb;

import java.util.List;
// KI-Log Nr. 12
/** Übungsdatensatz aus ExerciseDB v1 – nur im Speicher, keine Tabelle (PDF: Uebung). */
public record CatalogExercise(String exerciseId, String name, List<String> bodyParts,
                              List<String> targetMuscles, List<String> equipments,
                              String gifUrl) {

    public String firstBodyPart() {
        return bodyParts == null || bodyParts.isEmpty() ? null : bodyParts.get(0);
    }

    public String firstTargetMuscle() {
        return targetMuscles == null || targetMuscles.isEmpty() ? null : targetMuscles.get(0);
    }

    public String firstEquipment() {
        return equipments == null || equipments.isEmpty() ? null : equipments.get(0);
    }
}