import type { PageParams } from "@/lib/types/api.ts";

export const mosqueKeys = {
  all: ["mosques"] as const,
  lists: () => [...mosqueKeys.all, "list"] as const,
  list: (params: PageParams = {}) => [...mosqueKeys.lists(), params] as const,
  details: () => [...mosqueKeys.all, "detail"] as const,
  detail: (id: string) => [...mosqueKeys.details(), id] as const,
  joinPreview: (code: string) => [...mosqueKeys.all, "join-preview", code] as const,
};
