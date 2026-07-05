import type {
  GoalStatus,
  Instant,
  LocalDate,
  Uuid,
} from "@/lib/types/api.ts";

export interface GoalResponse {
  id: Uuid;
  studentId: Uuid;
  circleId: Uuid;
  title: string;
  targetSurah?: number;
  targetJuz?: number;
  status?: GoalStatus;
  dueDate?: LocalDate;
  completedDate?: LocalDate;
  setBy?: Uuid;
  createdAt?: Instant;
}

export interface GoalCreateRequest {
  studentId: Uuid;
  circleId: Uuid;
  title: string;
  targetSurah?: number;
  targetJuz?: number;
  status?: GoalStatus;
  dueDate?: LocalDate;
  setBy?: Uuid;
}

export interface GoalUpdateRequest {
  title?: string;
  targetSurah?: number;
  targetJuz?: number;
  status?: GoalStatus;
  dueDate?: LocalDate;
  completedDate?: LocalDate;
}
