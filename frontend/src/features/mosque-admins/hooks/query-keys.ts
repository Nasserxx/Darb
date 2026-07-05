import type { PageParams } from "@/lib/types/api.ts";

export const mosqueAdminKeys = {
  all: ["mosque-admins"] as const,
  lists: () => [...mosqueAdminKeys.all, "list"] as const,
  list: (params: PageParams = {}) =>
    [...mosqueAdminKeys.lists(), params] as const,
  details: () => [...mosqueAdminKeys.all, "detail"] as const,
  detail: (id: string) => [...mosqueAdminKeys.details(), id] as const,
};
