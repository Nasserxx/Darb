import { z } from "zod";

import type { AttendanceUpdateRequest } from "../types/index.ts";

const attendanceStatuses = [
  "PRESENT",
  "LATE",
  "ABSENT",
  "EXCUSED",
  "HOLIDAY",
] as const;

const absenceReasons = [
  "SICK",
  "FAMILY",
  "TRAVEL",
  "PERSONAL",
  "OTHER",
] as const;

const localTime = z
  .string()
  .regex(/^\d{2}:\d{2}(:\d{2})?$/, "Time must be HH:mm or HH:mm:ss");

export const attendanceUpdateSchema = z.object({
  status: z.enum(attendanceStatuses).optional(),
  actualCheckIn: localTime.optional(),
  minutesLate: z.number().int().min(0).optional(),
  parentNotified: z.boolean().optional(),
  absenceReason: z.enum(absenceReasons).optional(),
  excuseDocumentUrl: z.string().url().optional().or(z.literal("")),
});

export type AttendanceUpdateFormValues = z.infer<typeof attendanceUpdateSchema>;

export function toAttendanceUpdateRequest(
  values: AttendanceUpdateFormValues,
): AttendanceUpdateRequest {
  const body: AttendanceUpdateRequest = {};

  if (values.status !== undefined) {
    body.status = values.status;
  }
  if (values.actualCheckIn) {
    body.actualCheckIn = values.actualCheckIn;
  }
  if (values.minutesLate !== undefined) {
    body.minutesLate = values.minutesLate;
  }
  if (values.parentNotified !== undefined) {
    body.parentNotified = values.parentNotified;
  }
  if (values.absenceReason !== undefined) {
    body.absenceReason = values.absenceReason;
  }
  if (values.excuseDocumentUrl) {
    body.excuseDocumentUrl = values.excuseDocumentUrl;
  }

  return body;
}
