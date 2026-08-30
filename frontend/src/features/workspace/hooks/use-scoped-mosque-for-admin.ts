import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";

/** Hide mosque picker when MOSQUE_ADMIN already has a workspace mosqueId. */
export function useScopedMosqueForAdmin() {
  const { user } = useAuth();
  const { mosqueId } = useWorkspace();
  const role = user ? normalizeApiRole(user.role) : null;
  return {
    mosqueId: mosqueId ?? null,
    showMosqueField: role === "SUPER_ADMIN" || !mosqueId,
  };
}
