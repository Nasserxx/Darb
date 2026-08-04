import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { EnrollmentsTable } from "@/features/enrollments/components/enrollments-table.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { canManageEnrollments } from "@/lib/navigation/role-permissions.ts";

export function EnrollmentsPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const canManage = canManageEnrollments(user?.role);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("enrollments.title")}
        description={t("enrollments.description")}
      />
      <EnrollmentsTable canManage={canManage} />
    </div>
  );
}
