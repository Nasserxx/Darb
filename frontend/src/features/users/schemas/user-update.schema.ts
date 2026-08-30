import { z } from "zod";

import type { Gender } from "@/lib/types/api.ts";

const genders = ["MALE", "FEMALE"] as const satisfies readonly Gender[];

/** Optional address fields shared by profile + people-edit address forms. */
export const optionalAddressSchema = z.object({
  addressCountry: z
    .string()
    .regex(/^[A-Z]{2}$/, "Country must be a two-letter ISO code")
    .optional()
    .or(z.literal("")),
  city: z
    .string()
    .trim()
    .max(100, "City must be at most 100 characters")
    .optional()
    .or(z.literal("")),
  addressStreet: z
    .string()
    .trim()
    .max(200, "Street must be at most 200 characters")
    .optional()
    .or(z.literal("")),
  addressHouseNumber: z
    .string()
    .trim()
    .max(20, "House number must be at most 20 characters")
    .optional()
    .or(z.literal("")),
  addressPostalCode: z
    .string()
    .trim()
    .max(20, "Postal code must be at most 20 characters")
    .optional()
    .or(z.literal("")),
  addressState: z
    .string()
    .trim()
    .max(100, "State must be at most 100 characters")
    .optional()
    .or(z.literal("")),
});

export type AddressFormValues = z.infer<typeof optionalAddressSchema>;

export const userUpdateSchema = z
  .object({
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
  })
  .merge(optionalAddressSchema);

export type UserUpdateFormValues = z.infer<typeof userUpdateSchema>;

export type UserUpdateRequestBody = {
  fullName?: string;
  phone?: string;
  gender?: Gender;
  dateOfBirth?: string;
  avatarUrl?: string;
  city?: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
};

function omitBlank(value: string | undefined): string | undefined {
  if (value === undefined) return undefined;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

export function toUserUpdateRequestBody(
  values: UserUpdateFormValues | AddressFormValues,
): UserUpdateRequestBody {
  const body: UserUpdateRequestBody = {};
  const full = values as UserUpdateFormValues;

  if (full.fullName !== undefined) {
    const fullName = omitBlank(full.fullName);
    if (fullName !== undefined) body.fullName = fullName;
  }
  if (full.phone !== undefined) {
    const phone = omitBlank(full.phone);
    if (phone !== undefined) body.phone = phone;
  }
  if (full.gender !== undefined) {
    body.gender = full.gender;
  }
  if (full.dateOfBirth !== undefined) {
    const dateOfBirth = omitBlank(full.dateOfBirth);
    if (dateOfBirth !== undefined) body.dateOfBirth = dateOfBirth;
  }
  if (full.avatarUrl !== undefined) {
    const avatarUrl = omitBlank(full.avatarUrl);
    if (avatarUrl !== undefined) body.avatarUrl = avatarUrl;
  }

  const addressCountry = omitBlank(values.addressCountry);
  if (addressCountry !== undefined) body.addressCountry = addressCountry;
  const city = omitBlank(values.city);
  if (city !== undefined) body.city = city;
  const addressStreet = omitBlank(values.addressStreet);
  if (addressStreet !== undefined) body.addressStreet = addressStreet;
  const addressHouseNumber = omitBlank(values.addressHouseNumber);
  if (addressHouseNumber !== undefined) {
    body.addressHouseNumber = addressHouseNumber;
  }
  const addressPostalCode = omitBlank(values.addressPostalCode);
  if (addressPostalCode !== undefined) {
    body.addressPostalCode = addressPostalCode;
  }
  const addressState = omitBlank(values.addressState);
  if (addressState !== undefined) body.addressState = addressState;

  return body;
}

export const emptyAddressValues: AddressFormValues = {
  addressCountry: "",
  city: "",
  addressStreet: "",
  addressHouseNumber: "",
  addressPostalCode: "",
  addressState: "",
};
