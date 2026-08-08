import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { UserSearchSelect } from "@/components/shared/user-search-select.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Switch } from "@/components/ui/switch.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";

import {
  teacherCreateSchema,
  toTeacherCreateRequestBody,
  type TeacherCreateFormValues,
} from "../schemas/teacher-create.schema.ts";
import {
  teacherUpdateSchema,
  toTeacherUpdateRequestBody,
  type TeacherUpdateFormValues,
} from "../schemas/teacher-update.schema.ts";
import { useCreateTeacher, useUpdateTeacher } from "../hooks/use-teachers.ts";
import type { TeacherResponse } from "../types/index.ts";

type TeacherFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  teacher?: TeacherResponse | null;
};

export function TeacherFormDialog({ open, onOpenChange, teacher }: TeacherFormDialogProps) {
  const { t } = useTranslation("app");
  const isEdit = Boolean(teacher);
  const createTeacher = useCreateTeacher();
  const updateTeacher = useUpdateTeacher();
  const isPending = createTeacher.isPending || updateTeacher.isPending;
  const { data: mosquesPage } = useMosques({ page: 0, size: 100 });

  const createForm = useForm<TeacherCreateFormValues>({
    resolver: zodResolver(teacherCreateSchema),
    defaultValues: {
      userId: "",
      mosqueId: "",
      specialization: "",
      bio: "",
      yearsExperience: undefined,
      ijazahChain: "",
    },
  });

  const editForm = useForm<TeacherUpdateFormValues>({
    resolver: zodResolver(teacherUpdateSchema),
    defaultValues: {
      specialization: "",
      bio: "",
      yearsExperience: undefined,
      ijazahChain: "",
      isAvailable: true,
    },
  });

  useEffect(() => {
    if (!open) {
      createForm.reset();
      editForm.reset();
      return;
    }
    if (teacher) {
      editForm.reset({
        specialization: teacher.specialization ?? "",
        bio: teacher.bio ?? "",
        yearsExperience: teacher.yearsExperience ?? undefined,
        ijazahChain: teacher.ijazahChain ?? "",
        isAvailable: teacher.isAvailable,
      });
    } else {
      createForm.reset();
    }
  }, [open, teacher, createForm, editForm]);

  async function handleCreateSubmit(values: TeacherCreateFormValues) {
    try {
      await createTeacher.mutateAsync(toTeacherCreateRequestBody(values));
      toast.success(t("teachers.createSuccess"));
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, createForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleEditSubmit(values: TeacherUpdateFormValues) {
    if (!teacher) return;
    try {
      await updateTeacher.mutateAsync({
        id: teacher.id,
        body: toTeacherUpdateRequestBody(values),
      });
      toast.success(t("teachers.updateSuccess"));
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
      title={isEdit ? t("teachers.edit") : t("teachers.create")}
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
          <Field data-invalid={!!editErrors.specialization}>
            <FieldLabel htmlFor="teacher-specialization">
              {t("teachers.specialization")}
            </FieldLabel>
            <Input
              id="teacher-specialization"
              aria-invalid={!!editErrors.specialization}
              {...editForm.register("specialization")}
            />
            {editErrors.specialization ? (
              <FieldDescription className="text-destructive">
                {editErrors.specialization.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.yearsExperience}>
            <FieldLabel htmlFor="teacher-years">{t("teachers.yearsExperience")}</FieldLabel>
            <Input
              id="teacher-years"
              type="number"
              min={0}
              aria-invalid={!!editErrors.yearsExperience}
              {...editForm.register("yearsExperience", {
                setValueAs: (value) => (value === "" ? undefined : Number(value)),
              })}
            />
            {editErrors.yearsExperience ? (
              <FieldDescription className="text-destructive">
                {editErrors.yearsExperience.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.ijazahChain}>
            <FieldLabel htmlFor="teacher-ijazah">{t("teachers.ijazahChain")}</FieldLabel>
            <Input
              id="teacher-ijazah"
              aria-invalid={!!editErrors.ijazahChain}
              {...editForm.register("ijazahChain")}
            />
            {editErrors.ijazahChain ? (
              <FieldDescription className="text-destructive">
                {editErrors.ijazahChain.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.bio}>
            <FieldLabel htmlFor="teacher-bio">{t("teachers.bio")}</FieldLabel>
            <Textarea
              id="teacher-bio"
              aria-invalid={!!editErrors.bio}
              {...editForm.register("bio")}
            />
            {editErrors.bio ? (
              <FieldDescription className="text-destructive">
                {editErrors.bio.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field className="flex-row items-center justify-between gap-4">
            <FieldLabel htmlFor="teacher-available">{t("teachers.isAvailable")}</FieldLabel>
            <Controller
              name="isAvailable"
              control={editForm.control}
              render={({ field }) => (
                <Switch
                  id="teacher-available"
                  checked={field.value ?? false}
                  onCheckedChange={field.onChange}
                />
              )}
            />
          </Field>
        </FieldGroup>
      ) : (
        <FieldGroup>
          <Field data-invalid={!!createErrors.userId}>
            <FieldLabel>{t("teachers.userId")}</FieldLabel>
            <Controller
              name="userId"
              control={createForm.control}
              render={({ field }) => (
                <UserSearchSelect
                  value={field.value}
                  onValueChange={field.onChange}
                />
              )}
            />
            {createErrors.userId ? (
              <FieldDescription className="text-destructive">
                {createErrors.userId.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.mosqueId}>
            <FieldLabel>{t("teachers.mosqueId")}</FieldLabel>
            <Controller
              name="mosqueId"
              control={createForm.control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger aria-invalid={!!createErrors.mosqueId}>
                    <SelectValue placeholder={t("teachers.selectMosque")} />
                  </SelectTrigger>
                  <SelectContent>
                    {(mosquesPage?.content ?? []).map((mosque) => (
                      <SelectItem key={mosque.id} value={mosque.id}>
                        {mosque.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {createErrors.mosqueId ? (
              <FieldDescription className="text-destructive">
                {createErrors.mosqueId.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.specialization}>
            <FieldLabel htmlFor="teacher-specialization">
              {t("teachers.specialization")}
            </FieldLabel>
            <Input
              id="teacher-specialization"
              aria-invalid={!!createErrors.specialization}
              {...createForm.register("specialization")}
            />
            {createErrors.specialization ? (
              <FieldDescription className="text-destructive">
                {createErrors.specialization.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.yearsExperience}>
            <FieldLabel htmlFor="teacher-years">{t("teachers.yearsExperience")}</FieldLabel>
            <Input
              id="teacher-years"
              type="number"
              min={0}
              aria-invalid={!!createErrors.yearsExperience}
              {...createForm.register("yearsExperience", {
                setValueAs: (value) => (value === "" ? undefined : Number(value)),
              })}
            />
            {createErrors.yearsExperience ? (
              <FieldDescription className="text-destructive">
                {createErrors.yearsExperience.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.ijazahChain}>
            <FieldLabel htmlFor="teacher-ijazah">{t("teachers.ijazahChain")}</FieldLabel>
            <Input
              id="teacher-ijazah"
              aria-invalid={!!createErrors.ijazahChain}
              {...createForm.register("ijazahChain")}
            />
            {createErrors.ijazahChain ? (
              <FieldDescription className="text-destructive">
                {createErrors.ijazahChain.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.bio}>
            <FieldLabel htmlFor="teacher-bio">{t("teachers.bio")}</FieldLabel>
            <Textarea
              id="teacher-bio"
              aria-invalid={!!createErrors.bio}
              {...createForm.register("bio")}
            />
            {createErrors.bio ? (
              <FieldDescription className="text-destructive">
                {createErrors.bio.message}
              </FieldDescription>
            ) : null}
          </Field>
        </FieldGroup>
      )}
    </EntityFormDialog>
  );
}
