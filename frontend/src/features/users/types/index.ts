import type {
  Gender,
  Instant,
  LocalDate,
  PageParams,
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
  city?: string | null;
  addressCountry?: string | null;
  addressPostalCode?: string | null;
  addressStreet?: string | null;
  addressHouseNumber?: string | null;
  addressState?: string | null;
}

export interface UserPickerResponse {
  id: Uuid;
  fullName: string;
  dateOfBirth: LocalDate | null;
  city?: string | null;
  addressCountry?: string | null;
  addressState?: string | null;
}

export type UserPickerOccupancyRole = "STUDENT" | "TEACHER" | "PARENT";

export interface UserPickerParams extends PageParams {
  q?: string;
  country?: string;
  state?: string;
  city?: string;
  dateOfBirth?: string;
  mosqueId?: string;
  role?: UserPickerOccupancyRole;
}

export interface UserUpdateRequest {
  fullName?: string;
  phone?: string;
  gender?: Gender;
  dateOfBirth?: LocalDate;
  avatarUrl?: string;
  city?: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
}
