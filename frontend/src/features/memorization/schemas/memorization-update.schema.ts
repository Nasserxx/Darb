import { z } from "zod";

import type { MemorizationProgressUpdateRequest } from "../types/index.ts";

const recitationGrades = [
  "EXCELLENT",
  "VERY_GOOD",
  "GOOD",
  "ACCEPTABLE",
  "POOR",
] as const;

export const memorizationUpdateSchema = z.object({
  grade: z.enum(recitationGrades).optional(),
  tajweedScore: z.number().int().min(0).max(100).optional(),
  teacherNotes: z.string().max(2000).optional(),
  audioUrl: z.string().url().optional().or(z.literal("")),
});

export type MemorizationUpdateFormValues = z.infer<
  typeof memorizationUpdateSchema
>;

export function toMemorizationUpdateRequest(
  values: MemorizationUpdateFormValues,
): MemorizationProgressUpdateRequest {
  const body: MemorizationProgressUpdateRequest = {};

  if (values.grade !== undefined) {
    body.grade = values.grade;
  }
  if (values.tajweedScore !== undefined) {
    body.tajweedScore = values.tajweedScore;
  }
  if (values.teacherNotes !== undefined) {
    body.teacherNotes = values.teacherNotes;
  }
  if (values.audioUrl) {
    body.audioUrl = values.audioUrl;
  }

  return body;
}
