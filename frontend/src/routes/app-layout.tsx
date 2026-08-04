import type { ReactNode } from "react";
import { Navigate, useLocation, useParams } from "react-router-dom";

import { AppShell } from "@/components/app-shell/app-shell.tsx";
import { Skeleton } from "@/components/ui/skeleton";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";

const ONBOARDING_PATH = "/onboarding";

type AppLayoutProps = {
  children: ReactNode;
};

export function AppLayout({ children }: AppLayoutProps) {
  const { isLoading, needsOnboarding } = useWorkspace();
  const { locale } = useParams<{ locale: string }>();
  const location = useLocation();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const onboardingFullPath = `/${localePrefix}${ONBOARDING_PATH}`;

  if (isLoading) {
    return (
      <div className="flex min-h-svh flex-col gap-4 bg-background p-8">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-6 w-72" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (needsOnboarding && location.pathname !== onboardingFullPath) {
    return <Navigate to={onboardingFullPath} replace />;
  }

  if (location.pathname === onboardingFullPath) {
    return children;
  }

  return <AppShell>{children}</AppShell>;
}
