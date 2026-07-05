import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm, type UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  useCreateStudent,
  useUpdateStudent,
} from "@/features/students/hooks/use-students.ts";
import {
  studentCreateSchema,
  studentUpdateSchema,
  type StudentCreateFormValues,
  type StudentUpdateFormValues,
} from "@/features/students/schemas/student.schema.ts";
import type { StudentResponse } from "@/features/students/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";

type StudentFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  student?: StudentResponse | null;
};

export function StudentFormDialog({
  open,
  onOpenChange,
  student,
}: StudentFormDialogProps) {
  const { t } = useTranslation("app");
  const { mosqueId } = useWorkspace();
  const isEdit = Boolean(student);
  const createMutation = useCreateStudent();
  const updateMutation = useUpdateStudent();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const createForm = useForm<StudentCreateFormValues>({
    resolver: zodResolver(studentCreateSchema),
    defaultValues: {
      userId: "",
      mosqueId: mosqueId ?? "",
      nationalId: "",
      medicalNotes: "",
      memorizedJuz: undefined,
    },
  });

  const updateForm = useForm<StudentUpdateFormValues>({
    resolver: zodResolver(studentUpdateSchema),
    defaultValues: {
      nationalId: "",
      medicalNotes: "",
      memorizedJuz: undefined,
      totalAbsences: undefined,
      totalLateArrivals: undefined,
    },
  });

  useEffect(() => {
    if (!open) return;
    if (student) {
      updateForm.reset({
        nationalId: student.nationalId ?? "",
        medicalNotes: student.medicalNotes ?? "",
        memorizedJuz: student.memorizedJuz ?? undefined,
        totalAbsences: student.totalAbsences,
        totalLateArrivals: student.totalLateArrivals,
      });
    } else {
      createForm.reset({
        userId: "",
        mosqueId: mosqueId ?? "",
        nationalId: "",
        medicalNotes: "",
        memorizedJuz: undefined,
      });
    }
  }, [open, student, mosqueId, createForm, updateForm]);

  async function handleSubmit() {
    if (isEdit && student) {
      const valid = await updateForm.trigger();
      if (!valid) return;
      const values = updateForm.getValues();
      try {
        await updateMutation.mutateAsync({ id: student.id, body: values });
        toast.success(t("students.updateSuccess"));
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
    const values = createForm.getValues();
    try {
      await createMutation.mutateAsync(values);
      toast.success(t("students.createSuccess"));
      onOpenChange(false);
    } catch (error) {
      const result = toMutationError(error, t);
      if (result.fieldErrors) {
        applyFieldErrors(result.fieldErrors, createForm.setError, t);
      }
      toast.error(result.message);
    }
  }

  const form = (isEdit ? updateForm : createForm) as UseFormReturn<StudentCreateFormValues>;
  const errors = form.formState.errors;

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("students.edit") : t("students.create")}
      submitLabel={t("actions.save")}
      onSubmit={() => void handleSubmit()}
      isPending={isPending}
    >
      <FieldGroup>
        {!isEdit ? (
          <>
            <Field data-invalid={!!createForm.formState.errors.userId}>
              <FieldLabel htmlFor="userId">{t("students.userId")}</FieldLabel>
              <Input
                id="userId"
                {...createForm.register("userId")}
                aria-invalid={!!createForm.formState.errors.userId}
              />
              {createForm.formState.errors.userId ? (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.userId.message}
                </p>
              ) : null}
            </Field>
            <Field data-invalid={!!createForm.formState.errors.mosqueId}>
              <FieldLabel htmlFor="mosqueId">{t("students.mosqueId")}</FieldLabel>
              <Input
                id="mosqueId"
                {...createForm.register("mosqueId")}
                aria-invalid={!!createForm.formState.errors.mosqueId}
              />
              {createForm.formState.errors.mosqueId ? (
                <p className="text-sm text-destructive">
                  {createForm.formState.errors.mosqueId.message}
                </p>
              ) : null}
            </Field>
          </>
        ) : null}
        <Field data-invalid={!!errors.nationalId}>
          <FieldLabel htmlFor="nationalId">{t("students.nationalId")}</FieldLabel>
          <Input
            id="nationalId"
            {...form.register("nationalId")}
            aria-invalid={!!errors.nationalId}
          />
          {errors.nationalId ? (
            <p className="text-sm text-destructive">{errors.nationalId.message}</p>
          ) : null}
        </Field>
        <Field data-invalid={!!errors.memorizedJuz}>
          <FieldLabel htmlFor="memorizedJuz">{t("students.memorizedJuz")}</FieldLabel>
          <Input
            id="memorizedJuz"
            type="number"
            min={0}
            {...form.register("memorizedJuz", { valueAsNumber: true })}
            aria-invalid={!!errors.memorizedJuz}
          />
          {errors.memorizedJuz ? (
            <p className="text-sm text-destructive">{errors.memorizedJuz.message}</p>
          ) : null}
        </Field>
        <Field data-invalid={!!errors.medicalNotes}>
          <FieldLabel htmlFor="medicalNotes">{t("students.medicalNotes")}</FieldLabel>
          <Textarea
            id="medicalNotes"
            {...form.register("medicalNotes")}
            aria-invalid={!!errors.medicalNotes}
          />
          {errors.medicalNotes ? (
            <p className="text-sm text-destructive">{errors.medicalNotes.message}</p>
          ) : null}
        </Field>
        {isEdit ? (
          <>
            <Field data-invalid={!!updateForm.formState.errors.totalAbsences}>
              <FieldLabel htmlFor="totalAbsences">
                {t("students.totalAbsences")}
              </FieldLabel>
              <Input
                id="totalAbsences"
                type="number"
                min={0}
                {...updateForm.register("totalAbsences", { valueAsNumber: true })}
                aria-invalid={!!updateForm.formState.errors.totalAbsences}
              />
            </Field>
            <Field data-invalid={!!updateForm.formState.errors.totalLateArrivals}>
              <FieldLabel htmlFor="totalLateArrivals">
                {t("students.totalLateArrivals")}
              </FieldLabel>
              <Input
                id="totalLateArrivals"
                type="number"
                min={0}
                {...updateForm.register("totalLateArrivals", { valueAsNumber: true })}
                aria-invalid={!!updateForm.formState.errors.totalLateArrivals}
              />
            </Field>
          </>
        ) : null}
      </FieldGroup>
    </EntityFormDialog>
  );
}
