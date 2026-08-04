import type { FieldErrors, UseFormRegister } from "react-hook-form";
import { useTranslation } from "react-i18next";

import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";

import type { MosqueCreateFormValues } from "../schemas/mosque-create.schema.ts";

type MosqueCreateFieldsProps = {
  register: UseFormRegister<MosqueCreateFormValues>;
  errors: FieldErrors<MosqueCreateFormValues>;
  idPrefix?: string;
};

export function MosqueCreateFields({
  register,
  errors,
  idPrefix = "mosque",
}: MosqueCreateFieldsProps) {
  const { t } = useTranslation("app");

  return (
    <FieldGroup>
      <Field data-invalid={!!errors.name}>
        <FieldLabel htmlFor={`${idPrefix}-name`}>{t("mosques.name")}</FieldLabel>
        <Input
          id={`${idPrefix}-name`}
          aria-invalid={!!errors.name}
          {...register("name")}
        />
        {errors.name ? (
          <FieldDescription className="text-destructive">
            {errors.name.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errors.city}>
        <FieldLabel htmlFor={`${idPrefix}-city`}>{t("mosques.city")}</FieldLabel>
        <Input
          id={`${idPrefix}-city`}
          aria-invalid={!!errors.city}
          {...register("city")}
        />
        {errors.city ? (
          <FieldDescription className="text-destructive">
            {errors.city.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errors.address}>
        <FieldLabel htmlFor={`${idPrefix}-address`}>{t("mosques.address")}</FieldLabel>
        <Input
          id={`${idPrefix}-address`}
          aria-invalid={!!errors.address}
          {...register("address")}
        />
        {errors.address ? (
          <FieldDescription className="text-destructive">
            {errors.address.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errors.phone}>
        <FieldLabel htmlFor={`${idPrefix}-phone`}>{t("mosques.phone")}</FieldLabel>
        <Input
          id={`${idPrefix}-phone`}
          aria-invalid={!!errors.phone}
          {...register("phone")}
        />
        {errors.phone ? (
          <FieldDescription className="text-destructive">
            {errors.phone.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errors.email}>
        <FieldLabel htmlFor={`${idPrefix}-email`}>{t("mosques.email")}</FieldLabel>
        <Input
          id={`${idPrefix}-email`}
          type="email"
          aria-invalid={!!errors.email}
          {...register("email")}
        />
        {errors.email ? (
          <FieldDescription className="text-destructive">
            {errors.email.message}
          </FieldDescription>
        ) : null}
      </Field>

      <Field data-invalid={!!errors.timezone}>
        <FieldLabel htmlFor={`${idPrefix}-timezone`}>{t("mosques.timezone")}</FieldLabel>
        <Input
          id={`${idPrefix}-timezone`}
          aria-invalid={!!errors.timezone}
          {...register("timezone")}
        />
        {errors.timezone ? (
          <FieldDescription className="text-destructive">
            {errors.timezone.message}
          </FieldDescription>
        ) : null}
      </Field>
    </FieldGroup>
  );
}
