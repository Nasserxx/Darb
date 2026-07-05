import { z } from "zod";

const paymentStatuses = [
  "PENDING",
  "PARTIAL",
  "PAID",
  "OVERDUE",
  "WAIVED",
  "REFUNDED",
] as const;

const paymentMethods = [
  "CASH",
  "BANK_TRANSFER",
  "CARD",
  "ONLINE",
  "OTHER",
] as const;

const paymentCycles = [
  "MONTHLY",
  "QUARTERLY",
  "SEMESTER",
  "ANNUAL",
  "ONE_TIME",
] as const;

const localDateSchema = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Date must be YYYY-MM-DD");

const decimalSchema = z
  .string()
  .regex(/^\d+(\.\d+)?$/, "Amount must be a valid decimal");

export const paymentCreateSchema = z.object({
  studentId: z.uuid(),
  circleId: z.uuid(),
  mosqueId: z.uuid(),
  amount: decimalSchema,
  discount: decimalSchema.optional(),
  amountPaid: decimalSchema.optional(),
  status: z.enum(paymentStatuses).optional(),
  method: z.enum(paymentMethods).optional(),
  cycle: z.enum(paymentCycles).optional(),
  dueDate: localDateSchema,
  paidDate: localDateSchema.optional(),
  receiptUrl: z.string().optional(),
  recordedBy: z.uuid().optional(),
  notes: z.string().optional(),
});

export const paymentUpdateSchema = z.object({
  discount: decimalSchema.optional(),
  amountPaid: decimalSchema.optional(),
  status: z.enum(paymentStatuses).optional(),
  method: z.enum(paymentMethods).optional(),
  dueDate: localDateSchema.optional(),
  paidDate: localDateSchema.optional(),
  receiptUrl: z.string().optional(),
  notes: z.string().optional(),
});

export type PaymentCreateBody = z.infer<typeof paymentCreateSchema>;
export type PaymentUpdateBody = z.infer<typeof paymentUpdateSchema>;
