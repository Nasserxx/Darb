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

import com.darb.entities.enums.GoalStatus;
@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Goal extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "target_surah")
    private Short targetSurah;

    @Column(name = "target_juz")
    private Short targetJuz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GoalStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "set_by", nullable = false)
    private User setBy;
}
