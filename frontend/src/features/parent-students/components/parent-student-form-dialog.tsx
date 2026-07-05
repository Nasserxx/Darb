import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { Controller, useForm, type UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  useCreateParentStudent,
  useUpdateParentStudent,
} from "@/features/parent-students/hooks/use-parent-students.ts";
import {
  parentStudentCreateSchema,
  parentStudentUpdateSchema,
  type ParentStudentCreateFormValues,
  type ParentStudentUpdateFormValues,
} from "@/features/parent-students/schemas/parent-student.schema.ts";
import type { ParentStudentResponse } from "@/features/parent-students/types/index.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";

type ParentStudentFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  link?: ParentStudentResponse | null;
};

export function ParentStudentFormDialog({
  open,
  onOpenChange,
  link,
}: ParentStudentFormDialogProps) {
  const { t } = useTranslation("app");
  const isEdit = Boolean(link);
  const createMutation = useCreateParentStudent();
  const updateMutation = useUpdateParentStudent();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const createForm = useForm<ParentStudentCreateFormValues>({
    resolver: zodResolver(parentStudentCreateSchema),
    defaultValues: {
      parentUserId: "",
      studentId: "",
      relationship: "",
      isPrimary: false,
      receivesNotifications: true,
    },
  });

  const updateForm = useForm<ParentStudentUpdateFormValues>({
    resolver: zodResolver(parentStudentUpdateSchema),
    defaultValues: {
      relationship: "",
      isPrimary: false,
      receivesNotifications: true,
    },
  });

  useEffect(() => {
    if (!open) return;
    if (link) {
      updateForm.reset({
        relationship: link.relationship ?? "",
        isPrimary: link.isPrimary,
        receivesNotifications: link.receivesNotifications,
      });
    } else {
      createForm.reset({
        parentUserId: "",
        studentId: "",
        relationship: "",
        isPrimary: false,
        receivesNotifications: true,
      });
    }
  }, [open, link, createForm, updateForm]);

  async function handleSubmit() {
    if (isEdit && link) {
      const valid = await updateForm.trigger();
      if (!valid) return;
      try {
        await updateMutation.mutateAsync({
          id: link.id,
          body: updateForm.getValues(),
        });
        toast.success(t("parentStudents.updateSuccess"));
        onOpenChange(false);
      } catch (error) {
        const result = toMutationError(error, t);
        if (result.fieldErrors) {
          applyFieldErrors(result.fieldErrors, updateForm.setError, t);
        }
        toast.error(result.message);
      }
      return;
    }

    const valid = await createForm.trigger();
    if (!valid) return;
    try {
      await createMutation.mutateAsync(createForm.getValues());
      toast.success(t("parentStudents.createSuccess"));
      onOpenChange(false);
    } catch (error) {
      const result = toMutationError(error, t);
      if (result.fieldErrors) {
        applyFieldErrors(result.fieldErrors, createForm.setError, t);
      }
      toast.error(result.message);
    }
  }

  const form = (isEdit ? updateForm : createForm) as UseFormReturn<ParentStudentCreateFormValues>;

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("parentStudents.edit") : t("parentStudents.create")}
      submitLabel={t("actions.save")}
      onSubmit={() => void handleSubmit()}
      isPending={isPending}
    >
      <FieldGroup>
        {!isEdit ? (
          <>
            <Field data-invalid={!!createForm.formState.errors.parentUserId}>
              <FieldLabel htmlFor="parentUserId">
                {t("parentStudents.parentUserId")}
              </FieldLabel>
              <Input
                id="parentUserId"
                {...createForm.register("parentUserId")}
                aria-invalid={!!createForm.formState.errors.parentUserId}
              />
            </Field>
            <Field data-invalid={!!createForm.formState.errors.studentId}>
              <FieldLabel htmlFor="studentId">
                {t("parentStudents.studentId")}
              </FieldLabel>
              <Input
                id="studentId"
                {...createForm.register("studentId")}
                aria-invalid={!!createForm.formState.errors.studentId}
              />
            </Field>
          </>
        ) : null}
        <Field>
          <FieldLabel htmlFor="relationship">
            {t("parentStudents.relationship")}
          </FieldLabel>
          <Input id="relationship" {...form.register("relationship")} />
        </Field>
        <Field className="flex flex-row items-center gap-3">
          <Controller
            name="isPrimary"
            control={form.control}
            render={({ field }) => (
              <Checkbox
                id="isPrimary"
                checked={field.value ?? false}
                onCheckedChange={(checked) => field.onChange(checked === true)}
              />
            )}
          />
          <FieldLabel htmlFor="isPrimary" className="font-normal">
            {t("parentStudents.isPrimary")}
          </FieldLabel>
        </Field>
        <Field className="flex flex-row items-center gap-3">
          <Controller
            name="receivesNotifications"
            control={form.control}
            render={({ field }) => (
              <Checkbox
                id="receivesNotifications"
                checked={field.value ?? false}
                onCheckedChange={(checked) => field.onChange(checked === true)}
              />
            )}
          />
          <FieldLabel htmlFor="receivesNotifications" className="font-normal">
            {t("parentStudents.receivesNotifications")}
          </FieldLabel>
        </Field>
      </FieldGroup>
    </EntityFormDialog>
  );
}
