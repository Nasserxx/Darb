import { z } from "zod";

import type { AttendanceCreateRequest } from "../types/index.ts";

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

const localDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

const localTime = z
  .string()
  .regex(/^\d{2}:\d{2}(:\d{2})?$/, "Time must be HH:mm or HH:mm:ss");

export const attendanceCreateSchema = z.object({
  enrollmentId: z.uuid("Invalid enrollment ID"),
  circleId: z.uuid("Invalid circle ID"),
  sessionDate: localDate,
  status: z.enum(attendanceStatuses),
  scheduledStart: localTime.optional(),
  actualCheckIn: localTime.optional(),
  minutesLate: z.number().int().min(0).optional(),
  parentNotified: z.boolean().optional(),
  absenceReason: z.enum(absenceReasons).optional(),
  excuseDocumentUrl: z.string().url().optional().or(z.literal("")),
  recordedBy: z.uuid().optional(),
});

export type AttendanceCreateFormValues = z.infer<typeof attendanceCreateSchema>;

export function toAttendanceCreateRequest(
  values: AttendanceCreateFormValues,
): AttendanceCreateRequest {
  const body: AttendanceCreateRequest = {
    enrollmentId: values.enrollmentId,
    circleId: values.circleId,
    sessionDate: values.sessionDate,
    status: values.status,
  };

  if (values.scheduledStart) {
    body.scheduledStart = values.scheduledStart;
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
  if (values.absenceReason) {
    body.absenceReason = values.absenceReason;
  }
  if (values.excuseDocumentUrl) {
    body.excuseDocumentUrl = values.excuseDocumentUrl;
  }
  if (values.recordedBy) {
    body.recordedBy = values.recordedBy;
  }

  return body;
}
