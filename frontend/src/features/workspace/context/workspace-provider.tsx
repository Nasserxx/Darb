import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  deriveProfileStatus,
  type ProfileStatus,
} from "@/lib/navigation/post-auth.ts";
import type { UserRole } from "@/lib/types/api.ts";

import { workspaceApi } from "../api/workspace-api.ts";

export type MembershipStatus = "ASSIGNED" | "PENDING" | "NONE";

export interface WorkspaceProfile {
  profileId: string;
  mosqueId: string | null;
  teacherId?: string;
  studentId?: string;
  parentStudentIds?: string[];
  membershipStatus?: MembershipStatus;
  mosqueName?: string | null;
  pendingMosqueName?: string | null;
}

export interface WorkspaceContextValue {
  profile: WorkspaceProfile | null;
  mosqueId: string | null;
  profileStatus: ProfileStatus;
  isLoading: boolean;
  needsOnboarding: boolean;
  refreshProfile: () => Promise<void>;
}

const WorkspaceContext = createContext<WorkspaceContextValue | null>(null);

const ROLES_NEEDING_PROFILE: UserRole[] = [
  "MOSQUE_ADMIN",
  "TEACHER",
  "STUDENT",
  "PARENT",
];

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { user, isAuthenticated } = useAuth();
  const [profile, setProfile] = useState<WorkspaceProfile | null>(null);
  const [isLoading, setIsLoading] = useState(isAuthenticated && user !== null);

  const refreshProfile = useCallback(async () => {
    if (!user) {
      setProfile(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const resolved = await workspaceApi.getProfile();
      setProfile(resolved);
    } catch {
      setProfile(null);
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  useEffect(() => {
    void (async () => {
      if (isAuthenticated && user) {
        setIsLoading(true);
        await refreshProfile();
      } else {
        setProfile(null);
        setIsLoading(false);
      }
    })();
  }, [isAuthenticated, user, refreshProfile]);

  const profileStatus = useMemo(
    () => deriveProfileStatus(profile, isLoading),
    [profile, isLoading],
  );

  const needsOnboarding = useMemo(() => {
    if (!user || !isAuthenticated) return false;
    const role = user.role.toUpperCase().replace(/-/g, "_") as UserRole;
    if (!ROLES_NEEDING_PROFILE.includes(role)) return false;
    return profileStatus === "unassigned" || profileStatus === "pending";
  }, [user, isAuthenticated, profileStatus]);

  const value = useMemo<WorkspaceContextValue>(
    () => ({
      profile,
      mosqueId: profile?.mosqueId ?? null,
      profileStatus,
      isLoading,
      needsOnboarding,
      refreshProfile,
    }),
    [profile, profileStatus, isLoading, needsOnboarding, refreshProfile],
  );

  return (
    <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
  );
}

// useWorkspace is a hook, not a component; co-located with the provider contract
// eslint-disable-next-line react-refresh/only-export-components
export function useWorkspace(): WorkspaceContextValue {
  const context = useContext(WorkspaceContext);
  if (!context) {
    throw new Error("useWorkspace must be used within WorkspaceProvider");
  }
  return context;
}
