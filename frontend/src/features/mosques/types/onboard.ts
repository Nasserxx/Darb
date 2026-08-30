import type { MosqueAdminResponse } from "@/features/mosque-admins/types/index.ts";
import type { ParentRelationship } from "@/lib/types/api.ts";

import type { MosqueResponse } from "./index.ts";

export type JoinRequestDirection = "MEMBER_REQUEST" | "ADMIN_INVITE";

export interface MosqueOnboardResponse {
  mosque: MosqueResponse;
  admin: MosqueAdminResponse;
  inviteCode?: string;
  teacherInviteCode?: string;
  studentInviteCode?: string;
}

export interface MosqueSearchResult {
  id: string;
  name: string;
  city: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
}

export interface MemberJoinRequestResponse {
  id: string;
  userId: string;
  userFullName: string;
  userEmail: string;
  mosqueId: string;
  mosqueName: string;
  requestedRole: string;
  status: string;
  createdAt: string;
  direction?: JoinRequestDirection;
  linkedStudentId?: string | null;
  relationship?: ParentRelationship | null;
}

export interface MosqueInviteCodesResponse {
  adminInviteCode?: string;
  teacherInviteCode?: string;
  studentInviteCode?: string;
}

export interface MosqueJoinPreviewResponse {
  mosqueName: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
}

export interface MosqueJoinRequest {
  inviteCode: string;
}
