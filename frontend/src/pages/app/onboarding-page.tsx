import { Navigate, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { MemberOnboarding } from "@/features/mosques/components/member-onboarding.tsx";
import { MosqueAdminOnboarding } from "@/features/mosques/components/mosque-admin-onboarding.tsx";
import { ParentOnboarding } from "@/features/mosques/components/parent-onboarding.tsx";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { getDefaultLandingPath } from "@/lib/navigation/app-nav.ts";

export function OnboardingPage() {
  const { t } = useTranslation(["app", "auth"]);
  const { user, logout } = useAuth();
  const { profile, profileStatus, isLoading, refreshProfile } = useWorkspace();
  const navigate = useNavigate();
  const { locale } = useParams<{ locale: string }>();
  const [searchParams] = useSearchParams();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const role = user?.role.toUpperCase().replace(/-/g, "_") ?? "";

  function handleLogout() {
    logout();
    navigate(`/${locale}/login`);
  }

  if (!user) return null;

  if (isLoading || profileStatus === "unknown") {
    return (
      <>
        <div className="fixed right-4 top-4 z-50">
          <button
            onClick={handleLogout}
            className="text-sm text-muted-foreground hover:text-foreground underline underline-offset-2"
          >
            {t("auth:logout", "Sign out")}
          </button>
        </div>
        <div className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-8 p-8">
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-48 w-full" />
        </div>
      </>
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

  if (role === "MOSQUE_ADMIN") {
    return (
      <>
        <div className="fixed right-4 top-4 z-50">
          <button
            onClick={handleLogout}
            className="text-sm text-muted-foreground hover:text-foreground underline underline-offset-2"
          >
            {t("auth:logout", "Sign out")}
          </button>
        </div>
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
      </>
    );
  }

  if (role === "TEACHER" || role === "STUDENT") {
    const initialCode = searchParams.get("code") ?? undefined;
    const isPending =
      profileStatus === "pending" || searchParams.get("state") === "pending";

    return (
      <>
        <div className="fixed right-4 top-4 z-50">
          <button
            onClick={handleLogout}
            className="text-sm text-muted-foreground hover:text-foreground underline underline-offset-2"
          >
            {t("auth:logout", "Sign out")}
          </button>
        </div>
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
      </>
    );
  }

  if (role === "PARENT") {
    return (
      <>
        <div className="fixed right-4 top-4 z-50">
          <button
            onClick={handleLogout}
            className="text-sm text-muted-foreground hover:text-foreground underline underline-offset-2"
          >
            {t("auth:logout", "Sign out")}
          </button>
        </div>
        <div className="auth-stagger mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-8 p-8">
          <PageHeader
            title={t("onboarding.title")}
            description={t("onboarding.description", {
              role: t(`auth:roles.${user.role.toLowerCase().replace(/-/g, "_")}`),
            })}
          />
          <ParentOnboarding
            onComplete={() => {
              void finishOnboarding();
            }}
          />
        </div>
      </>
    );
  }

  return null;
}
