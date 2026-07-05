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
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useEnrollments } from "@/features/enrollments/hooks/use-enrollments.ts";
import {
  useCreateGoal,
  useGoalsByStudent,
  useUpdateGoal,
} from "@/features/goals/hooks/use-goals.ts";
import {
  goalCreateSchema,
  toGoalCreateRequest,
  type GoalCreateFormValues,
} from "@/features/goals/schemas/goal-create.schema.ts";
import type { GoalResponse } from "@/features/goals/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import type { GoalStatus } from "@/lib/types/api.ts";

const GOAL_STATUSES: GoalStatus[] = [
  "IN_PROGRESS",
  "COMPLETED",
  "OVERDUE",
  "CANCELLED",
];

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

export function StudentGoalsPage() {
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

  const { data, isLoading } = useGoalsByStudent(studentId, params);
  const { data: enrollmentsPage } = useEnrollments({ page: 0, size: 500 });
  const createGoal = useCreateGoal();
  const updateGoal = useUpdateGoal();

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
  } = useForm<GoalCreateFormValues>({
    resolver: zodResolver(goalCreateSchema),
    defaultValues: {
      studentId: studentId ?? "",
      circleId: enrolledCircles[0] ?? "",
      title: "",
      targetSurah: undefined,
      targetJuz: undefined,
      status: "IN_PROGRESS",
      dueDate: "",
    },
  });

  if (!studentId || !role || !canAccessStudent(role, studentId, profile)) {
    return <Navigate to={`/${DEFAULT_LOCALE}/forbidden`} replace />;
  }

  async function handleStatusChange(goalId: string, status: GoalStatus) {
    try {
      await updateGoal.mutateAsync({
        id: goalId,
        body: {
          status,
          ...(status === "COMPLETED"
            ? { completedDate: new Date().toISOString().slice(0, 10) }
            : {}),
        },
      });
      toast.success(t("goals.updated"));
    } catch (error) {
      const { message } = toMutationError(error, t);
      toast.error(message || t("goals.saveError"));
    }
  }

  const columns = [
    {
      id: "title",
      header: t("goals.goalTitle"),
      cell: (row: GoalResponse) => (
        <span className="font-medium">{row.title}</span>
      ),
    },
    {
      id: "targets",
      header: t("goals.targetSurah"),
      cell: (row: GoalResponse) => {
        const parts: string[] = [];
        if (row.targetSurah) parts.push(`S${row.targetSurah}`);
        if (row.targetJuz) parts.push(`J${row.targetJuz}`);
        return parts.length > 0 ? parts.join(" · ") : "—";
      },
    },
    {
      id: "dueDate",
      header: t("goals.dueDate"),
      cell: (row: GoalResponse) => row.dueDate ?? "—",
    },
    {
      id: "status",
      header: t("goals.status"),
      cell: (row: GoalResponse) =>
        canManage ? (
          <Select
            value={row.status ?? "IN_PROGRESS"}
            onValueChange={(value) =>
              void handleStatusChange(row.id, value as GoalStatus)
            }
          >
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {GOAL_STATUSES.map((status) => (
                <SelectItem key={status} value={status}>
                  {t(`goals.statuses.${status}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        ) : (
          <Badge variant="secondary">
            {t(`goals.statuses.${row.status ?? "IN_PROGRESS"}`)}
          </Badge>
        ),
    },
  ];

  async function onSubmit(values: GoalCreateFormValues) {
    try {
      await createGoal.mutateAsync(
        toGoalCreateRequest({ ...values, studentId: studentId! }),
      );
      toast.success(t("goals.saved"));
      setDialogOpen(false);
      reset();
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, setError, t);
      }
      toast.error(message || t("goals.saveError"));
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("goals.title")}
        description={t("goals.description")}
        actions={
          canManage ? (
            <Button onClick={() => setDialogOpen(true)}>{t("goals.create")}</Button>
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
        title={t("goals.create")}
        submitLabel={t("actions.save")}
        isPending={createGoal.isPending}
        onSubmit={() => void handleSubmit(onSubmit)()}
      >
        <form className="flex flex-col gap-4" onSubmit={(event) => event.preventDefault()}>
          <FieldGroup>
            <Field data-invalid={!!errors.title}>
              <FieldLabel htmlFor="goal-title">{t("goals.goalTitle")}</FieldLabel>
              <Input id="goal-title" {...register("title")} />
            </Field>
            <Field data-invalid={!!errors.circleId}>
              <FieldLabel>{t("goals.circle")}</FieldLabel>
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
            <div className="grid gap-4 sm:grid-cols-2">
              <Field>
                <FieldLabel htmlFor="target-surah">{t("goals.targetSurah")}</FieldLabel>
                <Input
                  id="target-surah"
                  type="number"
                  min={1}
                  max={114}
                  {...register("targetSurah", { valueAsNumber: true })}
                />
              </Field>
              <Field>
                <FieldLabel htmlFor="target-juz">{t("goals.targetJuz")}</FieldLabel>
                <Input
                  id="target-juz"
                  type="number"
                  min={1}
                  max={30}
                  {...register("targetJuz", { valueAsNumber: true })}
                />
              </Field>
            </div>
            <Field>
              <FieldLabel htmlFor="due-date">{t("goals.dueDate")}</FieldLabel>
              <Input id="due-date" type="date" {...register("dueDate")} />
            </Field>
          </FieldGroup>
        </form>
      </EntityFormDialog>
    </div>
  );
}
