import { z } from "zod";

export const teacherUpdateSchema = z.object({
  specialization: z
    .string()
    .trim()
    .max(200, "Specialization must be at most 200 characters")
    .optional(),
  bio: z.string().trim().optional(),
  yearsExperience: z
    .number()
    .int("Years of experience must be a whole number")
    .min(0, "Years of experience cannot be negative")
    .optional(),
  ijazahChain: z
    .string()
    .trim()
    .max(255, "Ijazah chain must be at most 255 characters")
    .optional(),
  isAvailable: z.boolean().optional(),
});

export type TeacherUpdateFormValues = z.infer<typeof teacherUpdateSchema>;

export type TeacherUpdateRequestBody = {
  specialization?: string;
  bio?: string;
  yearsExperience?: number;
  ijazahChain?: string;
  isAvailable?: boolean;
};

export function toTeacherUpdateRequestBody(
  values: TeacherUpdateFormValues,
): TeacherUpdateRequestBody {
  const body: TeacherUpdateRequestBody = {};
  if (values.specialization !== undefined) {
    body.specialization = values.specialization;
  }
  if (values.bio !== undefined) {
    body.bio = values.bio;
  }
  if (values.yearsExperience !== undefined) {
    body.yearsExperience = values.yearsExperience;
  }
  if (values.ijazahChain !== undefined) {
    body.ijazahChain = values.ijazahChain;
  }
  if (values.isAvailable !== undefined) {
    body.isAvailable = values.isAvailable;
  }
  return body;
}
