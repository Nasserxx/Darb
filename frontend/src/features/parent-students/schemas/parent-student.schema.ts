import { z } from "zod";

import type { ParentRelationship } from "@/lib/types/api.ts";

export const PARENT_RELATIONSHIPS = [
  "FATHER",
  "MOTHER",
  "STEPFATHER",
  "STEPMOTHER",
  "GRANDFATHER",
  "GRANDMOTHER",
  "UNCLE",
  "AUNT",
  "BROTHER",
  "SISTER",
  "GUARDIAN",
  "PARENT",
  "OTHER",
] as const satisfies readonly ParentRelationship[];

/** Shared create/update shape (ponytail: one schema for both modes). */
export const parentStudentFormSchema = z.object({
  parentUserId: z.uuid("Select a parent"),
  studentId: z.uuid("Select a student"),
  relationship: z.enum(PARENT_RELATIONSHIPS).optional(),
  isPrimary: z.boolean().optional(),
  receivesNotifications: z.boolean().optional(),
});

export const parentStudentCreateSchema = parentStudentFormSchema;
export const parentStudentUpdateSchema = parentStudentFormSchema;

export type ParentStudentFormValues = z.infer<typeof parentStudentFormSchema>;
export type ParentStudentCreateFormValues = ParentStudentFormValues;
export type ParentStudentUpdateFormValues = ParentStudentFormValues;
