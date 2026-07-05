import type { PageParams } from "@/lib/types/api.ts";

export const achievementKeys = {
  all: ["achievements"] as const,
  details: () => [...achievementKeys.all, "detail"] as const,
  detail: (id: string) => [...achievementKeys.details(), id] as const,
  students: () => [...achievementKeys.all, "student"] as const,
  student: (studentId: string, params: PageParams) =>
    [...achievementKeys.students(), studentId, params] as const,
  mosques: () => [...achievementKeys.all, "mosque"] as const,
  mosque: (mosqueId: string, params: PageParams) =>
    [...achievementKeys.mosques(), mosqueId, params] as const,
};
