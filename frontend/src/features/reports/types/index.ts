import type { Instant, ReportType, Uuid } from "@/lib/types/api.ts";

export interface ReportResponse {
  id: Uuid;
  mosqueId: Uuid;
  generatedBy: Uuid;
  type: ReportType;
  title: string;
  filters?: string | null;
  fileUrl?: string | null;
  generatedAt: Instant;
}

export interface ReportCreateRequest {
  mosqueId: Uuid;
  generatedBy: Uuid;
  type: ReportType;
  title: string;
  filters?: string;
  fileUrl?: string;
}
