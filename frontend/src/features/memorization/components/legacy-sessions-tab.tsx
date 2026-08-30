import { useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  useCreateMemorization,
  useMemorizationByStudent,
} from "@/features/memorization/hooks/use-memorization.ts";
import {
  memorizationCreateSchema,
  toMemorizationCreateRequest,
  type MemorizationCreateFormValues,
} from "@/features/memorization/schemas/memorization-create.schema.ts";
import type { MemorizationProgressResponse } from "@/features/memorization/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { canManageMemorization } from "@/lib/navigation/role-permissions.ts";

const GRADES = [
  "EXCELLENT",
  "VERY_GOOD",
  "GOOD",
  "ACCEPTABLE",
  "POOR",
] as const;

function todayLocalDate(): string {
  return new Date().toISOString().slice(0, 10);
}

interface EnrolledCircle {
  circleId: string;
  circleName?: string;
}

interface LegacySessionsTabProps {
  studentId: string;
  enrolledCircles: EnrolledCircle[];
}

export function LegacySessionsTab({
  studentId,
  enrolledCircles,
}: LegacySessionsTabProps) {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { profile } = useWorkspace();
  const { params, setPage } = usePagination();
  const [dialogOpen, setDialogOpen] = useState(false);
  const canManage = canManageMemorization(user?.role);

  const { data, isLoading } = useMemorizationByStudent(studentId, params);
  const createMemorization = useCreateMemorization();

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<MemorizationCreateFormValues>({
    resolver: zodResolver(memorizationCreateSchema),
    defaultValues: {
      studentId,
      circleId: enrolledCircles[0]?.circleId ?? "",
      teacherId: profile?.teacherId ?? "",
      surahNumber: 1,
      ayahFrom: 1,
      ayahTo: 1,
      sessionDate: todayLocalDate(),
      grade: undefined,
      tajweedScore: undefined,
      teacherNotes: "",
      audioUrl: "",
    },
  });

  const columns = useMemo(
    () => [
      {
        id: "sessionDate",
        header: t("memorization.sessionDate"),
        cell: (row: MemorizationProgressResponse) => row.sessionDate,
      },
      {
        id: "surah",
        header: t("memorization.surah"),
        cell: (row: MemorizationProgressResponse) => row.surahNumber,
      },
      {
        id: "ayah",
        header: t("memorization.ayahFrom"),
        cell: (row: MemorizationProgressResponse) =>
          `${row.ayahFrom}–${row.ayahTo}`,
      },
      {
        id: "grade",
        header: t("memorization.grade"),
        cell: (row: MemorizationProgressResponse) =>
          row.grade ? (
            <Badge variant="secondary">
              {t(`memorization.grades.${row.grade}`)}
            </Badge>
          ) : (
            "—"
          ),
      },
      {
        id: "tajweed",
        header: t("memorization.tajweedScore"),
        cell: (row: MemorizationProgressResponse) => row.tajweedScore ?? "—",
      },
    ],
    [t],
  );

  async function onSubmit(values: MemorizationCreateFormValues) {
    try {
      await createMemorization.mutateAsync(
        toMemorizationCreateRequest({
          ...values,
          studentId,
          teacherId: profile?.teacherId ?? values.teacherId,
        }),
      );
      toast.success(t("memorization.saved"));
      setDialogOpen(false);
      reset();
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, setError, t);
      }
      toast.error(message || t("memorization.saveError"));
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <p className="text-sm text-muted-foreground">
          {t("memorization.legacyTabDescription")}
        </p>
        {canManage ? (
          <Button onClick={() => setDialogOpen(true)}>
            {t("memorization.create")}
          </Button>
        ) : null}
      </div>

      <DataTable
        columns={columns}
        data={data}
        isLoading={isLoading}
        onPageChange={setPage}
      />

      <EntityFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        title={t("memorization.create")}
        submitLabel={t("actions.save")}
        isPending={createMemorization.isPending}
        onSubmit={() => void handleSubmit(onSubmit)()}
      >
        <form className="flex flex-col gap-4" onSubmit={(event) => event.preventDefault()}>
          <FieldGroup>
            <Field data-invalid={!!errors.circleId}>
              <FieldLabel>{t("memorization.circle")}</FieldLabel>
              <Controller
                control={control}
                name="circleId"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
                    </SelectTrigger>
                    <SelectContent>
                      {enrolledCircles.map((circle) => (
                        <SelectItem key={circle.circleId} value={circle.circleId}>
                          {circle.circleName ?? formatShortId(circle.circleId)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field data-invalid={!!errors.sessionDate}>
              <FieldLabel htmlFor="legacy-session-date">
                {t("memorization.sessionDate")}
              </FieldLabel>
              <Input id="legacy-session-date" type="date" {...register("sessionDate")} />
            </Field>
            <div className="grid gap-4 sm:grid-cols-3">
              <Field data-invalid={!!errors.surahNumber}>
                <FieldLabel htmlFor="legacy-surah">{t("memorization.surah")}</FieldLabel>
                <Input
                  id="legacy-surah"
                  type="number"
                  min={1}
                  max={114}
                  {...register("surahNumber", { valueAsNumber: true })}
                />
              </Field>
              <Field data-invalid={!!errors.ayahFrom}>
                <FieldLabel htmlFor="legacy-ayah-from">
                  {t("memorization.ayahFrom")}
                </FieldLabel>
                <Input
                  id="legacy-ayah-from"
                  type="number"
                  min={1}
                  {...register("ayahFrom", { valueAsNumber: true })}
                />
              </Field>
              <Field data-invalid={!!errors.ayahTo}>
                <FieldLabel htmlFor="legacy-ayah-to">{t("memorization.ayahTo")}</FieldLabel>
                <Input
                  id="legacy-ayah-to"
                  type="number"
                  min={1}
                  {...register("ayahTo", { valueAsNumber: true })}
                />
              </Field>
            </div>
            <Field>
              <FieldLabel>{t("memorization.grade")}</FieldLabel>
              <Controller
                control={control}
                name="grade"
                render={({ field }) => (
                  <Select
                    value={field.value ?? ""}
                    onValueChange={(value) => field.onChange(value || undefined)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
                    </SelectTrigger>
                    <SelectContent>
                      {GRADES.map((grade) => (
                        <SelectItem key={grade} value={grade}>
                          {t(`memorization.grades.${grade}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="legacy-tajweed">
                {t("memorization.tajweedScore")}
              </FieldLabel>
              <Input
                id="legacy-tajweed"
                type="number"
                min={0}
                max={100}
                {...register("tajweedScore", { valueAsNumber: true })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="legacy-notes">
                {t("memorization.teacherNotes")}
              </FieldLabel>
              <Textarea id="legacy-notes" rows={3} {...register("teacherNotes")} />
            </Field>
          </FieldGroup>
        </form>
      </EntityFormDialog>
    </div>
  );
}
