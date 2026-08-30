import { apiFetch } from "@/lib/api-client.ts";
import { withAuditReason } from "@/lib/api/audit-reason.ts";
import { mutateData } from "@/lib/api/pagination.ts";
import type { ApiResponse } from "@/lib/types/api.ts";
import type { MemberJoinRequestResponse } from "@/features/mosques/types/onboard.ts";

const BASE_PATH = "/api/v1/admin/stuck-work";
const MOSQUES_JOIN = "/api/v1/mosques/join-requests";
const ADMINS_JOIN = "/api/v1/mosque-admins/join-requests";

export type StuckWorkItemKind = "PENDING_JOIN";
export type JoinRequestDirection = "ADMIN_INVITE" | "MEMBER_REQUEST";

export interface StuckWorkItem {
  kind: StuckWorkItemKind;
  direction: JoinRequestDirection;
  mosqueId: string;
  mosqueName: string;
  resourceId: string;
  summary: string;
  createdAt: string;
  densityScore: number;
}

export const stuckWorkApi = {
  list: async (): Promise<StuckWorkItem[]> => {
    const response = await apiFetch<ApiResponse<StuckWorkItem[]>>(BASE_PATH, {
      auth: true,
    });
    return response.data ?? [];
  },

  /** ADMIN_INVITE — SA force-accept (consent override). */
  forceAccept: (id: string, auditReason: string) =>
    mutateData<MemberJoinRequestResponse>(`${MOSQUES_JOIN}/${id}/accept`, {
      method: "POST",
      headers: withAuditReason(undefined, auditReason),
    }),

  /** ADMIN_INVITE — SA cancel/withdraw invite. */
  cancelInvite: (id: string, auditReason: string) =>
    mutateData<MemberJoinRequestResponse>(`${MOSQUES_JOIN}/${id}/refuse`, {
      method: "POST",
      headers: withAuditReason(undefined, auditReason),
    }),

  /** MEMBER_REQUEST — SA approve. */
  approveRequest: (id: string, auditReason: string) =>
    mutateData<MemberJoinRequestResponse>(`${ADMINS_JOIN}/${id}/approve`, {
      method: "POST",
      headers: withAuditReason(undefined, auditReason),
    }),

  /** MEMBER_REQUEST — SA reject. */
  rejectRequest: (id: string, auditReason: string) =>
    mutateData<MemberJoinRequestResponse>(`${ADMINS_JOIN}/${id}/reject`, {
      method: "POST",
      headers: withAuditReason(undefined, auditReason),
    }),
};
