import type { PageParams } from "@/lib/types/api.ts";

export const parentStudentKeys = {
  all: ["parent-students"] as const,
  lists: () => [...parentStudentKeys.all, "list"] as const,
  list: (params: PageParams = {}) =>
    [...parentStudentKeys.lists(), params] as const,
  details: () => [...parentStudentKeys.all, "detail"] as const,
  detail: (id: string) => [...parentStudentKeys.details(), id] as const,
};
