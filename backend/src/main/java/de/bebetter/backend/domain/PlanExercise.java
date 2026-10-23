package de.bebetter.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Gespeicherte Vorgabe einer Übung in einer Einheit (PDF: PlanUebung). */
@Entity
@Table(name = "plan_uebung")
public class PlanExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "einheit_id")
    private TrainingSession session;

    @Column(name = "position_nr", nullable = false)
    private int position;

    @Column(name = "exercise_id", nullable = false, length = 20)
    private String exerciseId;

    @Column(name = "koerperteil", nullable = false, length = 20)
    private String bodyPart;

    @Column(name = "saetze", nullable = false)
    private int sets;

    @Column(name = "wiederholungen", nullable = false, length = 10)
    private String reps;

    // null = Körpergewicht ohne Zusatzgewicht
    @Column(name = "gewicht_kg", precision = 5, scale = 1)
    private BigDecimal weightKg;

    // Sprint 4: höchstens eine Markierung pro Planübung
    @OneToOne(mappedBy = "planExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private ExerciseMark mark;

    protected PlanExercise() {}

    public PlanExercise(int position, String exerciseId, String bodyPart, int sets,
                        String reps, BigDecimal weightKg) {
        this.position = position;
        this.exerciseId = exerciseId;
        this.bodyPart = bodyPart;
        this.sets = sets;
        this.reps = reps;
        this.weightKg = weightKg;
    }

    public ExerciseMark getMark() { return mark; }

    public void setMark(ExerciseMark mark) {
        this.mark = mark;
        mark.setPlanExercise(this);
    }

    public Long getId() { return id; }
    public TrainingSession getSession() { return session; }
    void setSession(TrainingSession session) { this.session = session; }
    public int getPosition() { return position; }
    public String getExerciseId() { return exerciseId; }
    public String getBodyPart() { return bodyPart; }
    public int getSets() { return sets; }
    public String getReps() { return reps; }
    public BigDecimal getWeightKg() { return weightKg; }
    public boolean isBodyweight() { return weightKg == null; }
}