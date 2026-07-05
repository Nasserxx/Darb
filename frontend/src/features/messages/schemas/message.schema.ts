import { z } from "zod";

export const messageCreateSchema = z.object({
  senderId: z.uuid().optional(),
  receiverId: z.uuid(),
  circleId: z.uuid(),
  content: z.string().trim().min(1, "Message content is required"),
});

export type MessageCreateBody = z.infer<typeof messageCreateSchema>;
