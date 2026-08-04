import { z } from "zod";

import type { MemorizationProgressCreateRequest } from "../types/index.ts";

const recitationGrades = [
  "EXCELLENT",
  "VERY_GOOD",
  "GOOD",
  "ACCEPTABLE",
  "POOR",
] as const;

const localDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

export const memorizationCreateSchema = z.object({
  studentId: z.uuid("Invalid student ID"),
  circleId: z.uuid("Invalid circle ID"),
  teacherId: z.uuid("Invalid teacher ID"),
  surahNumber: z.number().int().min(1).max(114),
  ayahFrom: z.number().int().min(1),
  ayahTo: z.number().int().min(1),
  grade: z.enum(recitationGrades).optional(),
  tajweedScore: z.number().int().min(0).max(100).optional(),
  teacherNotes: z.string().max(2000).optional(),
  audioUrl: z.string().url().optional().or(z.literal("")),
  sessionDate: localDate,
});

export type MemorizationCreateFormValues = z.infer<
  typeof memorizationCreateSchema
>;

export function toMemorizationCreateRequest(
  values: MemorizationCreateFormValues,
): MemorizationProgressCreateRequest {
  const body: MemorizationProgressCreateRequest = {
    studentId: values.studentId,
    circleId: values.circleId,
    teacherId: values.teacherId,
    surahNumber: values.surahNumber,
    ayahFrom: values.ayahFrom,
    ayahTo: values.ayahTo,
    sessionDate: values.sessionDate,
  };

  if (values.grade) {
    body.grade = values.grade;
  }
  if (values.tajweedScore !== undefined) {
    body.tajweedScore = values.tajweedScore;
  }
  if (values.teacherNotes) {
    body.teacherNotes = values.teacherNotes;
  }
  if (values.audioUrl) {
    body.audioUrl = values.audioUrl;
  }

  return body;
}
