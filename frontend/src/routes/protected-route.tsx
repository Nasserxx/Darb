import type { ReactNode } from "react";
import { Navigate, useLocation, useParams } from "react-router-dom";

import { useAuth } from "../features/auth/hooks/use-auth.ts";
import { DEFAULT_LOCALE } from "../i18n/index.ts";

type ProtectedRouteProps = {
  children: ReactNode;
};

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const { locale } = useParams<{ locale: string }>();
  const location = useLocation();
  const loginPath = `/${locale ?? DEFAULT_LOCALE}/login`;

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to={loginPath} replace state={{ from: location }} />;
  }

  return children;
}
