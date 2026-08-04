import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import type { TFunction } from "i18next";

import { ApiError } from "../api-client.ts";
import type { ApiResponse } from "../types/api.ts";

export function extractFieldErrors(body: unknown): Record<string, string> | undefined {
  if (!body || typeof body !== "object" || !("data" in body)) {
    return undefined;
  }
  const data = (body as { data?: unknown }).data;
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return undefined;
  }
  const entries = Object.entries(data).filter(
    (entry): entry is [string, string] => typeof entry[1] === "string",
  );
  return entries.length > 0 ? Object.fromEntries(entries) : undefined;
}

export function translateApiMessage(message: string, t: TFunction): string {
  const translated = t(`errors.api.${message}`, { defaultValue: "" });
  return translated || message;
}

export function applyFieldErrors<T extends FieldValues>(
  fieldErrors: Record<string, string>,
  setError: UseFormSetError<T>,
  t: TFunction,
): void {
  for (const [field, message] of Object.entries(fieldErrors)) {
    setError(field as Path<T>, {
      type: "server",
      message: translateApiMessage(message, t),
    });
  }
}

export function toMutationError(error: unknown, t: TFunction): {
  message: string;
  fieldErrors?: Record<string, string>;
} {
  if (error instanceof ApiError) {
    const fieldErrors = extractFieldErrors(error.body);
    return {
      message: translateApiMessage(error.message, t),
      ...(fieldErrors ? { fieldErrors } : {}),
    };
  }
  if (error instanceof Error) {
    return { message: error.message };
  }
  return { message: t("errors.unknown", { ns: "common" }) };
}

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  if (!response.data) {
    throw new Error(response.message || "Empty response");
  }
  return response.data;
}
