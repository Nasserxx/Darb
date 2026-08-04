import { z } from "zod";

const uuidSchema = z.uuid();

export const parentStudentCreateSchema = z.object({
  parentUserId: uuidSchema,
  studentId: uuidSchema,
  relationship: z.string().max(50).optional(),
  isPrimary: z.boolean().optional(),
  receivesNotifications: z.boolean().optional(),
});

export const parentStudentUpdateSchema = z.object({
  relationship: z.string().max(50).optional(),
  isPrimary: z.boolean().optional(),
  receivesNotifications: z.boolean().optional(),
});

export type ParentStudentCreateFormValues = z.infer<
  typeof parentStudentCreateSchema
>;
export type ParentStudentUpdateFormValues = z.infer<
  typeof parentStudentUpdateSchema
>;
