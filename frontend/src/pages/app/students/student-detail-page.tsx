import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

import { ConfirmDeleteDialog } from "@/components/shared/confirm-delete-dialog.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { StudentDetailCard } from "@/features/students/components/student-detail-card.tsx";
import { StudentFormDialog } from "@/features/students/components/student-form-dialog.tsx";
import {
  useDeleteStudent,
  useStudent,
} from "@/features/students/hooks/use-students.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { canManageStudents } from "@/lib/navigation/role-permissions.ts";
import { Award, BookOpen, ChevronLeftIcon, CircleDot, MessageSquare } from "lucide-react";

export function StudentDetailPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const navigate = useNavigate();
  const { id, locale } = useParams<{ id: string; locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const canWrite = canManageStudents(user?.role);
  const { data: student, isLoading, isError } = useStudent(id ?? "");
  const deleteMutation = useDeleteStudent();
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  async function handleDelete() {
    if (!student) return;
    try {
      await deleteMutation.mutateAsync(student.id);
      toast.success(t("students.deleteSuccess"));
      navigate(`/${localePrefix}/students`, { replace: true });
    } catch (error) {
      toast.error(toMutationError(error, t).message);
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (isError || !student) {
    return (
      <div className="flex flex-col gap-4">
        <PageHeader title={t("students.detail")} />
        <p className="text-sm text-muted-foreground">{t("table.empty")}</p>
        <Button variant="outline" asChild>
          <Link to={`/${localePrefix}/students`}>
            <ChevronLeftIcon data-icon="inline-start" />
            {t("students.backToList")}
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("students.detail")}
        actions={
          canWrite ? (
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setEditOpen(true)}>
                {t("actions.edit")}
              </Button>
              <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
                {t("actions.delete")}
              </Button>
            </div>
          ) : null
        }
      />
      <Button variant="ghost" size="sm" className="w-fit" asChild>
        <Link to={`/${localePrefix}/students`}>
          <ChevronLeftIcon data-icon="inline-start" />
          {t("students.backToList")}
        </Link>
      </Button>
      <StudentDetailCard student={student} />

      <div className="flex flex-col gap-4">
        <h3 className="font-serif text-lg font-semibold">{t("studentRecord.title")}</h3>
        <div className="grid gap-4 sm:grid-cols-2">
          {[
            {
              to: `/${localePrefix}/memorization/student/${student.id}`,
              icon: BookOpen,
              title: t("studentRecord.memorization"),
              description: t("studentRecord.memorizationDescription"),
            },
            {
              to: `/${localePrefix}/goals/student/${student.id}`,
              icon: CircleDot,
              title: t("studentRecord.goals"),
              description: t("studentRecord.goalsDescription"),
            },
            {
              to: `/${localePrefix}/achievements`,
              icon: Award,
              title: t("studentRecord.achievements"),
              description: t("studentRecord.achievementsDescription"),
            },
            {
              to: `/${localePrefix}/messages`,
              icon: MessageSquare,
              title: t("studentRecord.messages"),
              description: t("studentRecord.messagesDescription"),
            },
          ].map((link) => (
            <Link key={link.to} to={link.to}>
              <Card className="h-full transition-colors hover:bg-muted/40">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-lg">
                    <link.icon className="text-primary" />
                    {link.title}
                  </CardTitle>
                  <CardDescription>{link.description}</CardDescription>
                </CardHeader>
                <CardContent />
              </Card>
            </Link>
          ))}
        </div>
      </div>

      {canWrite ? (
        <>
          <StudentFormDialog
            open={editOpen}
            onOpenChange={setEditOpen}
            student={student}
          />
          <ConfirmDeleteDialog
            open={deleteOpen}
            onOpenChange={setDeleteOpen}
            onConfirm={() => void handleDelete()}
            isPending={deleteMutation.isPending}
          />
        </>
      ) : null}
    </div>
  );
}
