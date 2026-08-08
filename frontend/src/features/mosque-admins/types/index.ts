import type { AdminPermission, Instant, Uuid } from "@/lib/types/api.ts";

export type { AdminPermission } from "@/lib/types/api.ts";

export interface MosqueAdminResponse {
  id: Uuid;
  userId: Uuid;
  userName: string;
  mosqueId: Uuid;
  mosqueName: string;
  permission: AdminPermission;
  isPrimaryAdmin: boolean;
  assignedAt: Instant;
  assignedBy: Uuid | null;
}

export interface MosqueAdminCreateRequest {
  userId: Uuid;
  mosqueId: Uuid;
  permission: AdminPermission;
  isPrimaryAdmin?: boolean;
  assignedBy: Uuid;
}

export interface MosqueAdminUpdateRequest {
  permission?: AdminPermission;
  isPrimaryAdmin?: boolean;
}
