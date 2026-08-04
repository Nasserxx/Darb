import type { ReactNode } from "react";
import { Navigate, useParams } from "react-router-dom";

import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import type { UserRole } from "@/lib/types/api.ts";

type RoleRouteProps = {
  allowed: UserRole[];
  children: ReactNode;
};

export function RoleRoute({ allowed, children }: RoleRouteProps) {
  const { user, isLoading } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 p-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    );
  }

  if (!user) {
    return <Navigate to={`/${localePrefix}/login`} replace />;
  }

  const role = normalizeApiRole(user.role);
  if (!allowed.includes(role)) {
    return <Navigate to={`/${localePrefix}/forbidden`} replace />;
  }

  return children;
}
