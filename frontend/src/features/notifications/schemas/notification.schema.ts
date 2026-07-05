import { z } from "zod";

const notificationChannels = [
  "IN_APP",
  "SMS",
  "EMAIL",
  "PUSH",
] as const;

const notificationStatuses = [
  "PENDING",
  "SENT",
  "DELIVERED",
  "FAILED",
  "READ",
] as const;

export const notificationCreateSchema = z.object({
  mosqueId: z.uuid(),
  recipientUserId: z.uuid(),
  senderUserId: z.uuid().optional(),
  title: z
    .string()
    .trim()
    .min(1, "Title is required")
    .max(255, "Title must be at most 255 characters"),
  body: z.string().trim().min(1, "Body is required"),
  channel: z.enum(notificationChannels),
  status: z.enum(notificationStatuses).optional(),
  metadata: z.string().optional(),
});

export type NotificationCreateBody = z.infer<typeof notificationCreateSchema>;
