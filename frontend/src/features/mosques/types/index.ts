import type { Instant, Uuid } from "@/lib/types/api.ts";

export interface MosqueResponse {
  id: Uuid;
  name: string;
  address: string | null;
  city: string | null;
  phone: string | null;
  email: string | null;
  logoUrl: string | null;
  timezone: string | null;
  settings: string | null;
  isActive: boolean;
  createdAt: Instant;
}

export interface MosqueCreateRequest {
  name: string;
  address?: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
}

export interface MosqueUpdateRequest {
  name?: string;
  address?: string;
  city?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  settings?: string;
}
