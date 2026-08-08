import { zodResolver } from "@hookform/resolvers/zod";
import { PlusIcon } from "lucide-react";
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
import { useCircles } from "@/features/circles/hooks/use-circles.ts";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import {
  useCreatePayment,
  useMosquePayments,
  usePayments,
  useUpdatePayment,
} from "@/features/payments/hooks/use-payments.ts";
import {
  paymentCreateSchema,
  paymentUpdateSchema,
  type PaymentCreateBody,
  type PaymentUpdateBody,
} from "@/features/payments/schemas/payment.schema.ts";
import type { PaymentResponse } from "@/features/payments/types/index.ts";
import { useStudents } from "@/features/students/hooks/use-students.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { usePagination } from "@/lib/hooks/use-pagination.ts";
import { normalizeApiRole } from "@/lib/navigation/app-nav.ts";
import type {
  PaymentCycle,
  PaymentMethod,
  PaymentStatus,
} from "@/lib/types/api.ts";

const PAYMENT_STATUSES: PaymentStatus[] = [
  "PENDING",
  "PARTIAL",
  "PAID",
  "OVERDUE",
  "WAIVED",
  "REFUNDED",
];

const PAYMENT_METHODS: PaymentMethod[] = [
  "CASH",
  "BANK_TRANSFER",
  "CARD",
  "ONLINE",
  "OTHER",
];

const PAYMENT_CYCLES: PaymentCycle[] = [
  "MONTHLY",
  "QUARTERLY",
  "SEMESTER",
  "ANNUAL",
  "ONE_TIME",
];

export function PaymentsPage() {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const { mosqueId: workspaceMosqueId } = useWorkspace();
  const isSuperAdmin = user ? normalizeApiRole(user.role) === "SUPER_ADMIN" : false;
  const [selectedMosqueId, setSelectedMosqueId] = useState<string>("");
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<PaymentResponse | null>(null);
  const { params, setPage } = usePagination();

  const activeMosqueId = isSuperAdmin
    ? selectedMosqueId || undefined
    : workspaceMosqueId ?? undefined;

  const listAll = isSuperAdmin && !activeMosqueId;
  const listMosqueId = listAll
    ? undefined
    : activeMosqueId ?? workspaceMosqueId ?? undefined;

  const { data: mosquesPage } = useMosques({ page: 0, size: 100 });
  const { data: allPayments, isLoading: allLoading } = usePayments(params);
  const { data: mosquePayments, isLoading: mosqueLoading } = useMosquePayments(
    listMosqueId,
    params,
  );

  const data = listAll ? allPayments : mosquePayments;
  const isLoading = listAll ? allLoading : mosqueLoading;

  const columns = useMemo(
    () => [
      {
        id: "student",
        header: t("payments.student"),
        cell: (row: PaymentResponse) =>
          row.studentName ?? formatShortId(row.studentId),
      },
      {
        id: "amount",
        header: t("payments.amount"),
        cell: (row: PaymentResponse) => row.amount,
      },
      {
        id: "dueDate",
        header: t("payments.dueDate"),
        cell: (row: PaymentResponse) => row.dueDate,
      },
      {
        id: "status",
        header: t("payments.status"),
        cell: (row: PaymentResponse) => (
          <Badge variant={row.status === "PAID" ? "secondary" : "default"}>
            {row.status}
          </Badge>
        ),
      },
      {
        id: "actions",
        header: "",
        className: "w-24 text-right",
        cell: (row: PaymentResponse) => (
          <Button variant="ghost" size="sm" onClick={() => setEditing(row)}>
            {t("actions.edit")}
          </Button>
        ),
      },
    ],
    [t],
  );

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={t("payments.title")}
        description={t("payments.description")}
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <PlusIcon data-icon="inline-start" />
            {t("payments.create")}
          </Button>
        }
      />

      {isSuperAdmin ? (
        <Field className="max-w-sm">
          <FieldLabel>{t("payments.mosque")}</FieldLabel>
          <Select
            value={selectedMosqueId || "__all__"}
            onValueChange={(value) => {
              setSelectedMosqueId(value === "__all__" ? "" : value);
              setPage(0);
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder={t("payments.mosquePlaceholder")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__all__">{t("payments.allMosques")}</SelectItem>
              {mosquesPage?.content.map((mosque) => (
                <SelectItem key={mosque.id} value={mosque.id}>
                  {mosque.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </Field>
      ) : null}

      <DataTable
        columns={columns}
        data={data}
        isLoading={isLoading}
        emptyMessage={t("payments.empty")}
        onPageChange={setPage}
      />

      <PaymentCreateDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        defaultMosqueId={activeMosqueId ?? workspaceMosqueId ?? ""}
      />

      {editing ? (
        <PaymentEditDialog
          payment={editing}
          open={!!editing}
          onOpenChange={(open) => {
            if (!open) setEditing(null);
          }}
        />
      ) : null}
    </div>
  );
}

function PaymentCreateDialog({
  open,
  onOpenChange,
  defaultMosqueId,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defaultMosqueId: string;
}) {
  const { t } = useTranslation("app");
  const createPayment = useCreatePayment();
  const { data: studentsPage } = useStudents({ page: 0, size: 100 });
  const { data: circlesPage } = useCircles({ page: 0, size: 100 });
  const { data: mosquesPage } = useMosques({ page: 0, size: 100 });

  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    watch,
    formState: { errors },
  } = useForm<PaymentCreateBody>({
    resolver: zodResolver(paymentCreateSchema),
    defaultValues: {
      studentId: "",
      circleId: "",
      mosqueId: defaultMosqueId,
      amount: "",
      dueDate: "",
      status: "PENDING",
      method: "CASH",
      cycle: "MONTHLY",
    },
  });

  const mosqueId = watch("mosqueId");

  const filteredStudents =
    studentsPage?.content.filter((s) => !mosqueId || s.mosqueId === mosqueId) ??
    [];
  const filteredCircles =
    circlesPage?.content.filter((c) => !mosqueId || c.mosqueId === mosqueId) ??
    [];

  async function onSubmit(values: PaymentCreateBody) {
    try {
      await createPayment.mutateAsync(values);
      toast.success(t("payments.createSuccess"));
      reset({ mosqueId: defaultMosqueId });
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
      title={t("payments.create")}
      submitLabel={t("actions.save")}
      isPending={createPayment.isPending}
      onSubmit={() => void handleSubmit(onSubmit)()}
    >
      <FieldGroup>
        <Field data-invalid={!!errors.mosqueId}>
          <FieldLabel>{t("payments.mosque")}</FieldLabel>
          <Controller
            control={control}
            name="mosqueId"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder={t("payments.mosquePlaceholder")} />
                </SelectTrigger>
                <SelectContent>
                  {mosquesPage?.content.map((mosque) => (
                    <SelectItem key={mosque.id} value={mosque.id}>
                      {mosque.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field data-invalid={!!errors.studentId}>
          <FieldLabel>{t("payments.student")}</FieldLabel>
          <Controller
            control={control}
            name="studentId"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder={t("payments.studentPlaceholder")} />
                </SelectTrigger>
                <SelectContent>
                  {filteredStudents.map((student) => (
                    <SelectItem key={student.id} value={student.id}>
                      {student.fullName ?? formatShortId(student.id)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field data-invalid={!!errors.circleId}>
          <FieldLabel>{t("payments.circle")}</FieldLabel>
          <Controller
            control={control}
            name="circleId"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder={t("payments.circlePlaceholder")} />
                </SelectTrigger>
                <SelectContent>
                  {filteredCircles.map((circle) => (
                    <SelectItem key={circle.id} value={circle.id}>
                      {circle.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field data-invalid={!!errors.amount}>
          <FieldLabel>{t("payments.amount")}</FieldLabel>
          <Input {...register("amount")} />
        </Field>
        <Field data-invalid={!!errors.dueDate}>
          <FieldLabel>{t("payments.dueDate")}</FieldLabel>
          <Input type="date" {...register("dueDate")} />
        </Field>
        <Field>
          <FieldLabel>{t("payments.status")}</FieldLabel>
          <Controller
            control={control}
            name="status"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_STATUSES.map((status) => (
                    <SelectItem key={status} value={status}>
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel>{t("payments.method")}</FieldLabel>
          <Controller
            control={control}
            name="method"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_METHODS.map((method) => (
                    <SelectItem key={method} value={method}>
                      {method}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel>{t("payments.cycle")}</FieldLabel>
          <Controller
            control={control}
            name="cycle"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_CYCLES.map((cycle) => (
                    <SelectItem key={cycle} value={cycle}>
                      {cycle}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel>{t("payments.notes")}</FieldLabel>
          <Textarea rows={2} {...register("notes")} />
        </Field>
      </FieldGroup>
    </EntityFormDialog>
  );
}

function PaymentEditDialog({
  payment,
  open,
  onOpenChange,
}: {
  payment: PaymentResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation("app");
  const updatePayment = useUpdatePayment();

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<PaymentUpdateBody>({
    resolver: zodResolver(paymentUpdateSchema),
    defaultValues: {
      amountPaid: payment.amountPaid ?? "",
      discount: payment.discount ?? "",
      status: payment.status,
      method: payment.method ?? "CASH",
      dueDate: payment.dueDate,
      paidDate: payment.paidDate ?? "",
      notes: payment.notes ?? "",
    },
  });

  async function onSubmit(values: PaymentUpdateBody) {
    try {
      await updatePayment.mutateAsync({ id: payment.id, body: values });
      toast.success(t("payments.updateSuccess"));
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
      title={t("payments.edit")}
      submitLabel={t("actions.save")}
      isPending={updatePayment.isPending}
      onSubmit={() => void handleSubmit(onSubmit)()}
    >
      <FieldGroup>
        <Field data-invalid={!!errors.amountPaid}>
          <FieldLabel>{t("payments.amountPaid")}</FieldLabel>
          <Input {...register("amountPaid")} />
        </Field>
        <Field data-invalid={!!errors.discount}>
          <FieldLabel>{t("payments.discount")}</FieldLabel>
          <Input {...register("discount")} />
        </Field>
        <Field>
          <FieldLabel>{t("payments.status")}</FieldLabel>
          <Controller
            control={control}
            name="status"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_STATUSES.map((status) => (
                    <SelectItem key={status} value={status}>
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field>
          <FieldLabel>{t("payments.method")}</FieldLabel>
          <Controller
            control={control}
            name="method"
            render={({ field }) => (
              <Select value={field.value ?? ""} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PAYMENT_METHODS.map((method) => (
                    <SelectItem key={method} value={method}>
                      {method}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field data-invalid={!!errors.dueDate}>
          <FieldLabel>{t("payments.dueDate")}</FieldLabel>
          <Input type="date" {...register("dueDate")} />
        </Field>
        <Field>
          <FieldLabel>{t("payments.paidDate")}</FieldLabel>
          <Input type="date" {...register("paidDate")} />
        </Field>
        <Field>
          <FieldLabel>{t("payments.notes")}</FieldLabel>
          <Textarea rows={2} {...register("notes")} />
        </Field>
      </FieldGroup>
    </EntityFormDialog>
  );
}
