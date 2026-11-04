
package de.bebetter.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
//KI-Log Nr. 11, 17
@Entity
@Table(name = "profil")
public class Profile {

    @Id
    @Column(name = "nutzer_id")
    private Long userId;

    @Column(name = "alter_jahre", nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(name = "geschlecht", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "groesse_cm", nullable = false)
    private int heightCm;

    @Column(name = "gewicht_kg", nullable = false, precision = 4, scale = 1)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "ziel", nullable = false, length = 15)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitnesslevel", nullable = false, length = 10)
    private FitnessLevel fitnessLevel;

    @Column(name = "trainingstage", nullable = false, length = 20)
    private String trainingDays;

    @Column(name = "erstellt_am", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "aktualisiert_am", nullable = false)
    private OffsetDateTime updatedAt;

    // Datum, ab dem neu geplant werden soll
    @Column(name = "neu_planen_ab")
    private java.time.LocalDate replanFrom;

    protected Profile() {
        // für JPA
    }

    public Profile(Long userId, OffsetDateTime now) {
        this.userId = userId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public List<Weekday> getTrainingDayList() {
        if (trainingDays == null || trainingDays.isBlank()) return List.of();
        return Arrays.stream(trainingDays.split(",")).map(Weekday::valueOf).sorted().toList();
    }

    public void setTrainingDayList(List<Weekday> days) {
        this.trainingDays = days.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    public Long getUserId() { return userId; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public int getHeightCm() { return heightCm; }
    public void setHeightCm(int heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Goal getGoal() { return goal; }
    public void setGoal(Goal goal) { this.goal = goal; }
    public FitnessLevel getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(FitnessLevel fitnessLevel) { this.fitnessLevel = fitnessLevel; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public java.time.LocalDate getReplanFrom() { return replanFrom; }
    public void setReplanFrom(java.time.LocalDate replanFrom) { this.replanFrom = replanFrom; }
}