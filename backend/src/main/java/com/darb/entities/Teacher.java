package com.darb.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Teacher extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mosque_id", nullable = false)
    private Mosque mosque;

    @Column(length = 150)
    private String specialization;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "ijazah_chain", length = 500)
    private String ijazahChain;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
}
