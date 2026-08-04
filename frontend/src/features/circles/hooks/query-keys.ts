import type { PageParams } from "@/lib/types/api.ts";

export const circleKeys = {
  all: ["circles"] as const,
  lists: () => [...circleKeys.all, "list"] as const,
  list: (params: PageParams = {}) =>
    [...circleKeys.lists(), params] as const,
  details: () => [...circleKeys.all, "detail"] as const,
  detail: (id: string) => [...circleKeys.details(), id] as const,
};
