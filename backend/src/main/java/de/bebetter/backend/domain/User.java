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
    private String passwortHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false, length = 10)
    private Role role;

    @Column(name = "registriert_am", nullable = false)
    private OffsetDateTime registriertAm;

    protected User() {
        // für JPA
    }

    public User(String name, String email, String passwortHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwortHash = passwortHash;
        this.role = role;
        this.registriertAm = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswortHash() { return passwortHash; }
    public Role getRolle() { return role; }
    public OffsetDateTime getRegistriertAm() { return registriertAm; }
}