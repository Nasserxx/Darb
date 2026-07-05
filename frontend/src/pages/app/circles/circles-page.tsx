import { useState } from "react";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { CircleFormDialog } from "@/features/circles/components/circle-form-dialog.tsx";
import { CirclesTable } from "@/features/circles/components/circles-table.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { canManageCircles } from "@/lib/navigation/role-permissions.ts";

export function CirclesPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const canWrite = canManageCircles(user?.role);
  const [createOpen, setCreateOpen] = useState(false);

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
