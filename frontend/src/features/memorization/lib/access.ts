import type { UserRole } from "@/lib/types/api.ts";

interface WorkspaceProfile {
  studentId?: string;
  parentStudentIds?: string[];
}

export function canAccessStudentMemorization(
  role: UserRole,
  studentId: string,
  profile: WorkspaceProfile | null | undefined,
): boolean {
  if (role === "STUDENT") {
    return profile?.studentId === studentId;
  }
  if (role === "PARENT") {
    return profile?.parentStudentIds?.includes(studentId) ?? false;
  }
  return ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"].includes(role);
}
