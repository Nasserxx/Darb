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

import com.darb.entities.enums.RecitationGrade;

@Entity
@Table(name = "memorization_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MemorizationProgress extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(name = "surah_number", nullable = false)
    private Short surahNumber;

    @Column(name = "ayah_from", nullable = false)
    private Short ayahFrom;

    @Column(name = "ayah_to", nullable = false)
    private Short ayahTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private RecitationGrade grade;

    @Column(name = "tajweed_score")
    private Integer tajweedScore;

    @Column(name = "teacher_notes", columnDefinition = "TEXT")
    private String teacherNotes;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
}
