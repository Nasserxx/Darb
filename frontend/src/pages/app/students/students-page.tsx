import { useState } from "react";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { ParentChildrenView } from "@/features/parent-students/components/parent-children-view.tsx";
import { StudentFormDialog } from "@/features/students/components/student-form-dialog.tsx";
import { StudentsTable } from "@/features/students/components/students-table.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { canManageStudents } from "@/lib/navigation/role-permissions.ts";

export function StudentsPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const role = user?.role.toUpperCase().replace(/-/g, "_");
  const isParent = role === "PARENT";
  const canWrite = canManageStudents(user?.role);
  const [createOpen, setCreateOpen] = useState(false);

  if (isParent) {
    return (
      <div className="auth-stagger flex flex-col gap-6">
        <PageHeader
          title={t("students.title")}
          description={t("students.description")}
        />
        <ParentChildrenView />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("students.title")}
        description={t("students.description")}
        actions={
          canWrite ? (
            <Button onClick={() => setCreateOpen(true)}>{t("students.create")}</Button>
          ) : null
        }
      />
      <StudentsTable canWrite={canWrite} />
      {canWrite ? (
        <StudentFormDialog open={createOpen} onOpenChange={setCreateOpen} />
      ) : null}
    </div>
  );
}
