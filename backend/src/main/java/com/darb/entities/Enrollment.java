package com.darb.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import com.darb.entities.enums.EnrollmentStatus;
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Enrollment extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "enrolled_date", nullable = false)
    private LocalDate enrolledDate;

    @Column(name = "withdrawn_date")
    private LocalDate withdrawnDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "approved_by", nullable = false)
    private User approvedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
