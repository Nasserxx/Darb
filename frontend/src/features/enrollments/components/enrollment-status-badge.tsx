import { useTranslation } from "react-i18next";

import { Badge } from "@/components/ui/badge";
import type { EnrollmentStatus } from "@/lib/types/api.ts";

const STATUS_VARIANT: Record<
  EnrollmentStatus,
  "default" | "secondary" | "destructive" | "outline"
> = {
  PENDING: "outline",
  ACTIVE: "default",
  WITHDRAWN: "secondary",
  COMPLETED: "secondary",
  REJECTED: "destructive",
};

type EnrollmentStatusBadgeProps = {
  status: EnrollmentStatus;
};

export function EnrollmentStatusBadge({ status }: EnrollmentStatusBadgeProps) {
  const { t } = useTranslation("app");

  return (
    <Badge variant={STATUS_VARIANT[status]}>
      {t(`enums.enrollmentStatus.${status}`)}
    </Badge>
  );
}
