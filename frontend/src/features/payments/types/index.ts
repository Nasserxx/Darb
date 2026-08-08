import type {
  BigDecimal,
  Instant,
  LocalDate,
  PaymentCycle,
  PaymentMethod,
  PaymentStatus,
  Uuid,
} from "@/lib/types/api.ts";

export interface PaymentResponse {
  id: Uuid;
  studentName: string;
  studentId: Uuid;
  circleId: Uuid;
  mosqueId: Uuid;
  amount: BigDecimal;
  discount?: BigDecimal | null;
  amountPaid?: BigDecimal | null;
  status: PaymentStatus;
  method?: PaymentMethod | null;
  cycle?: PaymentCycle | null;
  dueDate: LocalDate;
  paidDate?: LocalDate | null;
  receiptUrl?: string | null;
  recordedBy?: Uuid | null;
  notes?: string | null;
  createdAt: Instant;
}

export interface PaymentCreateRequest {
  studentId: Uuid;
  circleId: Uuid;
  mosqueId: Uuid;
  amount: BigDecimal;
  discount?: BigDecimal;
  amountPaid?: BigDecimal;
  status?: PaymentStatus;
  method?: PaymentMethod;
  cycle?: PaymentCycle;
  dueDate: LocalDate;
  paidDate?: LocalDate;
  receiptUrl?: string;
  recordedBy?: Uuid;
  notes?: string;
}

export interface PaymentUpdateRequest {
  discount?: BigDecimal;
  amountPaid?: BigDecimal;
  status?: PaymentStatus;
  method?: PaymentMethod;
  dueDate?: LocalDate;
  paidDate?: LocalDate;
  receiptUrl?: string;
  notes?: string;
}
