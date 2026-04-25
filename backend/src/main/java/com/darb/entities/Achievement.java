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

import com.darb.entities.enums.AchievementType;
@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Achievement extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mosque_id", nullable = false)
    private Mosque mosque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AchievementType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "badge_url", columnDefinition = "TEXT")
    private String badgeUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "awarded_by", nullable = false)
    private User awardedBy;

    @Column(name = "awarded_date")
    private LocalDate awardedDate;
}
