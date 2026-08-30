import { z } from "zod";

import { optionalAddressSchema } from "@/features/users/schemas/user-update.schema.ts";
import type { StudentProvisionRequest } from "../types/index.ts";

const uuidSchema = z.uuid();
const genders = ["MALE", "FEMALE"] as const;

export const studentInviteSchema = z.object({
  userId: uuidSchema,
  mosqueId: uuidSchema,
  medicalNotes: z.string().optional(),
  memorizedJuz: z.number().int().min(0).optional(),
});

export const studentProvisionSchema = z
  .object({
    mosqueId: uuidSchema,
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
    medicalNotes: z.string().optional(),
    memorizedJuz: z.number().int().min(0).optional(),
  })
  .merge(optionalAddressSchema)
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match.",
    path: ["confirmPassword"],
  });

export const studentCreateSchema = studentInviteSchema;

export const studentUpdateSchema = z.object({
  medicalNotes: z.string().optional(),
  memorizedJuz: z.number().int().min(0).optional(),
  totalAbsences: z.number().int().min(0).optional(),
  totalLateArrivals: z.number().int().min(0).optional(),
});

export type StudentInviteFormValues = z.infer<typeof studentInviteSchema>;
export type StudentCreateFormValues = StudentInviteFormValues;
export type StudentProvisionFormValues = z.infer<typeof studentProvisionSchema>;
export type StudentUpdateFormValues = z.infer<typeof studentUpdateSchema>;

function omitBlank(value: string | undefined): string | undefined {
  if (!value) return undefined;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

export function toStudentProvisionRequest(
  values: StudentProvisionFormValues,
): StudentProvisionRequest {
  const body: StudentProvisionRequest = {
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
  if (values.medicalNotes) body.medicalNotes = values.medicalNotes;
  if (values.memorizedJuz !== undefined) body.memorizedJuz = values.memorizedJuz;
  return body;
}
