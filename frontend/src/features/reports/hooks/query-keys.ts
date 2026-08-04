import type { PageParams } from "@/lib/types/api.ts";

export const reportKeys = {
  all: ["reports"] as const,
  detail: (id: string) => [...reportKeys.all, "detail", id] as const,
  mosque: (mosqueId: string, params?: PageParams) =>
    [...reportKeys.all, "mosque", mosqueId, params] as const,
};
