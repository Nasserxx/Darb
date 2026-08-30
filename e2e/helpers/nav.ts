/**
 * English sidebar labels for MOSQUE_ADMIN nav items.
 * Mirrors frontend/src/lib/navigation/app-nav.ts + app-en.json labelKey values.
 */
export const MOSQUE_ADMIN_SIDEBAR_LABELS = [
  "Dashboard",
  "Mosques",
  "Teachers",
  "Students",
  "Parent links",
  "Circles",
  "Enrollments",
  "Attendance",
  "Achievements",
  "Reports",
  "Profile",
] as const;

/** Minimum nav items asserted in role-matrix tests. */
export const MOSQUE_ADMIN_REQUIRED_NAV = ["Dashboard", "Students", "Circles"] as const;
