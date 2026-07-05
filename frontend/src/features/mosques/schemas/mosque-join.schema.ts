import { z } from "zod";

export const mosqueJoinSchema = z.object({
  inviteCode: z
    .string()
    .trim()
    .min(8, "Invite code must be at least 8 characters")
    .max(32, "Invite code must be at most 32 characters"),
});

export type MosqueJoinFormValues = z.infer<typeof mosqueJoinSchema>;

export type MosqueJoinRequestBody = {
  inviteCode: string;
};

export function toMosqueJoinRequestBody(
  values: MosqueJoinFormValues,
): MosqueJoinRequestBody {
  return { inviteCode: values.inviteCode.trim() };
}
