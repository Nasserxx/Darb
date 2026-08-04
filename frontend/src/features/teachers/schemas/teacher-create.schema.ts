import { z } from "zod";

export const teacherCreateSchema = z.object({
  userId: z.uuid("Invalid user ID"),
  mosqueId: z.uuid("Invalid mosque ID"),
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
});

export type TeacherCreateFormValues = z.infer<typeof teacherCreateSchema>;

export type TeacherCreateRequestBody = {
  userId: string;
  mosqueId: string;
  specialization?: string;
  bio?: string;
  yearsExperience?: number;
  ijazahChain?: string;
};

export function toTeacherCreateRequestBody(
  values: TeacherCreateFormValues,
): TeacherCreateRequestBody {
  const body: TeacherCreateRequestBody = {
    userId: values.userId,
    mosqueId: values.mosqueId,
  };
  if (values.specialization) {
    body.specialization = values.specialization;
  }
  if (values.bio) {
    body.bio = values.bio;
  }
  if (values.yearsExperience !== undefined) {
    body.yearsExperience = values.yearsExperience;
  }
  if (values.ijazahChain) {
    body.ijazahChain = values.ijazahChain;
  }
  return body;
}
