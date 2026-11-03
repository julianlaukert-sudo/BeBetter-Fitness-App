package de.bebetter.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
//KI-Log Nr. 10, 11
// Tabellen- und Spaltennamen bleiben deutsch (V2__nutzer.sql).
@Entity
@Table(name = "nutzer")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "passwort_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false, length = 10)
    private Role role;

    @Column(name = "registriert_am", nullable = false)
    private OffsetDateTime registeredAt;

    protected User() {
        // für JPA
    }

    // Zeitpunkt wird übergeben, damit Tests mit einer festen Uhr (Clock) arbeiten können
    public User(String name, String email, String passwordHash, Role role, OffsetDateTime registeredAt) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.registeredAt = registeredAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}