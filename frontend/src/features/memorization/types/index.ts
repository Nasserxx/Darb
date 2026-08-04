import type {
  Instant,
  LocalDate,
  RecitationGrade,
  Uuid,
} from "@/lib/types/api.ts";

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
