import { z } from "zod";

const registerRoles = [
  "student",
  "teacher",
  "parent",
  "mosque_admin",
] as const;

const genders = ["MALE", "FEMALE"] as const;

export const registerSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(1, "Full name is required")
      .max(150, "Full name must be at most 150 characters"),
    email: z
      .email("Invalid email format")
      .max(255, "Email must be at most 255 characters"),
    phone: z
      .string()
      .trim()
      .max(30, "Phone must be at most 30 characters")
      .optional()
      .or(z.literal("")),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .max(128, "Password must be at most 128 characters"),
    confirmPassword: z.string().min(1, "Please confirm your password."),
    role: z.enum(registerRoles).optional().default("student"),
    gender: z.enum(genders).optional(),
    dateOfBirth: z
      .string()
      .regex(/^\d{4}-\d{2}-\d{2}$/, "Date of birth must be YYYY-MM-DD")
      .optional()
      .or(z.literal("")),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match.",
    path: ["confirmPassword"],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;

export type RegisterRequestBody = {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  role?: (typeof registerRoles)[number];
  gender?: (typeof genders)[number];
  dateOfBirth?: string;
};

export function toRegisterRequestBody(
  values: RegisterFormValues,
): RegisterRequestBody {
  const body: RegisterRequestBody = {
    fullName: values.fullName,
    email: values.email,
    password: values.password,
  };
  if (values.phone) {
    body.phone = values.phone;
  }
  if (values.role) {
    body.role = values.role;
  }
  if (values.gender) {
    body.gender = values.gender;
  }
  if (values.dateOfBirth) {
    body.dateOfBirth = values.dateOfBirth;
  }
  return body;
}
