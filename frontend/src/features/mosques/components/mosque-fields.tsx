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
import { Separator } from "@/components/ui/separator.tsx";
import { getCountryOptions } from "@/lib/countries.ts";

import { TimeZoneSelect } from "./timezone-select.tsx";

type MosqueFieldsProps<TFormValues extends FieldValues> = {
  register: UseFormRegister<TFormValues>;
  errors: FieldErrors<TFormValues>;
  watch: UseFormWatch<TFormValues>;
  setValue: UseFormSetValue<TFormValues>;
  idPrefix?: string;
  /** Edit-only: keeps a legacy stored zone selectable even if not in the list. */
  timezoneCustomValue?: string | null;
};

function SectionHeading({ children }: { children: React.ReactNode }) {
  return (
    <h3 className="text-sm font-medium text-muted-foreground">{children}</h3>
  );
}

/** Reads a string-typed field off a generic form state. */
function watchString<TFormValues extends FieldValues>(
  watch: UseFormWatch<TFormValues>,
  key: string,
): string | undefined {
  return watch(key as never) as unknown as string | undefined;
}

/** Reads a field error off a generic form state. */
function fieldError<TFormValues extends FieldValues>(
  errors: FieldErrors<TFormValues>,
  key: string,
): { message?: string } | undefined {
  return (errors as Record<string, { message?: string } | undefined>)[key];
}

export function MosqueFields<TFormValues extends FieldValues>({
  register,
  errors,
  watch,
  setValue,
  idPrefix = "mosque",
  timezoneCustomValue,
}: MosqueFieldsProps<TFormValues>) {
  const { t, i18n } = useTranslation("app");

  const countryOptions = getCountryOptions(i18n.language);

  const errorFor = (key: string) => fieldError(errors, key);

  return (
    <div className="flex flex-col gap-6">
      {/* Section: General */}
      <section className="flex flex-col gap-4">
        <SectionHeading>{t("mosques.sectionGeneral")}</SectionHeading>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field className="sm:col-span-2" data-invalid={!!errorFor("name")}>
            <FieldLabel htmlFor={`${idPrefix}-name`}>
              {t("mosques.name")} <span className="text-destructive">*</span>
            </FieldLabel>
            <Input
              id={`${idPrefix}-name`}
              autoComplete="organization"
              aria-invalid={!!errorFor("name")}
              {...register("name" as never)}
            />
            {errorFor("name") ? (
              <FieldDescription className="text-destructive">
                {errorFor("name")?.message}
              </FieldDescription>
            ) : null}
          </Field>
        </div>
      </section>

      <Separator />

      {/* Section: Location */}
      <section className="flex flex-col gap-4">
        <SectionHeading>{t("mosques.sectionLocation")}</SectionHeading>
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
              value={watchString(watch, "addressCountry") ?? null}
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
            <FieldLabel htmlFor={`${idPrefix}-city`}>
              {t("mosques.city")} <span className="text-destructive">*</span>
            </FieldLabel>
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
              {t("mosques.state")} <span className="text-destructive">*</span>
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
      </section>

      <Separator />

      {/* Section: Contact & Timezone */}
      <section className="flex flex-col gap-4">
        <SectionHeading>{t("mosques.sectionContactTimezone")}</SectionHeading>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field data-invalid={!!errorFor("phone")}>
            <FieldLabel htmlFor={`${idPrefix}-phone`}>
              {t("mosques.phone")}
            </FieldLabel>
            <Input
              id={`${idPrefix}-phone`}
              type="tel"
              autoComplete="tel"
              aria-invalid={!!errorFor("phone")}
              {...register("phone" as never)}
            />
            {errorFor("phone") ? (
              <FieldDescription className="text-destructive">
                {errorFor("phone")?.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!errorFor("email")}>
            <FieldLabel htmlFor={`${idPrefix}-email`}>
              {t("mosques.email")}
            </FieldLabel>
            <Input
              id={`${idPrefix}-email`}
              type="email"
              autoComplete="email"
              aria-invalid={!!errorFor("email")}
              {...register("email" as never)}
            />
            {errorFor("email") ? (
              <FieldDescription className="text-destructive">
                {errorFor("email")?.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field className="sm:col-span-2" data-invalid={!!errorFor("timezone")}>
            <FieldLabel htmlFor={`${idPrefix}-timezone`}>
              {t("mosques.timezone")}
            </FieldLabel>
            <TimeZoneSelect
              id={`${idPrefix}-timezone`}
              value={watchString(watch, "timezone") ?? null}
              onValueChange={(value) =>
                setValue("timezone" as never, (value ?? "") as never, {
                  shouldValidate: true,
                })
              }
              placeholder={t("mosques.timezonePlaceholder")}
              allowCustomValue={timezoneCustomValue ?? undefined}
            />
            {errorFor("timezone") ? (
              <FieldDescription className="text-destructive">
                {errorFor("timezone")?.message}
              </FieldDescription>
            ) : null}
          </Field>
        </div>
      </section>
    </div>
  );
}
