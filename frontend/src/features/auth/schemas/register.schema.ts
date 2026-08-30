import { z } from "zod";

type AuthT = (key: string, options?: Record<string, unknown>) => string;

const registerRoles = [
  "student",
  "teacher",
  "parent",
  "mosque_admin",
] as const;

const genders = ["MALE", "FEMALE"] as const;

export function createRegisterSchema(t: AuthT) {
  // ponytail: address has no auth.validation.* keys — keep constraints, drop EN copy
  const optionalAddress = {
    addressCountry: z
      .string()
      .regex(/^[A-Z]{2}$/)
      .optional()
      .or(z.literal("")),
    city: z.string().trim().max(100).optional().or(z.literal("")),
    addressStreet: z.string().trim().max(200).optional().or(z.literal("")),
    addressHouseNumber: z.string().trim().max(20).optional().or(z.literal("")),
    addressPostalCode: z.string().trim().max(20).optional().or(z.literal("")),
    addressState: z.string().trim().max(100).optional().or(z.literal("")),
  };

  return z
    .object({
      fullName: z
        .string()
        .trim()
        .min(1, t("validation.fullNameRequired"))
        .max(150, t("validation.fullNameMax", { max: 150 })),
      email: z
        .email(t("validation.emailInvalid"))
        .max(255),
      phone: z
        .string()
        .trim()
        .max(30, t("validation.phoneMax", { max: 30 }))
        .optional()
        .or(z.literal("")),
      password: z
        .string()
        .min(8, t("validation.passwordMin", { min: 8 }))
        .max(128, t("validation.passwordMax", { max: 128 })),
      confirmPassword: z
        .string()
        .min(1, t("validation.confirmPasswordRequired")),
      role: z.enum(registerRoles, {
        error: t("validation.roleInvalid"),
      }).optional().default("student"),
      gender: z
        .enum(genders, { error: t("validation.genderInvalid") })
        .optional(),
      dateOfBirth: z
        .string()
        .regex(/^\d{4}-\d{2}-\d{2}$/, t("validation.dateOfBirthInvalid"))
        .optional()
        .or(z.literal("")),
      ...optionalAddress,
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: t("validation.confirmPasswordMismatch"),
      path: ["confirmPassword"],
    });
}

export type RegisterFormValues = z.infer<ReturnType<typeof createRegisterSchema>>;

export type RegisterRequestBody = {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  role?: (typeof registerRoles)[number];
  gender?: (typeof genders)[number];
  dateOfBirth?: string;
  city?: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
};

function omitBlank(value: string | undefined): string | undefined {
  if (!value) return undefined;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

export function toRegisterRequestBody(
  values: RegisterFormValues,
): RegisterRequestBody {
  const body: RegisterRequestBody = {
    fullName: values.fullName,
    email: values.email,
    password: values.password,
  };
  const phone = omitBlank(values.phone);
  if (phone) body.phone = phone;
  if (values.role) body.role = values.role;
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

  return body;
}
