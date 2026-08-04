import { z } from "zod";

const reportTypes = [
  "ATTENDANCE_SUMMARY",
  "FINANCIAL",
  "STUDENT_PROGRESS",
  "TEACHER_PERFORMANCE",
  "ENROLLMENT",
] as const;

export const reportCreateSchema = z.object({
  mosqueId: z.uuid(),
  generatedBy: z.uuid().optional(),
  type: z.enum(reportTypes),
  title: z
    .string()
    .trim()
    .min(1, "Title is required")
    .max(255, "Title must be at most 255 characters"),
  filters: z.string().optional(),
  fileUrl: z.string().optional(),
});

export type ReportCreateBody = z.infer<typeof reportCreateSchema>;
