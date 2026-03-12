package com.tulumcore.api.entities;

import jakarta.persistence.*;

@Entity
public class Usuario extends BaseEntity {

    // NOTA: Si BaseEntity ya tiene el @Id, borrá estas dos líneas (id).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // Cambiado para coincidir con el Frontend

    @Column(nullable = false)
    private String password;

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}