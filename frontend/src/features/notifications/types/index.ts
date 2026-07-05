import type {
  Instant,
  NotificationChannel,
  NotificationStatus,
  Uuid,
} from "@/lib/types/api.ts";

export interface NotificationResponse {
  id: Uuid;
  mosqueId: Uuid;
  recipientUserId: Uuid;
  senderUserId: Uuid;
  title: string;
  body: string;
  channel: NotificationChannel;
  status: NotificationStatus;
  sentAt?: Instant | null;
  deliveredAt?: Instant | null;
  metadata?: string | null;
}

export interface NotificationCreateRequest {
  mosqueId: Uuid;
  recipientUserId: Uuid;
  senderUserId: Uuid;
  title: string;
  body: string;
  channel: NotificationChannel;
  status?: NotificationStatus;
  metadata?: string;
}
