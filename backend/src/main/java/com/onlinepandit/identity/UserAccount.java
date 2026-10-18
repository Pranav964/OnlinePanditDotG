package com.onlinepandit.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, unique = true, length = 190)
    private String email;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(length = 30)
    private String phone;
    @Column(length = 80)
    private String city;
    @Column(length = 80)
    private String country;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserAccount() { }

    public UserAccount(String fullName, String email, String passwordHash, String phone, String city, String country, Role role) {
        this(fullName, email, passwordHash, phone, city, country, null, role);
    }

    public UserAccount(String fullName, String email, String passwordHash, String phone, String city, String country,
                       LocalDate dateOfBirth, Role role) {
        this.fullName = fullName;
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.city = city;
        this.country = country;
        this.dateOfBirth = dateOfBirth;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setRole(Role role) { this.role = role; }

    public enum Role { USER, PANDIT, ADMIN }
}
