import type { Instant, Uuid } from "@/lib/types/api.ts";

export interface MosqueResponse {
  id: Uuid;
  name: string;
  city: string | null;
  phone: string | null;
  email: string | null;
  logoUrl: string | null;
  timezone: string | null;
  addressCountry?: string | null;
  addressPostalCode?: string | null;
  addressStreet?: string | null;
  addressHouseNumber?: string | null;
  addressState?: string | null;
  settings: string | null;
  isActive: boolean;
  createdAt: Instant;
}

export interface MosqueCreateRequest {
  name: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
}

export interface MosqueUpdateRequest {
  name?: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  addressCountry?: string;
  addressPostalCode?: string;
  addressStreet?: string;
  addressHouseNumber?: string;
  addressState?: string;
  settings?: string;
}
