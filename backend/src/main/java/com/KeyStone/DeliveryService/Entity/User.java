package com.KeyStone.DeliveryService.Entity;

import com.KeyStone.DeliveryService.Enum.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    // Only meaningful for role == CUSTOMER: links a customer-portal login
    // to the organisation (Customer) it may act on behalf of. Set once via
    // POST /api/auth/link-customer (self-service) or by a manager editing
    // the user record — never trust a client-supplied customerId elsewhere.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public User() {
    }

    public User(
            Integer id,
            String name,
            String email,
            String passwordHash,
            Role role,
            Instant createdAt) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }
}