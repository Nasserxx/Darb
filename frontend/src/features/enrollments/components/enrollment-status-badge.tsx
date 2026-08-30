import { useTranslation } from "react-i18next";

import { FormerMemberBadge } from "@/components/shared/former-member-badge";
import { Badge } from "@/components/ui/badge";
import type { EnrollmentStatus } from "@/lib/types/api.ts";

const STATUS_VARIANT: Record<
  Exclude<EnrollmentStatus, "WITHDRAWN">,
  "default" | "secondary" | "destructive" | "outline"
> = {
  PENDING: "outline",
  ACTIVE: "default",
  COMPLETED: "secondary",
  REJECTED: "destructive",
};

type EnrollmentStatusBadgeProps = {
  status: EnrollmentStatus;
};

export function EnrollmentStatusBadge({ status }: EnrollmentStatusBadgeProps) {
  const { t } = useTranslation("app");

  if (status === "WITHDRAWN") {
    return <FormerMemberBadge />;
  }

  return (
    <Badge variant={STATUS_VARIANT[status]}>
      {t(`enums.enrollmentStatus.${status}`)}
    </Badge>
  );
}
