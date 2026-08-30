import type {
  Instant,
  LocalDate,
  RecitationGrade,
  Uuid,
} from "@/lib/types/api.ts";
import type { PageHalf } from "@/features/mushaf/types/index.ts";

export type StampType = "TAJWEED" | "HIFZ";

export type CoverageGrain = "juz" | "page" | "half";

export interface MemorizationStamp {
  type: StampType;
  surah: number;
  ayah: number;
  x?: number;
  y?: number;
}

export interface MemorizationAttemptResponse {
  id: Uuid;
  studentId: Uuid;
  circleId: Uuid;
  page: number;
  half: PageHalf;
  edition: string;
  sessionDate: LocalDate;
  assessorId: Uuid;
  grade?: RecitationGrade;
  notes?: string;
  stamps: MemorizationStamp[];
  tajweedCount: number;
  hifzCount: number;
  createdAt?: Instant;
  updatedAt?: Instant;
}

export interface MemorizationAttemptCreateRequest {
  circleId: Uuid;
  page: number;
  half: PageHalf;
  sessionDate: LocalDate;
  grade?: RecitationGrade;
  notes?: string;
  stamps: MemorizationStamp[];
}

export interface LessonAssignmentResponse {
  id: Uuid;
  studentId: Uuid;
  circleId: Uuid;
  halfPageIds: string[];
  note?: string;
  assignedBy: Uuid;
  assignedAt: Instant;
}

export interface LessonAssignmentUpsertRequest {
  circleId: Uuid;
  halfPageIds: string[];
  note?: string;
}

export interface CoverageEntry {
  key: string;
  juz?: number;
  page?: number;
  half?: PageHalf;
  assessed: boolean;
  grade?: RecitationGrade;
  tajweedCount?: number;
  hifzCount?: number;
  assessedHalves?: number;
  totalHalves?: number;
  progressPercent?: number;
}

export interface CoverageResponse {
  grain: CoverageGrain;
  circleId: Uuid;
  entries: CoverageEntry[];
}

export interface MemorizationProgressResponse {
  id: Uuid;
  studentId: Uuid;
  circleId: Uuid;
  teacherId: Uuid;
  surahNumber: number;
  ayahFrom: number;
  ayahTo: number;
  grade?: RecitationGrade;
  tajweedScore?: number;
  teacherNotes?: string;
  audioUrl?: string;
  sessionDate: LocalDate;
  createdAt?: Instant;
}

export interface MemorizationProgressCreateRequest {
  studentId: Uuid;
  circleId: Uuid;
  teacherId: Uuid;
  surahNumber: number;
  ayahFrom: number;
  ayahTo: number;
  grade?: RecitationGrade;
  tajweedScore?: number;
  teacherNotes?: string;
  audioUrl?: string;
  sessionDate: LocalDate;
}

export interface MemorizationProgressUpdateRequest {
  grade?: RecitationGrade;
  tajweedScore?: number;
  teacherNotes?: string;
  audioUrl?: string;
}
