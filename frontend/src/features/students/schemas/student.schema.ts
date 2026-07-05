import { z } from "zod";

const uuidSchema = z.uuid();

export const studentCreateSchema = z.object({
  userId: uuidSchema,
  mosqueId: uuidSchema,
  nationalId: z.string().max(50).optional(),
  medicalNotes: z.string().optional(),
  memorizedJuz: z.number().int().min(0).optional(),
});

export const studentUpdateSchema = z.object({
  nationalId: z.string().max(50).optional(),
  medicalNotes: z.string().optional(),
  memorizedJuz: z.number().int().min(0).optional(),
  totalAbsences: z.number().int().min(0).optional(),
  totalLateArrivals: z.number().int().min(0).optional(),
});

export type StudentCreateFormValues = z.infer<typeof studentCreateSchema>;
export type StudentUpdateFormValues = z.infer<typeof studentUpdateSchema>;
