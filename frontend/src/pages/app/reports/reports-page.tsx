import { zodResolver } from "@hookform/resolvers/zod";
import { DownloadIcon, PlusIcon } from "lucide-react";
import { useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { DataTable } from "@/components/shared/data-table.tsx";
import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import {
  useCreateReport,
  useMosqueReports,
} from "@/features/reports/hooks/use-reports.ts";
import {
  reportCreateSchema,
  type ReportCreateBody,
} from "@/features/reports/schemas/report.schema.ts";
import type { ReportResponse } from "@/features/reports/types/index.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import type { ReportType } from "@/lib/types/api.ts";

const REPORT_TYPES: ReportType[] = [
  "ATTENDANCE_SUMMARY",
  "FINANCIAL",
  "STUDENT_PROGRESS",
  "TEACHER_PERFORMANCE",
  "ENROLLMENT",
];

function formatInstant(value: string): string {
  return new Date(value).toLocaleString();
}

export function ReportsPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { mosqueId: workspaceMosqueId } = useWorkspace();
  const isSuperAdmin = user ? normalizeApiRole(user.role) === "SUPER_ADMIN" : false;
  const [selectedMosqueId, setSelectedMosqueId] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const { params, setPage } = usePagination();

  const activeMosqueId = isSuperAdmin
    ? selectedMosqueId || workspaceMosqueId || undefined
    : workspaceMosqueId ?? undefined;

  const { data: mosquesPage } = useMosques({ page: 0, size: 100 });
  const { data, isLoading } = useMosqueReports(activeMosqueId, params);

  const columns = useMemo(
    () => [
      {
        id: "title",
        header: t("reports.reportTitle"),
        cell: (row: ReportResponse) => (
          <span className="font-medium">{row.title}</span>
        ),
      },
      {
        id: "type",
        header: t("reports.type"),
        cell: (row: ReportResponse) => (
          <Badge variant="outline">{row.type}</Badge>
        ),
      },
      {
        id: "generatedAt",
        header: t("reports.generatedAt"),
        cell: (row: ReportResponse) => (
          <span className="text-sm text-muted-foreground">
            {formatInstant(row.generatedAt)}
          </span>
        ),
      },
      {
        id: "actions",
        header: "",
        className: "w-32 text-right",
        cell: (row: ReportResponse) =>
          row.fileUrl ? (
            <Button variant="outline" size="sm" asChild>
              <a href={row.fileUrl} target="_blank" rel="noreferrer">
                <DownloadIcon data-icon="inline-start" />
                {t("reports.download")}
              </a>
            </Button>
          ) : (
            <span className="text-sm text-muted-foreground">—</span>
          ),
      },
    ],
    [t],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("reports.title")}
        description={t("reports.description")}
        actions={
          <Button disabled={!activeMosqueId} onClick={() => setCreateOpen(true)}>
            <PlusIcon data-icon="inline-start" />
            {t("reports.create")}
          </Button>
        }
      />

      {isSuperAdmin ? (
        <Field className="max-w-sm">
          <FieldLabel>{t("reports.mosque")}</FieldLabel>
          <Select
            value={selectedMosqueId || activeMosqueId || ""}
            onValueChange={(value) => {
              setSelectedMosqueId(value);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder={t("reports.mosquePlaceholder")} />
            </SelectTrigger>
            <SelectContent>
              {mosquesPage?.content.map((mosque) => (
                <SelectItem key={mosque.id} value={mosque.id}>
                  {mosque.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Field>
      ) : null}

      {!activeMosqueId ? (
        <p className="text-sm text-muted-foreground">{t("reports.mosqueRequired")}</p>
      ) : (
        <DataTable
          columns={columns}
          data={data}
          isLoading={isLoading}
          emptyMessage={t("reports.empty")}
          onPageChange={setPage}
        />
      )}

      {activeMosqueId ? (
        <ReportCreateDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          mosqueId={activeMosqueId}
        />
      ) : null}
    </div>
  );
}

function ReportCreateDialog({
  open,
  onOpenChange,
  mosqueId,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mosqueId: string;
}) {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const createReport = useCreateReport();

  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<ReportCreateBody>({
    resolver: zodResolver(reportCreateSchema),
    defaultValues: {
      mosqueId,
      generatedBy: user?.userId,
      type: "ATTENDANCE_SUMMARY",
      title: "",
      filters: "",
    },
  });

  async function onSubmit(values: ReportCreateBody) {
    try {
      await createReport.mutateAsync({
        ...values,
        mosqueId,
        generatedBy: user!.userId,
        filters: values.filters?.trim() || undefined,
      });
      toast.success(t("reports.createSuccess"));
      reset({ mosqueId, type: "ATTENDANCE_SUMMARY", title: "", filters: "" });
      onOpenChange(false);
    } catch (error) {
      const mapped = toMutationError(error, t);
      if (mapped.fieldErrors) applyFieldErrors(mapped.fieldErrors, setError, t);
      toast.error(mapped.message);
    }
  }

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("reports.create")}
      submitLabel={t("actions.generate")}
      isPending={createReport.isPending}
      onSubmit={() => void handleSubmit(onSubmit)()}
    >
      <FieldGroup>
        <Field>
          <FieldLabel>{t("reports.type")}</FieldLabel>
          <Controller
            control={control}
            name="type"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {REPORT_TYPES.map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field data-invalid={!!errors.title}>
          <FieldLabel>{t("reports.reportTitle")}</FieldLabel>
          <Input {...register("title")} />
          {errors.title ? (
            <p className="text-sm text-destructive">{errors.title.message}</p>
          ) : null}
        </Field>
        <Field>
          <FieldLabel>{t("reports.filters")}</FieldLabel>
          <Textarea
            rows={3}
            placeholder={t("reports.filtersPlaceholder")}
            {...register("filters")}
          />
        </Field>
      </FieldGroup>
    </EntityFormDialog>
  );
}
