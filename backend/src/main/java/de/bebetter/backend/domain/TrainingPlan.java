package de.bebetter.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
// KI-Log Nr. 12, 15
/** Wochenplan eines Nutzers (PDF: Trainingsplan). */
@Entity
@Table(name = "trainingsplan")
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nutzer_id", nullable = false)
    private Long userId;

    @Column(name = "wochen_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "wochen_ende", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "erstellt_am", nullable = false)
    private OffsetDateTime createdAt;

    // Sprint 4: true = Woche abgeschlossen, nicht mehr bearbeitbar
    @Column(name = "ausgewertet", nullable = false)
    private boolean evaluated;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<TrainingSession> sessions = new ArrayList<>();

    protected TrainingPlan() {}

    public TrainingPlan(Long userId, LocalDate weekStart, OffsetDateTime createdAt) {
        this.userId = userId;
        this.weekStart = weekStart;
        this.weekEnd = weekStart.plusDays(6);
        this.createdAt = createdAt;
    }

    public void addSession(TrainingSession session) {
        session.setPlan(this);
        sessions.add(session);
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(weekStart) && !day.isAfter(weekEnd);
    }

    public boolean isEvaluated() { return evaluated; }
    public void setEvaluated(boolean evaluated) { this.evaluated = evaluated; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getWeekStart() { return weekStart; }
    public LocalDate getWeekEnd() { return weekEnd; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<TrainingSession> getSessions() { return sessions; }
}