import { z } from "zod";

import type {
  CircleLevel,
  CircleStatus,
  CircleType,
} from "@/lib/types/api.ts";

const uuidSchema = z.uuid();

const circleLevels = [
  "BEGINNER",
  "INTERMEDIATE",
  "ADVANCED",
  "MEMORIZATION",
  "IJAZAH",
] as const satisfies readonly CircleLevel[];

const circleTypes = ["IN_PERSON", "ONLINE", "HYBRID"] as const satisfies readonly CircleType[];

const circleStatuses = [
  "PLANNING",
  "ACTIVE",
  "PAUSED",
  "ENDED",
] as const satisfies readonly CircleStatus[];

const localTimeSchema = z
  .string()
  .regex(/^\d{2}:\d{2}(:\d{2})?$/, "Time must be HH:mm or HH:mm:ss");

export const circleCreateSchema = z.object({
  mosqueId: uuidSchema,
  teacherId: uuidSchema,
  name: z.string().trim().min(1).max(200),
  level: z.enum(circleLevels),
  type: z.enum(circleTypes),
  status: z.enum(circleStatuses).optional(),
  capacity: z.number().int().positive().optional(),
  startTime: localTimeSchema.optional(),
  endTime: localTimeSchema.optional(),
  daysOfWeek: z.string().max(100).optional(),
  roomOrLink: z.string().optional(),
  lateThresholdMinutes: z.number().int().min(0).optional(),
  monthlyFee: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, "Fee must be a decimal string")
    .optional(),
});

export const circleUpdateSchema = z.object({
  name: z.string().trim().min(1).max(200).optional(),
  level: z.enum(circleLevels).optional(),
  type: z.enum(circleTypes).optional(),
  status: z.enum(circleStatuses).optional(),
  capacity: z.number().int().positive().optional(),
  startTime: localTimeSchema.optional(),
  endTime: localTimeSchema.optional(),
  daysOfWeek: z.string().max(100).optional(),
  roomOrLink: z.string().optional(),
  lateThresholdMinutes: z.number().int().min(0).optional(),
  monthlyFee: z
    .string()
    .regex(/^\d+(\.\d{1,2})?$/, "Fee must be a decimal string")
    .optional(),
});

export type CircleCreateFormValues = z.infer<typeof circleCreateSchema>;
export type CircleUpdateFormValues = z.infer<typeof circleUpdateSchema>;
