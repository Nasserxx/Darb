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

import java.math.BigDecimal;
import java.time.LocalTime;

import com.darb.entities.enums.CircleLevel;
import com.darb.entities.enums.CircleStatus;
import com.darb.entities.enums.CircleType;

@Entity
@Table(name = "circles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Circle extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "mosque_id", nullable = false)
    private Mosque mosque;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CircleLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CircleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CircleStatus status;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "days_of_week", length = 100)
    private String daysOfWeek;

    @Column(name = "room_or_link", columnDefinition = "TEXT")
    private String roomOrLink;

    @Column(name = "late_threshold_minutes")
    private Integer lateThresholdMinutes;

    @Column(name = "monthly_fee", precision = 10, scale = 2)
    private BigDecimal monthlyFee;
}
