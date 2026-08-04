import { useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";

import { Skeleton } from "@/components/ui/skeleton.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { workspaceApi } from "@/features/workspace/api/workspace-api.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import {
  deriveProfileStatus,
  resolvePostAuthPath,
  saveJoinIntent,
} from "@/lib/navigation/post-auth.ts";

export function JoinRedirectPage() {
  const { locale } = useParams<{ locale: string }>();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, isLoading, user } = useAuth();
  const { isLoading: workspaceLoading, refreshProfile } = useWorkspace();
  const navigate = useNavigate();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  const code = searchParams.get("code") ?? "";
  const role = searchParams.get("role") ?? "";

  useEffect(() => {
    if (isLoading) return;

    if (code && role) {
      saveJoinIntent(code, role);
    }

    if (!isAuthenticated || !user) {
      navigate(`/${localePrefix}/login`, { replace: true });
      return;
    }

    if (workspaceLoading) return;

    void (async () => {
      const profile = await workspaceApi.getProfile();
      const status = deriveProfileStatus(profile, false);
      const path = resolvePostAuthPath({
        locale: localePrefix,
        role: user.role,
        profileStatus: status,
        inviteCode: code || null,
        roleHint: role || null,
      });
      await refreshProfile();
      navigate(path ?? `/${localePrefix}/onboarding`, { replace: true });
    })();
  }, [
    code,
    isAuthenticated,
    isLoading,
    localePrefix,
    navigate,
    refreshProfile,
    role,
    user,
    workspaceLoading,
  ]);

  return (
    <div className="flex min-h-svh flex-col gap-4 bg-background p-8">
      <Skeleton className="h-10 w-48" />
      <Skeleton className="h-6 w-72" />
    </div>
  );
}
