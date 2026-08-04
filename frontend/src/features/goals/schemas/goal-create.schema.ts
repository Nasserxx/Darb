import { z } from "zod";

import type { GoalCreateRequest } from "../types/index.ts";

const goalStatuses = [
  "IN_PROGRESS",
  "COMPLETED",
  "OVERDUE",
  "CANCELLED",
] as const;

const localDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

export const goalCreateSchema = z.object({
  studentId: z.uuid("Invalid student ID"),
  circleId: z.uuid("Invalid circle ID"),
  title: z.string().trim().min(1).max(200),
  targetSurah: z.number().int().min(1).max(114).optional(),
  targetJuz: z.number().int().min(1).max(30).optional(),
  status: z.enum(goalStatuses).optional(),
  dueDate: localDate.optional(),
  setBy: z.uuid().optional(),
});

export type GoalCreateFormValues = z.infer<typeof goalCreateSchema>;

export function toGoalCreateRequest(
  values: GoalCreateFormValues,
): GoalCreateRequest {
  const body: GoalCreateRequest = {
    studentId: values.studentId,
    circleId: values.circleId,
    title: values.title,
  };

  if (values.targetSurah !== undefined) {
    body.targetSurah = values.targetSurah;
  }
  if (values.targetJuz !== undefined) {
    body.targetJuz = values.targetJuz;
  }
  if (values.status) {
    body.status = values.status;
  }
  if (values.dueDate) {
    body.dueDate = values.dueDate;
  }
  if (values.setBy) {
    body.setBy = values.setBy;
  }

  return body;
}
