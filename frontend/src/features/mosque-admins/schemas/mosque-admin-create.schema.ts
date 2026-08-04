import { z } from "zod";

import type { AdminPermission } from "@/lib/types/api.ts";

const adminPermissions = [
  "FULL_ACCESS",
  "MANAGE_TEACHERS",
  "MANAGE_STUDENTS",
  "MANAGE_CIRCLES",
  "MANAGE_PAYMENTS",
  "VIEW_REPORTS",
] as const satisfies readonly AdminPermission[];

export const mosqueAdminCreateSchema = z.object({
  userId: z.uuid("Invalid user ID"),
  mosqueId: z.uuid("Invalid mosque ID"),
  permission: z.enum(adminPermissions),
  isPrimaryAdmin: z.boolean().optional(),
});

export type MosqueAdminCreateFormValues = z.infer<typeof mosqueAdminCreateSchema>;

export type MosqueAdminCreateRequestBody = {
  userId: string;
  mosqueId: string;
  permission: AdminPermission;
  isPrimaryAdmin?: boolean;
};

export function toMosqueAdminCreateRequestBody(
  values: MosqueAdminCreateFormValues,
): MosqueAdminCreateRequestBody {
  const body: MosqueAdminCreateRequestBody = {
    userId: values.userId,
    mosqueId: values.mosqueId,
    permission: values.permission,
  };
  if (values.isPrimaryAdmin !== undefined) {
    body.isPrimaryAdmin = values.isPrimaryAdmin;
  }
  return body;
}
