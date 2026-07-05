import { useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Navigate, useParams } from "react-router-dom";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
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
import { useEnrollments } from "@/features/enrollments/hooks/use-enrollments.ts";
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
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";

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

function canAccessStudent(
  role: ReturnType<typeof normalizeApiRole>,
  studentId: string,
  profile: ReturnType<typeof useWorkspace>["profile"],
): boolean {
  if (role === "STUDENT") {
    return profile?.studentId === studentId;
  }
  if (role === "PARENT") {
    return profile?.parentStudentIds?.includes(studentId) ?? false;
  }
  return ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"].includes(role);
}

export function StudentMemorizationPage() {
  const { t } = useTranslation("app");
  const { studentId } = useParams<{ studentId: string }>();
  const { user } = useAuth();
  const { profile } = useWorkspace();
  const { params, setPage } = usePagination();
  const [dialogOpen, setDialogOpen] = useState(false);

  const role = user ? normalizeApiRole(user.role) : null;
  const canManage = role
    ? ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"].includes(role)
    : false;

  const { data, isLoading } = useMemorizationByStudent(studentId, params);
  const { data: enrollmentsPage } = useEnrollments({ page: 0, size: 500 });
  const createMemorization = useCreateMemorization();

  const enrolledCircles = useMemo(
    () =>
      (enrollmentsPage?.content ?? [])
        .filter(
          (enrollment) =>
            enrollment.studentId === studentId &&
            enrollment.status === "ACTIVE",
        )
        .map((enrollment) => enrollment.circleId),
    [enrollmentsPage?.content, studentId],
  );

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
      studentId: studentId ?? "",
      circleId: enrolledCircles[0] ?? "",
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

  if (!studentId || !role || !canAccessStudent(role, studentId, profile)) {
    return <Navigate to={`/${DEFAULT_LOCALE}/forbidden`} replace />;
  }

  const columns = [
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
  ];

  async function onSubmit(values: MemorizationCreateFormValues) {
    try {
      await createMemorization.mutateAsync(
        toMemorizationCreateRequest({
          ...values,
          studentId: studentId!,
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
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("memorization.title")}
        description={t("memorization.description")}
        actions={
          canManage ? (
            <Button onClick={() => setDialogOpen(true)}>{t("memorization.create")}</Button>
          ) : null
        }
      />

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
                      {enrolledCircles.map((circleId) => (
                        <SelectItem key={circleId} value={circleId}>
                          {circleId.slice(0, 8)}…
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field data-invalid={!!errors.sessionDate}>
              <FieldLabel htmlFor="session-date">{t("memorization.sessionDate")}</FieldLabel>
              <Input id="session-date" type="date" {...register("sessionDate")} />
            </Field>
            <div className="grid gap-4 sm:grid-cols-3">
              <Field data-invalid={!!errors.surahNumber}>
                <FieldLabel htmlFor="surah">{t("memorization.surah")}</FieldLabel>
                <Input
                  id="surah"
                  type="number"
                  min={1}
                  max={114}
                  {...register("surahNumber", { valueAsNumber: true })}
                />
              </Field>
              <Field data-invalid={!!errors.ayahFrom}>
                <FieldLabel htmlFor="ayah-from">{t("memorization.ayahFrom")}</FieldLabel>
                <Input
                  id="ayah-from"
                  type="number"
                  min={1}
                  {...register("ayahFrom", { valueAsNumber: true })}
                />
              </Field>
              <Field data-invalid={!!errors.ayahTo}>
                <FieldLabel htmlFor="ayah-to">{t("memorization.ayahTo")}</FieldLabel>
                <Input
                  id="ayah-to"
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
                    onValueChange={(value) =>
                      field.onChange(value || undefined)
                    }
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
              <FieldLabel htmlFor="tajweed">{t("memorization.tajweedScore")}</FieldLabel>
              <Input
                id="tajweed"
                type="number"
                min={0}
                max={100}
                {...register("tajweedScore", { valueAsNumber: true })}
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="notes">{t("memorization.teacherNotes")}</FieldLabel>
              <Textarea id="notes" rows={3} {...register("teacherNotes")} />
            </Field>
          </FieldGroup>
        </form>
      </EntityFormDialog>
    </div>
  );
}
