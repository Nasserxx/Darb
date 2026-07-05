import { z } from "zod";

export const mosqueCreateSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Mosque name is required")
    .max(200, "Mosque name must be at most 200 characters"),
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
});

export type MosqueCreateFormValues = z.infer<typeof mosqueCreateSchema>;

export type MosqueCreateRequestBody = {
  name: string;
  address?: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
};

export function toMosqueCreateRequestBody(
  values: MosqueCreateFormValues,
): MosqueCreateRequestBody {
  const body: MosqueCreateRequestBody = { name: values.name };
  if (values.address) {
    body.address = values.address;
  }
  if (values.city) {
    body.city = values.city;
  }
  if (values.phone) {
    body.phone = values.phone;
  }
  if (values.email) {
    body.email = values.email;
  }
  if (values.logoUrl) {
    body.logoUrl = values.logoUrl;
  }
  if (values.timezone) {
    body.timezone = values.timezone;
  }
  return body;
}
