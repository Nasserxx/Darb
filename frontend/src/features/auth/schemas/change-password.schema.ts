import { z } from "zod";

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, "Current password is required"),
  newPassword: z
    .string()
    .min(8, "New password must be at least 8 characters")
    .max(128, "New password must be at most 128 characters"),
});

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

export type ChangePasswordRequestBody = {
  currentPassword: string;
  newPassword: string;
};
