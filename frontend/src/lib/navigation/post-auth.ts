import {
  getDefaultLandingPath,
  getNavItemsForRole,
} from "@/lib/navigation/app-nav.ts";

import type { WorkspaceProfile } from "@/features/workspace/context/workspace-provider.tsx";

export type ProfileStatus = "unknown" | "assigned" | "unassigned" | "pending";

export function deriveProfileStatus(
  profile: WorkspaceProfile | null,
  isLoading: boolean,
): ProfileStatus {
  if (isLoading) return "unknown";
  if (!profile) return "unassigned";
  if (profile.membershipStatus === "PENDING") return "pending";
  if (profile.mosqueId || (profile.parentStudentIds?.length ?? 0) > 0) {
    return "assigned";
  }
  if (profile.membershipStatus === "ASSIGNED" && profile.profileId) {
    return "assigned";
  }
  return "unassigned";
}

function stripLocalePrefix(path: string): string {
  return path.replace(/^\/[a-z]{2}(?=\/)/, "") || path;
}

export function isPathAllowedForRole(role: string, path: string): boolean {
  const normalized = stripLocalePrefix(path);
  const allowed = getNavItemsForRole(role).map((item) => item.href);
  return allowed.some(
    (href) => normalized === href || normalized.startsWith(`${href}/`),
  );
}

// ponytail: single post-auth destination resolver
export function resolvePostAuthPath(options: {
  locale: string;
  role: string;
  profileStatus: ProfileStatus;
  returnTo?: string | null;
  inviteCode?: string | null;
  roleHint?: string | null;
}): string | null {
  const { locale, role, profileStatus, returnTo, inviteCode, roleHint } =
    options;

  if (profileStatus === "unknown") return null;

  if (profileStatus === "assigned") {
    if (returnTo && isPathAllowedForRole(role, returnTo)) {
      return returnTo.startsWith("/") ? returnTo : `/${locale}${returnTo}`;
    }
    return `/${locale}${getDefaultLandingPath(role)}`;
  }

  if (profileStatus === "pending") {
    return `/${locale}/onboarding?state=pending`;
  }

  const params = new URLSearchParams();
  if (inviteCode) params.set("code", inviteCode);
  if (roleHint) params.set("role", roleHint);
  const query = params.toString();
  return `/${locale}/onboarding${query ? `?${query}` : ""}`;
}

export const JOIN_INTENT_KEY = "darb.joinIntent";

export function saveJoinIntent(code: string, role: string): void {
  sessionStorage.setItem(JOIN_INTENT_KEY, JSON.stringify({ code, role }));
}

export function consumeJoinIntent(): { code: string; role: string } | null {
  const raw = sessionStorage.getItem(JOIN_INTENT_KEY);
  if (!raw) return null;
  sessionStorage.removeItem(JOIN_INTENT_KEY);
  try {
    const parsed = JSON.parse(raw) as { code?: string; role?: string };
    if (parsed.code && parsed.role) {
      return { code: parsed.code, role: parsed.role };
    }
  } catch {
    return null;
  }
  return null;
}
