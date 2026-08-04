import { useTranslation } from "react-i18next";

import { Badge } from "@/components/ui/badge";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { translateRole } from "@/i18n/index.ts";
import type { UserRole } from "@/lib/types/api.ts";
import { cn } from "@/lib/utils.ts";

function resolveScopeLabel(
  role: UserRole,
  options: {
    mosqueName?: string | null;
    pendingMosqueName?: string | null;
    linkedChildCount?: number;
    t: (key: string, options?: Record<string, unknown>) => string;
  },
): string {
  const { mosqueName, pendingMosqueName, linkedChildCount, t } = options;

  switch (role) {
    case "SUPER_ADMIN":
      return t("capability.scope.platformAllMosques");
    case "PARENT": {
      if (linkedChildCount && linkedChildCount > 0) {
        return t("capability.scope.linkedChildrenCount", {
          count: linkedChildCount,
        });
      }
      return t("capability.scope.linkedChildren");
    }
    case "MOSQUE_ADMIN":
    case "TEACHER":
    case "STUDENT":
      if (mosqueName) {
        return mosqueName;
      }
      if (pendingMosqueName) {
        return t("capability.scope.pendingMosque", {
          mosque: pendingMosqueName,
        });
      }
      return t("capability.scope.mosque");
    default:
      return t("capability.scope.mosque");
  }
}

type CapabilityChipProps = {
  className?: string;
};

export function CapabilityChip({ className }: CapabilityChipProps) {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { profile } = useWorkspace();

  if (!user) {
    return null;
  }

  const role = user.role.toUpperCase().replace(/-/g, "_") as UserRole;
  const roleLabel = translateRole(role);
  const scopeLabel = resolveScopeLabel(role, {
    mosqueName: profile?.mosqueName,
    pendingMosqueName: profile?.pendingMosqueName,
    linkedChildCount: profile?.parentStudentIds?.length,
    t,
  });
  const label = `${roleLabel} · ${scopeLabel}`;

  return (
    <Tooltip>
      <TooltipTrigger>
        <Badge
          variant="secondary"
          className={cn("max-w-[min(100%,16rem)] truncate font-normal", className)}
          title={label}
        >
          {label}
        </Badge>
      </TooltipTrigger>
      <TooltipContent side="bottom" className="max-w-xs">
        {t("capability.tooltip")}
      </TooltipContent>
    </Tooltip>
  );
}
