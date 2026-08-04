import type {
  Gender,
  Instant,
  LocalDate,
  UserRole,
  Uuid,
} from "@/lib/types/api.ts";

export interface UserResponse {
  id: Uuid;
  fullName: string;
  email: string;
  phone: string | null;
  role: UserRole;
  gender: Gender | null;
  dateOfBirth: LocalDate | null;
  avatarUrl: string | null;
  isActive: boolean;
  lastLogin: Instant | null;
  createdAt: Instant;
}

export interface UserUpdateRequest {
  fullName?: string;
  phone?: string;
  gender?: Gender;
  dateOfBirth?: LocalDate;
  avatarUrl?: string;
}
