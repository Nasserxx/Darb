import type {
  AbsenceReason,
  AttendanceStatus,
  Instant,
  LocalDate,
  LocalTime,
  Uuid,
} from "@/lib/types/api.ts";

export interface AttendanceResponse {
  id: Uuid;
  enrollmentId: Uuid;
  studentName: string;
  circleId: Uuid;
  circleName: string;
  sessionDate: LocalDate;
  status: AttendanceStatus;
  scheduledStart?: LocalTime;
  actualCheckIn?: LocalTime;
  minutesLate?: number;
  parentNotified?: boolean;
  absenceReason?: AbsenceReason;
  excuseDocumentUrl?: string;
  recordedBy?: Uuid;
  createdAt?: Instant;
}

export interface AttendanceCreateRequest {
  enrollmentId: Uuid;
  circleId: Uuid;
  sessionDate: LocalDate;
  status: AttendanceStatus;
  scheduledStart?: LocalTime;
  actualCheckIn?: LocalTime;
  minutesLate?: number;
  parentNotified?: boolean;
  absenceReason?: AbsenceReason;
  excuseDocumentUrl?: string;
  recordedBy?: Uuid;
}

export interface AttendanceUpdateRequest {
  status?: AttendanceStatus;
  actualCheckIn?: LocalTime;
  minutesLate?: number;
  parentNotified?: boolean;
  absenceReason?: AbsenceReason;
  excuseDocumentUrl?: string;
}
