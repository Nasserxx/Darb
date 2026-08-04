import type {
  BigDecimal,
  CircleLevel,
  CircleStatus,
  CircleType,
  Instant,
  LocalTime,
  Uuid,
} from "@/lib/types/api.ts";

export interface CircleResponse {
  id: Uuid;
  mosqueId: Uuid;
  teacherId: Uuid;
  teacherName?: string;
  name: string;
  level: CircleLevel;
  type: CircleType;
  status: CircleStatus;
  capacity: number | null;
  startTime: LocalTime | null;
  endTime: LocalTime | null;
  daysOfWeek: string | null;
  roomOrLink: string | null;
  lateThresholdMinutes: number | null;
  monthlyFee: BigDecimal | null;
  createdAt: Instant;
}
