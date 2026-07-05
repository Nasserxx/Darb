import { useState } from "react";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { StudentFormDialog } from "@/features/students/components/student-form-dialog.tsx";
import { StudentsTable } from "@/features/students/components/students-table.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { canManageStudents } from "@/lib/navigation/role-permissions.ts";

export function StudentsPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const canWrite = canManageStudents(user?.role);
  const [createOpen, setCreateOpen] = useState(false);

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
