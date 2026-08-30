import type { ParentRelationship, Uuid } from "@/lib/types/api.ts";

export interface ParentStudentResponse {
  id: Uuid;
  parentUserId: Uuid;
  parentName: string;
  studentId: Uuid;
  studentName: string;
  relationship: ParentRelationship | null;
  isPrimary: boolean;
  receivesNotifications: boolean;
}

export interface ParentStudentCreateRequest {
  parentUserId: Uuid;
  studentId: Uuid;
  relationship?: ParentRelationship;
  isPrimary?: boolean;
  receivesNotifications?: boolean;
}

export interface ParentStudentUpdateRequest {
  parentUserId: Uuid;
  studentId: Uuid;
  relationship?: ParentRelationship;
  isPrimary?: boolean;
  receivesNotifications?: boolean;
}
