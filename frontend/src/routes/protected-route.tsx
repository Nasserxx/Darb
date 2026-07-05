import type { ReactNode } from "react";
import { Navigate, useLocation, useParams } from "react-router-dom";

import { Skeleton } from "@/components/ui/skeleton";
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
    return (
      <div className="flex min-h-svh flex-col gap-4 bg-background p-8">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-6 w-72" />
        <Skeleton className="h-40 w-full max-w-2xl" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to={loginPath} replace state={{ from: location }} />;
  }

  return children;
}
