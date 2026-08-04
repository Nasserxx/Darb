import type { PageParams } from "@/lib/types/api.ts";

export const notificationKeys = {
  all: ["notifications"] as const,
  mine: (params?: PageParams) =>
    [...notificationKeys.all, "mine", params] as const,
};
