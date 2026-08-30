import { z } from "zod";

type AuthT = (key: string, options?: Record<string, unknown>) => string;

export function createLoginSchema(t: AuthT) {
  return z.object({
    email: z.email(t("validation.emailInvalid")),
    password: z.string().min(1, t("validation.passwordRequired")),
  });
}

export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>;

export type LoginRequestBody = {
  email: string;
  password: string;
};
