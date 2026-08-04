import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import type { UserRole } from "@/lib/types/api.ts";

export function hasRole(userRole: string | undefined, allowed: UserRole[]): boolean {
  if (!userRole) return false;
  return allowed.includes(normalizeApiRole(userRole));
}

export const ADMIN_ROLES: UserRole[] = ["SUPER_ADMIN", "MOSQUE_ADMIN"];

export const STAFF_ROLES: UserRole[] = [...ADMIN_ROLES, "TEACHER"];

export function canManageStudents(userRole: string | undefined): boolean {
  return hasRole(userRole, ADMIN_ROLES);
}

export function canManageParentStudents(userRole: string | undefined): boolean {
  return hasRole(userRole, ADMIN_ROLES);
}

export function canManageCircles(userRole: string | undefined): boolean {
  return hasRole(userRole, ADMIN_ROLES);
}

export function canManageEnrollments(userRole: string | undefined): boolean {
  return hasRole(userRole, STAFF_ROLES);
}

export function canMarkAttendance(userRole: string | undefined): boolean {
  return hasRole(userRole, STAFF_ROLES);
}

export function canManageAchievements(userRole: string | undefined): boolean {
  return hasRole(userRole, STAFF_ROLES);
}

export function canManageMemorization(userRole: string | undefined): boolean {
  return hasRole(userRole, STAFF_ROLES);
}

export function canManageGoals(userRole: string | undefined): boolean {
  return hasRole(userRole, STAFF_ROLES);
}
