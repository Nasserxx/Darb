import type {
  EnrollmentStatus,
  Gender,
  Instant,
  LocalDate,
  Uuid,
} from "@/lib/types/api.ts";

export interface StudentResponse {
  id: Uuid;
  userId: Uuid;
  mosqueId: Uuid;
  mosqueName: string;
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
  medicalNotes?: string;
  memorizedJuz?: number;
  parentInviteCode?: string;
}

export interface StudentProvisionRequest {
  mosqueId: Uuid;
  fullName: string;
  email: string;
  password: string;
  phone?: string | null;
  gender?: Gender | null;
  dateOfBirth?: LocalDate | null;
  addressCountry?: string | null;
  addressState?: string | null;
  city?: string | null;
  addressPostalCode?: string | null;
  addressStreet?: string | null;
  addressHouseNumber?: string | null;
  medicalNotes?: string | null;
  memorizedJuz?: number | null;
}

export interface StudentUpdateRequest {
  medicalNotes?: string;
  memorizedJuz?: number;
  totalAbsences?: number;
  totalLateArrivals?: number;
  parentInviteCode?: string;
}
