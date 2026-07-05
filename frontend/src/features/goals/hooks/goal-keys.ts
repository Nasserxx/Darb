import type { PageParams } from "@/lib/types/api.ts";

export const goalKeys = {
  all: ["goals"] as const,
  details: () => [...goalKeys.all, "detail"] as const,
  detail: (id: string) => [...goalKeys.details(), id] as const,
  students: () => [...goalKeys.all, "student"] as const,
  student: (studentId: string, params: PageParams) =>
    [...goalKeys.students(), studentId, params] as const,
};
