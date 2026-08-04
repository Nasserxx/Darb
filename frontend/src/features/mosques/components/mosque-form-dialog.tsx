import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";

import { MosqueCreateFields } from "./mosque-create-fields.tsx";

import {
  mosqueCreateSchema,
  toMosqueCreateRequestBody,
  type MosqueCreateFormValues,
} from "../schemas/mosque-create.schema.ts";
import {
  mosqueUpdateSchema,
  toMosqueUpdateRequestBody,
  type MosqueUpdateFormValues,
} from "../schemas/mosque-update.schema.ts";
import { useCreateMosque, useUpdateMosque } from "../hooks/use-mosques.ts";
import type { MosqueResponse } from "../types/index.ts";

type MosqueFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mosque?: MosqueResponse | null;
};

export function MosqueFormDialog({ open, onOpenChange, mosque }: MosqueFormDialogProps) {
  const { t } = useTranslation("app");
  const isEdit = Boolean(mosque);
  const createMosque = useCreateMosque();
  const updateMosque = useUpdateMosque();
  const isPending = createMosque.isPending || updateMosque.isPending;

  const createForm = useForm<MosqueCreateFormValues>({
    resolver: zodResolver(mosqueCreateSchema),
    defaultValues: {
      name: "",
      address: "",
      city: "",
      phone: "",
      email: "",
      logoUrl: "",
      timezone: "",
    },
  });

  const editForm = useForm<MosqueUpdateFormValues>({
    resolver: zodResolver(mosqueUpdateSchema),
    defaultValues: {
      name: "",
      address: "",
      city: "",
      phone: "",
      email: "",
      logoUrl: "",
      timezone: "",
      settings: "",
    },
  });

  useEffect(() => {
    if (!open) {
      createForm.reset();
      editForm.reset();
      return;
    }
    if (mosque) {
      editForm.reset({
        name: mosque.name,
        address: mosque.address ?? "",
        city: mosque.city ?? "",
        phone: mosque.phone ?? "",
        email: mosque.email ?? "",
        logoUrl: mosque.logoUrl ?? "",
        timezone: mosque.timezone ?? "",
        settings: mosque.settings ?? "",
      });
    } else {
      createForm.reset();
    }
  }, [open, mosque, createForm, editForm]);

  async function handleCreateSubmit(values: MosqueCreateFormValues) {
    try {
      await createMosque.mutateAsync(toMosqueCreateRequestBody(values));
      toast.success(t("mosques.createSuccess"));
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, createForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleEditSubmit(values: MosqueUpdateFormValues) {
    if (!mosque) return;
    try {
      await updateMosque.mutateAsync({
        id: mosque.id,
        body: toMosqueUpdateRequestBody(values),
      });
      toast.success(t("mosques.updateSuccess"));
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, editForm.setError, t);
      }
      toast.error(message);
    }
  }

  const createErrors = createForm.formState.errors;
  const editErrors = editForm.formState.errors;

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("mosques.edit") : t("mosques.create")}
      submitLabel={isEdit ? t("actions.save") : t("actions.create")}
      isPending={isPending}
      onSubmit={() => {
        if (isEdit) {
          void editForm.handleSubmit(handleEditSubmit)();
        } else {
          void createForm.handleSubmit(handleCreateSubmit)();
        }
      }}
    >
      {isEdit ? (
        <FieldGroup>
          <Field data-invalid={!!editErrors.name}>
            <FieldLabel htmlFor="mosque-name">{t("mosques.name")}</FieldLabel>
            <Input
              id="mosque-name"
              aria-invalid={!!editErrors.name}
              {...editForm.register("name")}
            />
            {editErrors.name ? (
              <FieldDescription className="text-destructive">
                {editErrors.name.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.city}>
            <FieldLabel htmlFor="mosque-city">{t("mosques.city")}</FieldLabel>
            <Input
              id="mosque-city"
              aria-invalid={!!editErrors.city}
              {...editForm.register("city")}
            />
            {editErrors.city ? (
              <FieldDescription className="text-destructive">
                {editErrors.city.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.address}>
            <FieldLabel htmlFor="mosque-address">{t("mosques.address")}</FieldLabel>
            <Input
              id="mosque-address"
              aria-invalid={!!editErrors.address}
              {...editForm.register("address")}
            />
            {editErrors.address ? (
              <FieldDescription className="text-destructive">
                {editErrors.address.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.phone}>
            <FieldLabel htmlFor="mosque-phone">{t("mosques.phone")}</FieldLabel>
            <Input
              id="mosque-phone"
              aria-invalid={!!editErrors.phone}
              {...editForm.register("phone")}
            />
            {editErrors.phone ? (
              <FieldDescription className="text-destructive">
                {editErrors.phone.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.email}>
            <FieldLabel htmlFor="mosque-email">{t("mosques.email")}</FieldLabel>
            <Input
              id="mosque-email"
              type="email"
              aria-invalid={!!editErrors.email}
              {...editForm.register("email")}
            />
            {editErrors.email ? (
              <FieldDescription className="text-destructive">
                {editErrors.email.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.timezone}>
            <FieldLabel htmlFor="mosque-timezone">{t("mosques.timezone")}</FieldLabel>
            <Input
              id="mosque-timezone"
              aria-invalid={!!editErrors.timezone}
              {...editForm.register("timezone")}
            />
            {editErrors.timezone ? (
              <FieldDescription className="text-destructive">
                {editErrors.timezone.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.settings}>
            <FieldLabel htmlFor="mosque-settings">{t("mosques.settings")}</FieldLabel>
            <Input id="mosque-settings" {...editForm.register("settings")} />
            {editErrors.settings ? (
              <FieldDescription className="text-destructive">
                {editErrors.settings.message}
              </FieldDescription>
            ) : null}
          </Field>
        </FieldGroup>
      ) : (
        <MosqueCreateFields
          register={createForm.register}
          errors={createErrors}
        />
      )}
    </EntityFormDialog>
  );
}
