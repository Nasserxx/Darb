import type { PageParams } from "@/lib/types/api.ts";

export const paymentKeys = {
  all: ["payments"] as const,
  list: (params?: PageParams) => [...paymentKeys.all, "list", params] as const,
  detail: (id: string) => [...paymentKeys.all, "detail", id] as const,
  mosque: (mosqueId: string, params?: PageParams) =>
    [...paymentKeys.all, "mosque", mosqueId, params] as const,
};
