import { z } from "zod";

import type { EnrollmentStatus } from "@/lib/types/api.ts";

const uuidSchema = z.uuid();

const enrollmentStatuses = [
  "PENDING",
  "ACTIVE",
  "WITHDRAWN",
  "COMPLETED",
  "REJECTED",
] as const satisfies readonly EnrollmentStatus[];

const localDateSchema = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

export const enrollmentCreateSchema = z.object({
  studentId: uuidSchema,
  circleId: uuidSchema,
  status: z.enum(enrollmentStatuses).optional(),
  enrolledDate: localDateSchema.optional(),
  approvedBy: uuidSchema.optional(),
  notes: z.string().optional(),
});

export const enrollmentUpdateSchema = z.object({
  status: z.enum(enrollmentStatuses).optional(),
  withdrawnDate: localDateSchema.optional(),
  notes: z.string().optional(),
});

export type EnrollmentCreateFormValues = z.infer<typeof enrollmentCreateSchema>;
export type EnrollmentUpdateFormValues = z.infer<typeof enrollmentUpdateSchema>;
