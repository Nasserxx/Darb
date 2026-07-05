import {
  fetchData,
  fetchPage,
  mutateData,
} from "@/lib/api/pagination.ts";
import type { PageParams } from "@/lib/types/api.ts";
import type {
  PaymentCreateRequest,
  PaymentResponse,
  PaymentUpdateRequest,
} from "../types/index.ts";

const BASE = "/api/v1/payments";

export function getPayments(params?: PageParams) {
  return fetchPage<PaymentResponse>(BASE, params);
}

export function getPayment(id: string) {
  return fetchData<PaymentResponse>(`${BASE}/${id}`);
}

export function getMosquePayments(mosqueId: string, params?: PageParams) {
  return fetchPage<PaymentResponse>(`${BASE}/mosque/${mosqueId}`, params);
}

export function createPayment(body: PaymentCreateRequest) {
  return mutateData<PaymentResponse>(BASE, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updatePayment(id: string, body: PaymentUpdateRequest) {
  return mutateData<PaymentResponse>(`${BASE}/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
