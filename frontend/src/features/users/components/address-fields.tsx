import { useState } from "react";
import type {
  FieldErrors,
  FieldValues,
  UseFormRegister,
  UseFormSetValue,
  UseFormWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import { Combobox } from "@/components/ui/combobox.tsx";
import {
  Field,
  FieldDescription,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import { getCountryOptions } from "@/lib/countries.ts";

type AddressFieldsProps<TFormValues extends FieldValues> = {
  register: UseFormRegister<TFormValues>;
  errors: FieldErrors<TFormValues>;
  watch: UseFormWatch<TFormValues>;
  setValue: UseFormSetValue<TFormValues>;
  idPrefix?: string;
  /** When set (or defaultOpen), wraps fields in a native collapsible. */
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  summaryLabel?: string;
};

function watchString<TFormValues extends FieldValues>(
  watch: UseFormWatch<TFormValues>,
  key: string,
): string | undefined {
  return watch(key as never) as unknown as string | undefined;
}

function fieldError<TFormValues extends FieldValues>(
  errors: FieldErrors<TFormValues>,
  key: string,
): { message?: string } | undefined {
  return (errors as Record<string, { message?: string } | undefined>)[key];
}

export function AddressFields<TFormValues extends FieldValues>({
  register,
  errors,
  watch,
  setValue,
  idPrefix = "address",
  open,
  defaultOpen,
  onOpenChange,
  summaryLabel,
}: AddressFieldsProps<TFormValues>) {
  const { t, i18n } = useTranslation("app");
  const countryOptions = getCountryOptions(i18n.language);
  const collapsible = open !== undefined || defaultOpen !== undefined;
  const [internalOpen, setInternalOpen] = useState(defaultOpen ?? false);
  const isOpen = open ?? internalOpen;

  const errorFor = (key: string) => fieldError(errors, key);

  const fields = (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <Field
        className="sm:col-span-2"
        data-invalid={!!errorFor("addressCountry")}
      >
        <FieldLabel htmlFor={`${idPrefix}-address-country`}>
          {t("mosques.country")}
        </FieldLabel>
        <Combobox
          id={`${idPrefix}-address-country`}
          options={countryOptions}
          value={watchString(watch, "addressCountry") || null}
          onValueChange={(value) =>
            setValue("addressCountry" as never, (value ?? "") as never, {
              shouldValidate: true,
            })
          }
          placeholder={t("mosques.country")}
        />
        {errorFor("addressCountry") ? (
          <FieldDescription className="text-destructive">
            {errorFor("addressCountry")?.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errorFor("addressStreet")}>
        <FieldLabel htmlFor={`${idPrefix}-address-street`}>
          {t("mosques.street")}
        </FieldLabel>
        <Input
          id={`${idPrefix}-address-street`}
          autoComplete="address-line1"
          aria-invalid={!!errorFor("addressStreet")}
          {...register("addressStreet" as never)}
        />
        {errorFor("addressStreet") ? (
          <FieldDescription className="text-destructive">
            {errorFor("addressStreet")?.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errorFor("addressHouseNumber")}>
        <FieldLabel htmlFor={`${idPrefix}-address-house-number`}>
          {t("mosques.houseNumber")}
        </FieldLabel>
        <Input
          id={`${idPrefix}-address-house-number`}
          autoComplete="address-line2"
          aria-invalid={!!errorFor("addressHouseNumber")}
          {...register("addressHouseNumber" as never)}
        />
        {errorFor("addressHouseNumber") ? (
          <FieldDescription className="text-destructive">
            {errorFor("addressHouseNumber")?.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errorFor("addressPostalCode")}>
        <FieldLabel htmlFor={`${idPrefix}-address-postal-code`}>
          {t("mosques.postalCode")}
        </FieldLabel>
        <Input
          id={`${idPrefix}-address-postal-code`}
          autoComplete="postal-code"
          aria-invalid={!!errorFor("addressPostalCode")}
          {...register("addressPostalCode" as never)}
        />
        {errorFor("addressPostalCode") ? (
          <FieldDescription className="text-destructive">
            {errorFor("addressPostalCode")?.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errorFor("city")}>
        <FieldLabel htmlFor={`${idPrefix}-city`}>{t("mosques.city")}</FieldLabel>
        <Input
          id={`${idPrefix}-city`}
          autoComplete="address-level2"
          aria-invalid={!!errorFor("city")}
          {...register("city" as never)}
        />
        {errorFor("city") ? (
          <FieldDescription className="text-destructive">
            {errorFor("city")?.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errorFor("addressState")}>
        <FieldLabel htmlFor={`${idPrefix}-address-state`}>
          {t("mosques.state")}
        </FieldLabel>
        <Input
          id={`${idPrefix}-address-state`}
          autoComplete="address-level1"
          aria-invalid={!!errorFor("addressState")}
          {...register("addressState" as never)}
        />
        {errorFor("addressState") ? (
          <FieldDescription className="text-destructive">
            {errorFor("addressState")?.message}
          </FieldDescription>
        ) : null}
      </Field>
    </div>
  );

  if (!collapsible) {
    return fields;
  }

  return (
    <details
      className="rounded-md border border-border p-3"
      open={isOpen}
      onToggle={(event) => {
        const next = event.currentTarget.open;
        onOpenChange?.(next);
        if (open === undefined) {
          setInternalOpen(next);
        }
      }}
    >
      <summary className="cursor-pointer text-sm font-medium">
        {summaryLabel ?? t("profile.addressOptional")}
      </summary>
      <div className="mt-4">{fields}</div>
    </details>
  );
}
