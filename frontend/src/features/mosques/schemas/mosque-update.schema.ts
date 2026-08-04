import { z } from "zod";

export const mosqueUpdateSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Mosque name is required")
    .max(200, "Mosque name must be at most 200 characters")
    .optional(),
  address: z.string().trim().optional(),
  city: z.string().trim().max(100, "City must be at most 100 characters").optional(),
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
  settings: z.string().trim().optional(),
});

export type MosqueUpdateFormValues = z.infer<typeof mosqueUpdateSchema>;

export type MosqueUpdateRequestBody = {
  name?: string;
  address?: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  settings?: string;
};

export function toMosqueUpdateRequestBody(
  values: MosqueUpdateFormValues,
): MosqueUpdateRequestBody {
  const body: MosqueUpdateRequestBody = {};
  if (values.name !== undefined) {
    body.name = values.name;
  }
  if (values.address !== undefined) {
    body.address = values.address;
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
  if (values.settings !== undefined) {
    body.settings = values.settings;
  }
  return body;
}
