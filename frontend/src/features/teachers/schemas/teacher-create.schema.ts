import { z } from "zod";

import { optionalAddressSchema } from "@/features/users/schemas/user-update.schema.ts";
import type { TeacherProvisionRequest } from "../types/index.ts";

const genders = ["MALE", "FEMALE"] as const;

export const teacherInviteSchema = z.object({
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

export const teacherCreateSchema = teacherInviteSchema;

export type TeacherCreateFormValues = z.infer<typeof teacherInviteSchema>;
export type TeacherInviteFormValues = TeacherCreateFormValues;

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

export const teacherProvisionSchema = z
  .object({
    mosqueId: z.uuid("Invalid mosque ID"),
    fullName: z
      .string()
      .trim()
      .min(1, "Full name is required")
      .max(150, "Full name must be at most 150 characters"),
    email: z
      .email("Invalid email format")
      .max(255, "Email must be at most 255 characters"),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .max(128, "Password must be at most 128 characters"),
    confirmPassword: z.string().min(1, "Please confirm your password."),
    phone: z
      .string()
      .trim()
      .max(30, "Phone must be at most 30 characters")
      .optional()
      .or(z.literal("")),
    gender: z.enum(genders).optional(),
    dateOfBirth: z
      .string()
      .regex(/^\d{4}-\d{2}-\d{2}$/, "Date of birth must be YYYY-MM-DD")
      .optional()
      .or(z.literal("")),
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
  })
  .merge(optionalAddressSchema)
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match.",
    path: ["confirmPassword"],
  });

export type TeacherProvisionFormValues = z.infer<typeof teacherProvisionSchema>;

function omitBlank(value: string | undefined): string | undefined {
  if (!value) return undefined;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

export function toTeacherProvisionRequest(
  values: TeacherProvisionFormValues,
): TeacherProvisionRequest {
  const body: TeacherProvisionRequest = {
    mosqueId: values.mosqueId,
    fullName: values.fullName,
    email: values.email,
    password: values.password,
  };
  const phone = omitBlank(values.phone);
  if (phone) body.phone = phone;
  if (values.gender) body.gender = values.gender;
  const dateOfBirth = omitBlank(values.dateOfBirth);
  if (dateOfBirth) body.dateOfBirth = dateOfBirth;
  const addressCountry = omitBlank(values.addressCountry);
  if (addressCountry) body.addressCountry = addressCountry;
  const city = omitBlank(values.city);
  if (city) body.city = city;
  const addressStreet = omitBlank(values.addressStreet);
  if (addressStreet) body.addressStreet = addressStreet;
  const addressHouseNumber = omitBlank(values.addressHouseNumber);
  if (addressHouseNumber) body.addressHouseNumber = addressHouseNumber;
  const addressPostalCode = omitBlank(values.addressPostalCode);
  if (addressPostalCode) body.addressPostalCode = addressPostalCode;
  const addressState = omitBlank(values.addressState);
  if (addressState) body.addressState = addressState;
  if (values.specialization) body.specialization = values.specialization;
  if (values.bio) body.bio = values.bio;
  if (values.yearsExperience !== undefined) {
    body.yearsExperience = values.yearsExperience;
  }
  if (values.ijazahChain) body.ijazahChain = values.ijazahChain;
  return body;
}
