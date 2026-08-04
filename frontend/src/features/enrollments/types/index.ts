import type {
  EnrollmentStatus,
  LocalDate,
  Uuid,
} from "@/lib/types/api.ts";

export interface EnrollmentResponse {
  studentName?: string;
  circleName?: string;
  id: Uuid;
  studentId: Uuid;
  circleId: Uuid;
  status: EnrollmentStatus;
  enrolledDate: LocalDate | null;
  withdrawnDate: LocalDate | null;
  approvedBy: Uuid | null;
  notes: string | null;
}

export interface EnrollmentCreateRequest {
  studentId: Uuid;
  circleId: Uuid;
  status?: EnrollmentStatus;
  enrolledDate?: LocalDate;
  approvedBy?: Uuid;
  notes?: string;
}

export interface EnrollmentUpdateRequest {
  status?: EnrollmentStatus;
  withdrawnDate?: LocalDate;
  notes?: string;
}
