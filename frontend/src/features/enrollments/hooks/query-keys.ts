import type { PageParams } from "@/lib/types/api.ts";

export const enrollmentKeys = {
  all: ["enrollments"] as const,
  lists: () => [...enrollmentKeys.all, "list"] as const,
  list: (params: PageParams = {}) =>
    [...enrollmentKeys.lists(), params] as const,
  details: () => [...enrollmentKeys.all, "detail"] as const,
  detail: (id: string) => [...enrollmentKeys.details(), id] as const,
};
