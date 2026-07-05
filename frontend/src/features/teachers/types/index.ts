import type { Instant, Uuid } from "@/lib/types/api.ts";

export interface TeacherResponse {
  id: Uuid;
  userId: Uuid;
  mosqueId: Uuid;
  specialization: string | null;
  bio: string | null;
  yearsExperience: number | null;
  ijazahChain: string | null;
  isAvailable: boolean;
  isActive: boolean;
  joinedAt: Instant;
}

export interface TeacherCreateRequest {
  userId: Uuid;
  mosqueId: Uuid;
  specialization?: string;
  bio?: string;
  yearsExperience?: number;
  ijazahChain?: string;
}

export interface TeacherUpdateRequest {
  specialization?: string;
  bio?: string;
  yearsExperience?: number;
  ijazahChain?: string;
  isAvailable?: boolean;
}
