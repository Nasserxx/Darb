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

import java.time.Instant;

import com.darb.entities.enums.AdminPermission;
@Entity
@Table(name = "mosque_admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MosqueAdmin extends BaseAuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mosque_id", nullable = false)
    private Mosque mosque;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 30)
    private AdminPermission permission;

    @Column(name = "is_primary_admin", nullable = false)
    private Boolean isPrimaryAdmin;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;
}
