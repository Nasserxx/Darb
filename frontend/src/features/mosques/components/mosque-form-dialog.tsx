import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { DEFAULT_TIME_ZONE } from "@/lib/timezones.ts";

import { MosqueFields } from "./mosque-fields.tsx";

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
      city: "",
      phone: "",
      email: "",
      logoUrl: "",
      timezone: DEFAULT_TIME_ZONE,
      addressCountry: "",
      addressPostalCode: "",
      addressStreet: "",
      addressHouseNumber: "",
      addressState: "",
    },
  });

  const editForm = useForm<MosqueUpdateFormValues>({
    resolver: zodResolver(mosqueUpdateSchema),
    defaultValues: {
      name: "",
      city: "",
      phone: "",
      email: "",
      logoUrl: "",
      timezone: "",
      addressCountry: "",
      addressPostalCode: "",
      addressStreet: "",
      addressHouseNumber: "",
      addressState: "",
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
        city: mosque.city ?? "",
        phone: mosque.phone ?? "",
        email: mosque.email ?? "",
        logoUrl: mosque.logoUrl ?? "",
        timezone: mosque.timezone ?? "",
        addressCountry: mosque.addressCountry ?? "",
        addressPostalCode: mosque.addressPostalCode ?? "",
        addressStreet: mosque.addressStreet ?? "",
        addressHouseNumber: mosque.addressHouseNumber ?? "",
        addressState: mosque.addressState ?? "",
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
        <MosqueFields
          register={editForm.register}
          errors={editErrors}
          watch={editForm.watch}
          setValue={editForm.setValue}
          idPrefix="mosque"
          timezoneCustomValue={mosque?.timezone ?? null}
        />
      ) : (
        <MosqueFields
          register={createForm.register}
          errors={createErrors}
          watch={createForm.watch}
          setValue={createForm.setValue}
          idPrefix="mosque"
        />
      )}
    </EntityFormDialog>
  );
}
