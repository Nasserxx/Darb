import type { PageParams } from "@/lib/types/api.ts";

export const attendanceKeys = {
  all: ["attendance"] as const,
  lists: () => [...attendanceKeys.all, "list"] as const,
  list: (params: PageParams) => [...attendanceKeys.lists(), params] as const,
  circles: () => [...attendanceKeys.all, "circle"] as const,
  circle: (circleId: string, params: PageParams) =>
    [...attendanceKeys.circles(), circleId, params] as const,
  details: () => [...attendanceKeys.all, "detail"] as const,
  detail: (id: string) => [...attendanceKeys.details(), id] as const,
};
