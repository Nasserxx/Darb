import type { Gender, Instant, LocalDate, Uuid } from "@/lib/types/api.ts";

export interface TeacherResponse {
  id: Uuid;
  userId: Uuid;
  userName: string;
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

export interface TeacherProvisionRequest {
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
  specialization?: string | null;
  bio?: string | null;
  yearsExperience?: number | null;
  ijazahChain?: string | null;
}

export interface TeacherUpdateRequest {
  specialization?: string;
  bio?: string;
  yearsExperience?: number;
  ijazahChain?: string;
  isAvailable?: boolean;
}
