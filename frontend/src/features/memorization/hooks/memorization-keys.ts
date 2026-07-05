import type { PageParams } from "@/lib/types/api.ts";

export const memorizationKeys = {
  all: ["memorization"] as const,
  details: () => [...memorizationKeys.all, "detail"] as const,
  detail: (id: string) => [...memorizationKeys.details(), id] as const,
  students: () => [...memorizationKeys.all, "student"] as const,
  student: (studentId: string, params: PageParams) =>
    [...memorizationKeys.students(), studentId, params] as const,
  circles: () => [...memorizationKeys.all, "circle"] as const,
  circle: (circleId: string, params: PageParams) =>
    [...memorizationKeys.circles(), circleId, params] as const,
};
