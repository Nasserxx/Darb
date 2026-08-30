package com.darb.entities;

import com.darb.entities.enums.ParentRelationship;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "parent_student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ParentStudent extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mosque_id")
    private Mosque mosque;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ParentRelationship relationship;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "receives_notifications", nullable = false)
    private Boolean receivesNotifications;
}
