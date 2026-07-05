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

export const mosqueAdminUpdateSchema = z.object({
  permission: z.enum(adminPermissions).optional(),
  isPrimaryAdmin: z.boolean().optional(),
});

export type MosqueAdminUpdateFormValues = z.infer<typeof mosqueAdminUpdateSchema>;

export type MosqueAdminUpdateRequestBody = {
  permission?: AdminPermission;
  isPrimaryAdmin?: boolean;
};

export function toMosqueAdminUpdateRequestBody(
  values: MosqueAdminUpdateFormValues,
): MosqueAdminUpdateRequestBody {
  const body: MosqueAdminUpdateRequestBody = {};
  if (values.permission !== undefined) {
    body.permission = values.permission;
  }
  if (values.isPrimaryAdmin !== undefined) {
    body.isPrimaryAdmin = values.isPrimaryAdmin;
  }
  return body;
}
