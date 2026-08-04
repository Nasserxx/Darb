import { z } from "zod";

import type { Gender } from "@/lib/types/api.ts";

const genders = ["MALE", "FEMALE"] as const satisfies readonly Gender[];

export const userUpdateSchema = z.object({
  fullName: z
    .string()
    .trim()
    .min(1, "Full name is required")
    .max(150, "Full name must be at most 150 characters")
    .optional(),
  phone: z.string().trim().max(30, "Phone must be at most 30 characters").optional(),
  gender: z.enum(genders).optional(),
  dateOfBirth: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Date of birth must be YYYY-MM-DD")
    .optional(),
  avatarUrl: z.string().trim().optional(),
});

export type UserUpdateFormValues = z.infer<typeof userUpdateSchema>;

export type UserUpdateRequestBody = {
  fullName?: string;
  phone?: string;
  gender?: Gender;
  dateOfBirth?: string;
  avatarUrl?: string;
};

export function toUserUpdateRequestBody(
  values: UserUpdateFormValues,
): UserUpdateRequestBody {
  const body: UserUpdateRequestBody = {};
  if (values.fullName !== undefined) {
    body.fullName = values.fullName;
  }
  if (values.phone !== undefined) {
    body.phone = values.phone;
  }
  if (values.gender !== undefined) {
    body.gender = values.gender;
  }
  if (values.dateOfBirth !== undefined) {
    body.dateOfBirth = values.dateOfBirth;
  }
  if (values.avatarUrl !== undefined) {
    body.avatarUrl = values.avatarUrl;
  }
  return body;
}
