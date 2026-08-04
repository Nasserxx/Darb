import { z } from "zod";

import type { GoalUpdateRequest } from "../types/index.ts";

const goalStatuses = [
  "IN_PROGRESS",
  "COMPLETED",
  "OVERDUE",
  "CANCELLED",
] as const;

const localDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

export const goalUpdateSchema = z.object({
  title: z.string().trim().min(1).max(200).optional(),
  targetSurah: z.number().int().min(1).max(114).optional(),
  targetJuz: z.number().int().min(1).max(30).optional(),
  status: z.enum(goalStatuses).optional(),
  dueDate: localDate.optional(),
  completedDate: localDate.optional(),
});

export type GoalUpdateFormValues = z.infer<typeof goalUpdateSchema>;

export function toGoalUpdateRequest(
  values: GoalUpdateFormValues,
): GoalUpdateRequest {
  const body: GoalUpdateRequest = {};

  if (values.title !== undefined) {
    body.title = values.title;
  }
  if (values.targetSurah !== undefined) {
    body.targetSurah = values.targetSurah;
  }
  if (values.targetJuz !== undefined) {
    body.targetJuz = values.targetJuz;
  }
  if (values.status !== undefined) {
    body.status = values.status;
  }
  if (values.dueDate !== undefined) {
    body.dueDate = values.dueDate;
  }
  if (values.completedDate !== undefined) {
    body.completedDate = values.completedDate;
  }

  return body;
}
