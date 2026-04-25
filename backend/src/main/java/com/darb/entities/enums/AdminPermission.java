package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Permissions that can be granted to mosque administrators.
 */
@Getter
@RequiredArgsConstructor
public enum AdminPermission {
    FULL_ACCESS("full_access", "All mosque management permissions"),
    MANAGE_TEACHERS("manage_teachers", "Can manage teachers"),
    MANAGE_STUDENTS("manage_students", "Can manage students"),
    MANAGE_CIRCLES("manage_circles", "Can manage study circles"),
    MANAGE_PAYMENTS("manage_payments", "Can manage payments"),
    VIEW_REPORTS("view_reports", "Can view reports");

    private final String key;
    private final String description;
}
