import { Navigate, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { useState } from "react";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { MemberOnboarding } from "@/features/mosques/components/member-onboarding.tsx";
import { MosqueAdminOnboarding } from "@/features/mosques/components/mosque-admin-onboarding.tsx";
import { parentStudentsApi } from "@/features/parent-students/api/parent-students-api.ts";
import { useStudents } from "@/features/students/hooks/use-students.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { getDefaultLandingPath } from "@/lib/navigation/app-nav.ts";

export function OnboardingPage() {
  const { t } = useTranslation(["app", "auth"]);
  const { user } = useAuth();
  const { profile, profileStatus, isLoading, refreshProfile } = useWorkspace();
  const navigate = useNavigate();
  const { locale } = useParams<{ locale: string }>();
  const [searchParams] = useSearchParams();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const role = user?.role.toUpperCase().replace(/-/g, "_") ?? "";
  const [selectedId, setSelectedId] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { data: studentsPage, isLoading: studentsLoading } = useStudents({
    page: 0,
    size: 100,
  });

  if (!user) return null;

  if (isLoading || profileStatus === "unknown") {
    return (
      <div className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-8 p-8">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (profileStatus === "assigned") {
    return (
      <Navigate
        to={`/${localePrefix}${getDefaultLandingPath(user.role)}`}
        replace
      />
    );
  }

  async function finishOnboarding() {
    await refreshProfile();
    navigate(`/${localePrefix}${getDefaultLandingPath(user!.role)}`, {
      replace: true,
    });
  }

  async function handleParentSubmit() {
    if (!selectedId) {
      toast.error(t("onboarding.studentRequired"));
      return;
    }

    setIsSubmitting(true);
    try {
      await parentStudentsApi.create({
        parentUserId: user!.userId,
        studentId: selectedId,
        relationship: "parent",
        isPrimary: true,
        receivesNotifications: true,
      });
      toast.success(t("onboarding.success"));
      await finishOnboarding();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("onboarding.error"));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (role === "MOSQUE_ADMIN") {
    return (
      <div className="auth-stagger mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-8 p-8">
        <PageHeader
          title={t("onboarding.title")}
          description={t("onboarding.mosqueAdmin.description")}
        />
        <MosqueAdminOnboarding
          onComplete={() => {
            void finishOnboarding();
          }}
        />
      </div>
    );
  }

  if (role === "TEACHER" || role === "STUDENT") {
    const initialCode = searchParams.get("code") ?? undefined;
    const isPending =
      profileStatus === "pending" || searchParams.get("state") === "pending";

    return (
      <div className="auth-stagger mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-8 p-8">
        <PageHeader
          title={t("onboarding.title")}
          description={t("onboarding.description", {
            role: t(`auth:roles.${user.role.toLowerCase().replace(/-/g, "_")}`),
          })}
        />
        <MemberOnboarding
          role={role}
          initialCode={initialCode}
          isPending={isPending}
          pendingMosqueName={profile?.pendingMosqueName}
          onComplete={() => {
            void finishOnboarding();
          }}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-8 p-8">
      <PageHeader
        title={t("onboarding.title")}
        description={t("onboarding.description", {
          role: t(`auth:roles.${user.role.toLowerCase().replace(/-/g, "_")}`),
        })}
      />
      <Card>
        <CardHeader>
          <CardTitle>{t("onboarding.stepTitle")}</CardTitle>
          <CardDescription>{t("onboarding.stepDescription")}</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-6">
          <FieldGroup>
            <Field>
              <FieldLabel>{t("onboarding.studentLink")}</FieldLabel>
              <Select value={selectedId} onValueChange={setSelectedId} disabled={studentsLoading}>
                <SelectTrigger>
                  <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {studentsPage?.content.map((student) => (
                      <SelectItem key={student.id} value={student.id}>
                        {student.id.slice(0, 8)}…
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </Field>
          </FieldGroup>
          <p className="text-sm text-muted-foreground">{t("onboarding.parentHint")}</p>
          <Button disabled={isSubmitting || !selectedId} onClick={() => void handleParentSubmit()}>
            {t("onboarding.submit")}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
