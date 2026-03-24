package com.example.HelpNote.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // In a real app, this should be hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'FREE'")
    private PlanType planType = PlanType.FREE;

    private LocalDateTime planStartDate;

    @Column(columnDefinition = "integer default 0")
    private int dailyNoteCount = 0;

    @Column(columnDefinition = "integer default 0")
    private int dailyAtaCount = 0;

    private LocalDate lastUsageResetDate;

    public User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.planType = PlanType.FREE;
        this.lastUsageResetDate = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public LocalDateTime getPlanStartDate() {
        return planStartDate;
    }

    public void setPlanStartDate(LocalDateTime planStartDate) {
        this.planStartDate = planStartDate;
    }

    public int getDailyNoteCount() {
        return dailyNoteCount;
    }

    public void setDailyNoteCount(int dailyNoteCount) {
        this.dailyNoteCount = dailyNoteCount;
    }

    public int getDailyAtaCount() {
        return dailyAtaCount;
    }

    public void setDailyAtaCount(int dailyAtaCount) {
        this.dailyAtaCount = dailyAtaCount;
    }

    public LocalDate getLastUsageResetDate() {
        return lastUsageResetDate;
    }

    public void setLastUsageResetDate(LocalDate lastUsageResetDate) {
        this.lastUsageResetDate = lastUsageResetDate;
    }
}
