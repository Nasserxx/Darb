import type { PageParams } from "@/lib/types/api.ts";

export const messageKeys = {
  all: ["messages"] as const,
  mine: (params?: PageParams) => [...messageKeys.all, "mine", params] as const,
  circle: (circleId: string, params?: PageParams) =>
    [...messageKeys.all, "circle", circleId, params] as const,
};
