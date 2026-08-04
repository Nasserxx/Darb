/**
 * Maps API role strings (enum names like TEACHER, MOSQUE_ADMIN) to stable i18n keys.
 */
const ROLE_API_TO_KEY: Record<string, string> = {
  SUPER_ADMIN: "super_admin",
  MOSQUE_ADMIN: "mosque_admin",
  TEACHER: "teacher",
  STUDENT: "student",
  PARENT: "parent",
};

/**
 * Normalizes a role from the API (typically uppercase enum name) to a lowercase snake_case key.
 * Already-normalized keys are returned unchanged.
 */
export function normalizeRole(roleFromApi: string): string {
  const trimmed = roleFromApi.trim();
  if (!trimmed) {
    return trimmed;
  }

  const upper = trimmed.toUpperCase();
  const mapped = ROLE_API_TO_KEY[upper];
  if (mapped) {
    return mapped;
  }

  if (trimmed === trimmed.toLowerCase() && trimmed.includes("_")) {
    return trimmed;
  }

  return trimmed.toLowerCase();
}

export type RoleKey =
  | "super_admin"
  | "super_admin"
  | "mosque_admin"
  | "teacher"
  | "student"
  | "parent";
