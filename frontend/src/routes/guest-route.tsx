import type { ReactNode } from "react";
import { Navigate, useParams } from "react-router-dom";

import { useAuth } from "../features/auth/hooks/use-auth.ts";
import { DEFAULT_LOCALE } from "../i18n/index.ts";

type GuestRouteProps = {
  children: ReactNode;
};

export function GuestRoute({ children }: GuestRouteProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const dashboardPath = `/${locale ?? DEFAULT_LOCALE}/dashboard`;

  if (isLoading) {
    return null;
  }

  if (isAuthenticated) {
    return <Navigate to={dashboardPath} replace />;
  }

  return children;
}
