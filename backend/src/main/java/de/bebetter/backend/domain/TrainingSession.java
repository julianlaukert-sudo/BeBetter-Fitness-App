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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Eine Trainingseinheit A, B oder C (PDF: Einheit). */
@Entity
@Table(name = "einheit")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private TrainingPlan plan;

    @Column(nullable = false, length = 1)
    private String label;

    @Column(name = "position_nr", nullable = false)
    private int position;

    @Column(name = "datum", nullable = false)
    private LocalDate date;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PlanExercise> exercises = new ArrayList<>();

    protected TrainingSession() {}

    public TrainingSession(String label, int position, LocalDate date) {
        this.label = label;
        this.position = position;
        this.date = date;
    }

    public void addExercise(PlanExercise exercise) {
        exercise.setSession(this);
        exercises.add(exercise);
    }

    public Long getId() { return id; }
    public TrainingPlan getPlan() { return plan; }
    void setPlan(TrainingPlan plan) { this.plan = plan; }
    public String getLabel() { return label; }
    public int getPosition() { return position; }
    public LocalDate getDate() { return date; }
    public List<PlanExercise> getExercises() { return exercises; }
}