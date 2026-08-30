package com.darb.entities;

import com.darb.entities.enums.PageHalf;
import com.darb.entities.enums.RecitationGrade;
import com.darb.entities.enums.StampType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "memorization_attempt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "circle_id", "page", "half"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MemorizationAttempt extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "circle_id", nullable = false)
    private Circle circle;

    @Column(nullable = false)
    private Short page;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private PageHalf half;

    @Column(nullable = false, length = 20)
    private String edition;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assessor_id", nullable = false)
    private User assessor;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private RecitationGrade grade;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<AttemptStamp> stamps = new ArrayList<>();

    @Column(name = "tajweed_count", nullable = false)
    private Integer tajweedCount;

    @Column(name = "hifz_count", nullable = false)
    private Integer hifzCount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttemptStamp {
        private StampType type;
        private Integer surah;
        private Integer ayah;
        private Double x;
        private Double y;
    }
}
