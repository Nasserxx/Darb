import type { Uuid } from "@/lib/types/api.ts";

export interface ParentStudentResponse {
  id: Uuid;
  parentUserId: Uuid;
  studentId: Uuid;
  relationship: string | null;
  isPrimary: boolean;
  receivesNotifications: boolean;
}

export interface ParentStudentCreateRequest {
  parentUserId: Uuid;
  studentId: Uuid;
  relationship?: string;
  isPrimary?: boolean;
  receivesNotifications?: boolean;
}

export interface ParentStudentUpdateRequest {
  relationship?: string;
  isPrimary?: boolean;
  receivesNotifications?: boolean;
}
