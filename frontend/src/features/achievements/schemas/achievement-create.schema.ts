import { z } from "zod";

import type { AchievementCreateRequest } from "../types/index.ts";

const achievementTypes = [
  "MEMORIZATION",
  "ATTENDANCE",
  "RECITATION",
  "COMPETITION",
  "MILESTONE",
] as const;

const localDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

export const achievementCreateSchema = z.object({
  studentId: z.uuid("Invalid student ID"),
  mosqueId: z.uuid("Invalid mosque ID"),
  type: z.enum(achievementTypes),
  title: z.string().trim().min(1).max(200),
  description: z.string().max(2000).optional(),
  badgeUrl: z.string().url().optional().or(z.literal("")),
  awardedBy: z.uuid().optional(),
  awardedDate: localDate.optional(),
});

export type AchievementCreateFormValues = z.infer<
  typeof achievementCreateSchema
>;

export function toAchievementCreateRequest(
  values: AchievementCreateFormValues,
): AchievementCreateRequest {
  const body: AchievementCreateRequest = {
    studentId: values.studentId,
    mosqueId: values.mosqueId,
    type: values.type,
    title: values.title,
  };

  if (values.description) {
    body.description = values.description;
  }
  if (values.badgeUrl) {
    body.badgeUrl = values.badgeUrl;
  }
  if (values.awardedBy) {
    body.awardedBy = values.awardedBy;
  }
  if (values.awardedDate) {
    body.awardedDate = values.awardedDate;
  }

  return body;
}
