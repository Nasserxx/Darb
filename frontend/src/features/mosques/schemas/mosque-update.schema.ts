import { z } from "zod";

export const mosqueUpdateSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Mosque name is required")
    .max(200, "Mosque name must be at most 200 characters"),
  city: z.string().trim().min(1, "City is required").max(100, "City must be at most 100 characters"),
  phone: z.string().trim().max(30, "Phone must be at most 30 characters").optional(),
  email: z
    .email("Invalid email format")
    .max(255, "Email must be at most 255 characters")
    .optional()
    .or(z.literal("")),
  logoUrl: z.string().trim().optional(),
  timezone: z
    .string()
    .trim()
    .max(50, "Timezone must be at most 50 characters")
    .optional(),
  addressCountry: z
    .string()
    .regex(/^[A-Z]{2}$/, "Country must be a two-letter ISO code")
    .optional()
    .or(z.literal("")),
  addressPostalCode: z.string().trim().max(20, "Postal code must be at most 20 characters").optional(),
  addressStreet: z.string().trim().max(200, "Street must be at most 200 characters").optional(),
  addressHouseNumber: z.string().trim().max(20, "House number must be at most 20 characters").optional(),
  addressState: z.string().trim().min(1, "State / Region is required").max(100, "State must be at most 100 characters"),
});

export type MosqueUpdateFormValues = z.infer<typeof mosqueUpdateSchema>;

export type MosqueUpdateRequestBody = {
  name?: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
};

export function toMosqueUpdateRequestBody(
  values: MosqueUpdateFormValues,
): MosqueUpdateRequestBody {
  const body: MosqueUpdateRequestBody = {};
  if (values.name !== undefined) {
    body.name = values.name;
  }
  if (values.city !== undefined) {
    body.city = values.city;
  }
  if (values.phone !== undefined) {
    body.phone = values.phone;
  }
  if (values.email !== undefined) {
    body.email = values.email;
  }
  if (values.logoUrl !== undefined) {
    body.logoUrl = values.logoUrl;
  }
  if (values.timezone !== undefined) {
    body.timezone = values.timezone;
  }
  if (values.addressCountry !== undefined) {
    body.addressCountry = values.addressCountry;
  }
  if (values.addressPostalCode !== undefined) {
    body.addressPostalCode = values.addressPostalCode;
  }
  if (values.addressStreet !== undefined) {
    body.addressStreet = values.addressStreet;
  }
  if (values.addressHouseNumber !== undefined) {
    body.addressHouseNumber = values.addressHouseNumber;
  }
  if (values.addressState !== undefined) {
    body.addressState = values.addressState;
  }
  return body;
}
