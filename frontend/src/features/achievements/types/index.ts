import type {
  AchievementType,
  Instant,
  LocalDate,
  Uuid,
} from "@/lib/types/api.ts";

export interface AchievementResponse {
  id: Uuid;
  studentId: Uuid;
  mosqueId: Uuid;
  type: AchievementType;
  title: string;
  description?: string;
  badgeUrl?: string;
  awardedBy?: Uuid;
  awardedDate?: LocalDate;
  createdAt?: Instant;
}

export interface AchievementCreateRequest {
  studentId: Uuid;
  mosqueId: Uuid;
  type: AchievementType;
  title: string;
  description?: string;
  badgeUrl?: string;
  awardedBy?: Uuid;
  awardedDate?: LocalDate;
}
