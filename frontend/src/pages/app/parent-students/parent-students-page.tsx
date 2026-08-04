import { useState } from "react";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { ParentStudentFormDialog } from "@/features/parent-students/components/parent-student-form-dialog.tsx";
import { ParentStudentsTable } from "@/features/parent-students/components/parent-students-table.tsx";

export function ParentStudentsPage() {
  const { t } = useTranslation("app");
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("parentStudents.title")}
        description={t("parentStudents.description")}
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            {t("parentStudents.create")}
          </Button>
        }
      />
      <ParentStudentsTable />
      <ParentStudentFormDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}
