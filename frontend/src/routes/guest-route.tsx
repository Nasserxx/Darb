import type { ReactNode } from "react";
import { Navigate, useParams } from "react-router-dom";

import { Skeleton } from "@/components/ui/skeleton.tsx";
import { useAuth } from "../features/auth/hooks/use-auth.ts";
import { useWorkspace } from "../features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "../i18n/index.ts";
import { resolvePostAuthPath } from "../lib/navigation/post-auth.ts";

type GuestRouteProps = {
  children: ReactNode;
};

export function GuestRoute({ children }: GuestRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const { profileStatus, isLoading: workspaceLoading } = useWorkspace();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  if (isLoading || (isAuthenticated && workspaceLoading)) {
    return (
      <div className="flex min-h-svh flex-col gap-4 bg-background p-8">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-6 w-72" />
      </div>
    );
  }

  if (isAuthenticated && user) {
    const path = resolvePostAuthPath({
      locale: localePrefix,
      role: user.role,
      profileStatus,
    });
    if (path) {
      return <Navigate to={path} replace />;
    }
  }

  return children;
}
