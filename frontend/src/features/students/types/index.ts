import type { EnrollmentStatus, Instant, Uuid } from "@/lib/types/api.ts";

export interface StudentResponse {
  id: Uuid;
  userId: Uuid;
  mosqueId: Uuid;
  mosqueName: string;
  nationalId: string | null;
  fullName: string | null;
  medicalNotes: string | null;
  memorizedJuz: number | null;
  totalAbsences: number;
  totalLateArrivals: number;
  status: EnrollmentStatus;
  enrolledAt: Instant;
  parentInviteCode?: string | null;
}

export interface StudentCreateRequest {
  userId: Uuid;
  mosqueId: Uuid;
  nationalId?: string;
  medicalNotes?: string;
  memorizedJuz?: number;
  parentInviteCode?: string;
}

export interface StudentUpdateRequest {
  nationalId?: string;
  medicalNotes?: string;
  memorizedJuz?: number;
  totalAbsences?: number;
  totalLateArrivals?: number;
  parentInviteCode?: string;
}
