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
import java.time.LocalTime;

import com.darb.entities.enums.AbsenceReason;
import com.darb.entities.enums.AttendanceStatus;
@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Attendance extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AttendanceStatus status;

    @Column(name = "scheduled_start", nullable = false)
    private LocalTime scheduledStart;

    @Column(name = "actual_check_in")
    private LocalTime actualCheckIn;

    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(name = "parent_notified")
    private Boolean parentNotified;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_reason", length = 20)
    private AbsenceReason absenceReason;

    @Column(name = "excuse_document_url", columnDefinition = "TEXT")
    private String excuseDocumentUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;
}
