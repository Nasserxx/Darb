import { useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { AwardIcon } from "lucide-react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EmptyState } from "@/components/shared/empty-state.tsx";
import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import {
  useAchievementsByMosque,
  useAchievementsByStudent,
  useCreateAchievement,
} from "@/features/achievements/hooks/use-achievements.ts";
import {
  achievementCreateSchema,
  toAchievementCreateRequest,
  type AchievementCreateFormValues,
} from "@/features/achievements/schemas/achievement-create.schema.ts";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import { useStudents } from "@/features/students/hooks/use-students.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";

const ACHIEVEMENT_TYPES = [
  "MEMORIZATION",
  "ATTENDANCE",
  "RECITATION",
  "COMPETITION",
  "MILESTONE",
] as const;

function todayLocalDate(): string {
  return new Date().toISOString().slice(0, 10);
}

export function AchievementsPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { mosqueId, profile } = useWorkspace();
  const { params } = usePagination(24);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedMosqueId, setSelectedMosqueId] = useState<string>("");

  const role = user ? normalizeApiRole(user.role) : null;
  const isStudent = role === "STUDENT";
  const canManage = role
    ? ["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"].includes(role)
    : false;

  const effectiveMosqueId =
    role === "SUPER_ADMIN" ? selectedMosqueId || mosqueId : mosqueId;

  const { data: mosquesPage } = useMosques({ page: 0, size: 100 });
  const { data: studentsPage } = useStudents({ page: 0, size: 500 });

  const { data: studentData, isLoading: studentLoading } =
    useAchievementsByStudent(
      isStudent ? profile?.studentId : undefined,
      params,
    );
  const { data: mosqueData, isLoading: mosqueLoading } =
    useAchievementsByMosque(
      !isStudent ? (effectiveMosqueId ?? undefined) : undefined,
      params,
    );

  const createAchievement = useCreateAchievement();

  const achievements = isStudent ? studentData : mosqueData;
  const isLoading = isStudent ? studentLoading : mosqueLoading;

  const mosqueStudents = useMemo(
    () =>
      (studentsPage?.content ?? []).filter(
        (student) =>
          !effectiveMosqueId || student.mosqueId === effectiveMosqueId,
      ),
    [effectiveMosqueId, studentsPage?.content],
  );

  const {
    register,
    control,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<AchievementCreateFormValues>({
    resolver: zodResolver(achievementCreateSchema),
    defaultValues: {
      studentId: "",
      mosqueId: effectiveMosqueId ?? "",
      type: "MILESTONE",
      title: "",
      description: "",
      badgeUrl: "",
      awardedDate: todayLocalDate(),
    },
  });

  async function onSubmit(values: AchievementCreateFormValues) {
    if (!effectiveMosqueId) {
      toast.error(t("achievements.selectMosque"));
      return;
    }

    try {
      await createAchievement.mutateAsync(
        toAchievementCreateRequest({
          ...values,
          mosqueId: effectiveMosqueId,
        }),
      );
      toast.success(t("achievements.saved"));
      setDialogOpen(false);
      reset();
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, setError, t);
      }
      toast.error(message || t("achievements.saveError"));
    }
  }

  const showMosquePicker = role === "SUPER_ADMIN" && !mosqueId;

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("achievements.title")}
        description={t("achievements.description")}
        actions={
          canManage && effectiveMosqueId ? (
            <Button onClick={() => setDialogOpen(true)}>
              {t("achievements.create")}
            </Button>
          ) : null
        }
      />

      {showMosquePicker ? (
        <FieldGroup className="max-w-sm">
          <Field>
            <FieldLabel>{t("onboarding.mosque")}</FieldLabel>
            <Select value={selectedMosqueId} onValueChange={setSelectedMosqueId}>
              <SelectTrigger>
                <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
              </SelectTrigger>
              <SelectContent>
                {(mosquesPage?.content ?? []).map((mosque) => (
                  <SelectItem key={mosque.id} value={mosque.id}>
                    {mosque.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
        </FieldGroup>
      ) : null}

      {!isStudent && !effectiveMosqueId ? (
        <EmptyState
          title={t("achievements.title")}
          description={t("achievements.selectMosque")}
          icon={<AwardIcon />}
        />
      ) : isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-40 w-full" />
          ))}
        </div>
      ) : (achievements?.content ?? []).length === 0 ? (
        <EmptyState
          title={t("empty.title")}
          description={t("empty.description")}
          icon={<AwardIcon />}
          actionLabel={canManage ? t("achievements.create") : undefined}
          onAction={canManage ? () => setDialogOpen(true) : undefined}
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(achievements?.content ?? []).map((achievement) => (
            <Card key={achievement.id} className="overflow-hidden">
              <CardHeader className="flex flex-row items-start justify-between gap-2 pb-2">
                <CardTitle className="text-base font-semibold leading-snug">
                  {achievement.title}
                </CardTitle>
                <Badge variant="secondary">
                  {t(`achievements.types.${achievement.type}`)}
                </Badge>
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                {achievement.badgeUrl ? (
                  <img
                    src={achievement.badgeUrl}
                    alt={achievement.title}
                    className="h-24 w-24 rounded-lg object-cover"
                  />
                ) : (
                  <div className="flex h-24 w-24 items-center justify-center rounded-lg bg-muted">
                    <AwardIcon className="size-10 text-muted-foreground" />
                  </div>
                )}
                {achievement.description ? (
                  <p className="text-sm text-muted-foreground line-clamp-3">
                    {achievement.description}
                  </p>
                ) : null}
                {achievement.awardedDate ? (
                  <p className="text-xs text-muted-foreground">
                    {t("achievements.awardedDate")}: {achievement.awardedDate}
                  </p>
                ) : null}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <EntityFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        title={t("achievements.create")}
        submitLabel={t("actions.save")}
        isPending={createAchievement.isPending}
        onSubmit={() => void handleSubmit(onSubmit)()}
      >
        <form className="flex flex-col gap-4" onSubmit={(event) => event.preventDefault()}>
          <FieldGroup>
            <Field data-invalid={!!errors.studentId}>
              <FieldLabel>{t("achievements.student")}</FieldLabel>
              <Controller
                control={control}
                name="studentId"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
                    </SelectTrigger>
                    <SelectContent>
                      {mosqueStudents.map((student) => (
                        <SelectItem key={student.id} value={student.id}>
                          {student.id.slice(0, 8)}…
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field data-invalid={!!errors.type}>
              <FieldLabel>{t("achievements.type")}</FieldLabel>
              <Controller
                control={control}
                name="type"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ACHIEVEMENT_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {t(`achievements.types.${type}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
            <Field data-invalid={!!errors.title}>
              <FieldLabel htmlFor="achievement-title">{t("goals.goalTitle")}</FieldLabel>
              <Input id="achievement-title" {...register("title")} />
            </Field>
            <Field>
              <FieldLabel htmlFor="achievement-description">
                {t("messages.content")}
              </FieldLabel>
              <Textarea id="achievement-description" rows={3} {...register("description")} />
            </Field>
            <Field>
              <FieldLabel htmlFor="awarded-date">{t("achievements.awardedDate")}</FieldLabel>
              <Input id="awarded-date" type="date" {...register("awardedDate")} />
            </Field>
          </FieldGroup>
        </form>
      </EntityFormDialog>
    </div>
  );
}
