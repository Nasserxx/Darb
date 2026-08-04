import type {
  Instant,
  MessageStatus,
  Uuid,
} from "@/lib/types/api.ts";

export interface MessageResponse {
  id: Uuid;
  senderId: Uuid;
  receiverId: Uuid;
  circleId: Uuid;
  content: string;
  status: MessageStatus;
  sentAt: Instant;
  readAt?: Instant | null;
}

export interface MessageCreateRequest {
  senderId: Uuid;
  receiverId: Uuid;
  circleId: Uuid;
  content: string;
}
