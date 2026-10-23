package de.bebetter.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Ergebnis einer Planübung: geschafft/nicht geschafft mit Istwerten (PDF: Markierung). */
@Entity
@Table(name = "markierung")
public class ExerciseMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_uebung_id", unique = true)
    private PlanExercise planExercise;

    @Column(name = "datum", nullable = false)
    private LocalDate date;

    @Column(name = "geschafft", nullable = false)
    private boolean done;

    // null bei Körpergewicht und bei automatischen Fehlmarkierungen
    @Column(name = "gewicht_kg", precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "wiederholungen", nullable = false)
    private int reps;

    // true = beim Wochenabschluss automatisch als nicht geschafft ergänzt
    @Column(name = "automatisch", nullable = false)
    private boolean automatic;

    @Column(name = "erstellt_am", nullable = false)
    private OffsetDateTime createdAt;

    protected ExerciseMark() {}

    public ExerciseMark(LocalDate date, boolean done, BigDecimal weightKg, int reps,
                        boolean automatic, OffsetDateTime createdAt) {
        update(date, done, weightKg, reps);
        this.automatic = automatic;
        this.createdAt = createdAt;
    }

    /** Korrektur durch den Nutzer – dieselbe Zeile, dieselbe ID. */
    public void update(LocalDate date, boolean done, BigDecimal weightKg, int reps) {
        this.date = date;
        this.done = done;
        this.weightKg = weightKg;
        this.reps = reps;
        this.automatic = false;
    }

    void setPlanExercise(PlanExercise planExercise) { this.planExercise = planExercise; }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public boolean isDone() { return done; }
    public BigDecimal getWeightKg() { return weightKg; }
    public int getReps() { return reps; }
    public boolean isAutomatic() { return automatic; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}