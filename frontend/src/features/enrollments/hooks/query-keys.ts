import type { PageParams } from "@/lib/types/api.ts";

export const enrollmentKeys = {
  all: ["enrollments"] as const,
  lists: () => [...enrollmentKeys.all, "list"] as const,
  list: (params: PageParams = {}) =>
    [...enrollmentKeys.lists(), params] as const,
  details: () => [...enrollmentKeys.all, "detail"] as const,
  detail: (id: string) => [...enrollmentKeys.details(), id] as const,
  students: () => [...enrollmentKeys.all, "student"] as const,
  student: (studentId: string, params: PageParams = {}) =>
    [...enrollmentKeys.students(), studentId, params] as const,
};
