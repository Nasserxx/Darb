import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { Controller, useForm, type UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useCreateCircle,
  useUpdateCircle,
} from "@/features/circles/hooks/use-circles.ts";
import {
  circleCreateSchema,
  circleUpdateSchema,
  type CircleCreateFormValues,
  type CircleUpdateFormValues,
} from "@/features/circles/schemas/circle.schema.ts";
import type { CircleResponse } from "@/features/circles/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import type { CircleLevel, CircleStatus, CircleType } from "@/lib/types/api.ts";

const LEVELS: CircleLevel[] = [
  "BEGINNER",
  "INTERMEDIATE",
  "ADVANCED",
  "MEMORIZATION",
  "IJAZAH",
];
const TYPES: CircleType[] = ["IN_PERSON", "ONLINE", "HYBRID"];
const STATUSES: CircleStatus[] = ["PLANNING", "ACTIVE", "PAUSED", "ENDED"];

type CircleFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  circle?: CircleResponse | null;
};

export function CircleFormDialog({ open, onOpenChange, circle }: CircleFormDialogProps) {
  const { t } = useTranslation("app");
  const { mosqueId } = useWorkspace();
  const isEdit = Boolean(circle);
  const createMutation = useCreateCircle();
  const updateMutation = useUpdateCircle();
  const isPending = createMutation.isPending || updateMutation.isPending;

  const createForm = useForm<CircleCreateFormValues>({
    resolver: zodResolver(circleCreateSchema),
    defaultValues: {
      mosqueId: mosqueId ?? "",
      teacherId: "",
      name: "",
      level: "BEGINNER",
      type: "IN_PERSON",
      status: "PLANNING",
      capacity: undefined,
      startTime: "",
      endTime: "",
      daysOfWeek: "",
      roomOrLink: "",
    },
  });

  const updateForm = useForm<CircleUpdateFormValues>({
    resolver: zodResolver(circleUpdateSchema),
    defaultValues: {
      name: "",
      level: "BEGINNER",
      type: "IN_PERSON",
      status: "PLANNING",
    },
  });

  useEffect(() => {
    if (!open) return;
    if (circle) {
      updateForm.reset({
        name: circle.name,
        level: circle.level,
        type: circle.type,
        status: circle.status,
        capacity: circle.capacity ?? undefined,
        startTime: circle.startTime ?? "",
        endTime: circle.endTime ?? "",
        daysOfWeek: circle.daysOfWeek ?? "",
        roomOrLink: circle.roomOrLink ?? "",
        lateThresholdMinutes: circle.lateThresholdMinutes ?? undefined,
        monthlyFee: circle.monthlyFee ?? undefined,
      });
    } else {
      createForm.reset({
        mosqueId: mosqueId ?? "",
        teacherId: "",
        name: "",
        level: "BEGINNER",
        type: "IN_PERSON",
        status: "PLANNING",
        capacity: undefined,
        startTime: "",
        endTime: "",
        daysOfWeek: "",
        roomOrLink: "",
      });
    }
  }, [open, circle, mosqueId, createForm, updateForm]);

  async function handleSubmit() {
    if (isEdit && circle) {
      const valid = await updateForm.trigger();
      if (!valid) return;
      const values = updateForm.getValues();
      try {
        await updateMutation.mutateAsync({ id: circle.id, body: values });
        toast.success(t("circles.updateSuccess"));
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
      toast.success(t("circles.createSuccess"));
      onOpenChange(false);
    } catch (error) {
      const result = toMutationError(error, t);
      if (result.fieldErrors) {
        applyFieldErrors(result.fieldErrors, createForm.setError, t);
      }
      toast.error(result.message);
    }
  }

  const form = (isEdit ? updateForm : createForm) as UseFormReturn<CircleCreateFormValues>;

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("circles.edit") : t("circles.create")}
      submitLabel={t("actions.save")}
      onSubmit={() => void handleSubmit()}
      isPending={isPending}
    >
      <FieldGroup>
        {!isEdit ? (
          <>
            <Field>
              <FieldLabel htmlFor="mosqueId">{t("circles.mosqueId")}</FieldLabel>
              <Input id="mosqueId" {...createForm.register("mosqueId")} />
            </Field>
            <Field>
              <FieldLabel htmlFor="teacherId">{t("circles.teacherId")}</FieldLabel>
              <Input id="teacherId" {...createForm.register("teacherId")} />
            </Field>
          </>
        ) : null}
        <Field>
          <FieldLabel htmlFor="name">{t("circles.name")}</FieldLabel>
          <Input id="name" {...form.register("name")} />
        </Field>
        <Field>
          <FieldLabel>{t("circles.level")}</FieldLabel>
          <Controller
            name="level"
            control={form.control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {LEVELS.map((level) => (
                      <SelectItem key={level} value={level}>
                        {t(`enums.circleLevel.${level}`)}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel>{t("circles.type")}</FieldLabel>
          <Controller
            name="type"
            control={form.control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {t(`enums.circleType.${type}`)}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel>{t("circles.status")}</FieldLabel>
          <Controller
            name="status"
            control={form.control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {STATUSES.map((status) => (
                      <SelectItem key={status} value={status}>
                        {t(`enums.circleStatus.${status}`)}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="capacity">{t("circles.capacity")}</FieldLabel>
          <Input
            id="capacity"
            type="number"
            min={1}
            {...form.register("capacity", { valueAsNumber: true })}
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="daysOfWeek">{t("circles.daysOfWeek")}</FieldLabel>
          <Input
            id="daysOfWeek"
            placeholder="Mon,Wed,Fri"
            {...form.register("daysOfWeek")}
          />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field>
            <FieldLabel htmlFor="startTime">{t("circles.startTime")}</FieldLabel>
            <Input id="startTime" type="time" {...form.register("startTime")} />
          </Field>
          <Field>
            <FieldLabel htmlFor="endTime">{t("circles.endTime")}</FieldLabel>
            <Input id="endTime" type="time" {...form.register("endTime")} />
          </Field>
        </div>
        <Field>
          <FieldLabel htmlFor="roomOrLink">{t("circles.roomOrLink")}</FieldLabel>
          <Input id="roomOrLink" {...form.register("roomOrLink")} />
        </Field>
      </FieldGroup>
    </EntityFormDialog>
  );
}
