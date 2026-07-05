import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import type { PageParams } from "@/lib/types/api.ts";
import {
  createPayment,
  getMosquePayments,
  getPayment,
  getPayments,
  updatePayment,
} from "../api/payments-api.ts";
import type {
  PaymentCreateRequest,
  PaymentUpdateRequest,
} from "../types/index.ts";
import { paymentKeys } from "./query-keys.ts";

export function usePayments(params?: PageParams) {
  return useQuery({
    queryKey: paymentKeys.list(params),
    queryFn: () => getPayments(params),
  });
}

export function usePayment(id: string | undefined) {
  return useQuery({
    queryKey: paymentKeys.detail(id ?? ""),
    queryFn: () => getPayment(id!),
    enabled: !!id,
  });
}

export function useMosquePayments(
  mosqueId: string | undefined,
  params?: PageParams,
) {
  return useQuery({
    queryKey: paymentKeys.mosque(mosqueId ?? "", params),
    queryFn: () => getMosquePayments(mosqueId!, params),
    enabled: !!mosqueId,
  });
}

export function useCreatePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: PaymentCreateRequest) => createPayment(body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentKeys.all });
    },
  });
}

export function useUpdatePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: PaymentUpdateRequest }) =>
      updatePayment(id, body),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentKeys.all });
    },
  });
}
