export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type Uuid = string;
export type LocalDate = string;
export type LocalTime = string;
export type Instant = string;
export type BigDecimal = string;

export type UserRole =
  | "SUPER_ADMIN"
  | "MOSQUE_ADMIN"
  | "TEACHER"
  | "STUDENT"
  | "PARENT";

export type Gender = "MALE" | "FEMALE";

export type EnrollmentStatus =
  | "PENDING"
  | "ACTIVE"
  | "WITHDRAWN"
  | "COMPLETED"
  | "REJECTED";

export type AttendanceStatus =
  | "PRESENT"
  | "LATE"
  | "ABSENT"
  | "EXCUSED"
  | "HOLIDAY";

export type AbsenceReason = "SICK" | "FAMILY" | "TRAVEL" | "PERSONAL" | "OTHER";

export type CircleLevel =
  | "BEGINNER"
  | "INTERMEDIATE"
  | "ADVANCED"
  | "MEMORIZATION"
  | "IJAZAH";

export type CircleType = "IN_PERSON" | "ONLINE" | "HYBRID";

export type CircleStatus = "PLANNING" | "ACTIVE" | "PAUSED" | "ENDED";

export type GoalStatus = "IN_PROGRESS" | "COMPLETED" | "OVERDUE" | "CANCELLED";

export type RecitationGrade =
  | "EXCELLENT"
  | "VERY_GOOD"
  | "GOOD"
  | "ACCEPTABLE"
  | "POOR";

export type AchievementType =
  | "MEMORIZATION"
  | "ATTENDANCE"
  | "RECITATION"
  | "COMPETITION"
  | "MILESTONE";

export type PaymentStatus =
  | "PENDING"
  | "PARTIAL"
  | "PAID"
  | "OVERDUE"
  | "WAIVED"
  | "REFUNDED";

export type PaymentMethod =
  | "CASH"
  | "BANK_TRANSFER"
  | "CARD"
  | "ONLINE"
  | "OTHER";

export type PaymentCycle =
  | "MONTHLY"
  | "QUARTERLY"
  | "SEMESTER"
  | "ANNUAL"
  | "ONE_TIME";

export type NotificationChannel = "IN_APP" | "SMS" | "EMAIL" | "PUSH";

export type NotificationStatus =
  | "PENDING"
  | "SENT"
  | "DELIVERED"
  | "FAILED"
  | "READ";

export type MessageStatus = "SENT" | "DELIVERED" | "READ";

export type ReportType =
  | "ATTENDANCE_SUMMARY"
  | "FINANCIAL"
  | "STUDENT_PROGRESS"
  | "TEACHER_PERFORMANCE"
  | "ENROLLMENT";

export type AdminPermission =
  | "FULL_ACCESS"
  | "MANAGE_TEACHERS"
  | "MANAGE_STUDENTS"
  | "MANAGE_CIRCLES"
  | "MANAGE_PAYMENTS"
  | "VIEW_REPORTS";

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}
