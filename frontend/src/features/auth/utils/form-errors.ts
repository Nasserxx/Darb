import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import type { TFunction } from "i18next";

export function translateAuthApiMessage(message: string, t: TFunction): string {
  const key = `errors.api.${message}`;
  const translated = t(key, { ns: "auth", defaultValue: "" });
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
      message: translateAuthApiMessage(message, t),
    });
  }
}
