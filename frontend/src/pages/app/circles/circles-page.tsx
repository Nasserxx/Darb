import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { CircleFormDialog } from "@/features/circles/components/circle-form-dialog.tsx";
import { CirclesTable } from "@/features/circles/components/circles-table.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { useStudentEnrollments } from "@/features/enrollments/hooks/use-enrollments.ts";
import { canManageCircles } from "@/lib/navigation/role-permissions.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import { ParentCirclesView } from "./parent-circles-view.tsx";

function StudentCirclesView() {
  const { profile } = useWorkspace();
  const studentId = profile?.studentId;
  const { data: enrollmentsPage } = useStudentEnrollments(studentId, {
    page: 0,
    size: 500,
  });

  const circleIds = useMemo(() => {
    if (!enrollmentsPage?.content) return undefined;
    const ids = new Set<string>();
    for (const enrollment of enrollmentsPage.content) {
      if (enrollment.status === "ACTIVE") {
        ids.add(enrollment.circleId);
      }
    }
    return ids.size > 0 ? ids : undefined;
  }, [enrollmentsPage?.content]);

  return <CirclesTable canWrite={false} circleIds={circleIds} />;
}

export function CirclesPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { profile } = useWorkspace();
  const role = user ? normalizeApiRole(user.role) : null;
  const isParent = role === "PARENT";
  const isStudent = role === "STUDENT";
  const canWrite = canManageCircles(user?.role);
  const [createOpen, setCreateOpen] = useState(false);

  if (isParent) {
    return (
      <ParentCirclesView parentStudentIds={profile?.parentStudentIds ?? []} />
    );
  }

  if (isStudent) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader
          title={t("circles.title")}
          description={t("circles.studentDescription")}
        />
        <StudentCirclesView />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("circles.title")}
        description={t("circles.description")}
        actions={
          canWrite ? (
            <Button onClick={() => setCreateOpen(true)}>{t("circles.create")}</Button>
          ) : null
        }
      />
      <CirclesTable canWrite={canWrite} />
      {canWrite ? (
        <CircleFormDialog open={createOpen} onOpenChange={setCreateOpen} />
      ) : null}
    </div>
  );
}
