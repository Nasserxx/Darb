import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { SuperAdminAuditReasonDialog } from "@/components/shared/super-admin-audit-reason-dialog.tsx";
import { UserSearchSelect } from "@/components/shared/user-search-select.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Switch } from "@/components/ui/switch.tsx";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { hasRole } from "@/lib/navigation/role-permissions.ts";
import type { AdminPermission } from "@/lib/types/api.ts";

import {
  mosqueAdminCreateSchema,
  toMosqueAdminCreateRequestBody,
  type MosqueAdminCreateFormValues,
} from "../schemas/mosque-admin-create.schema.ts";
import {
  mosqueAdminUpdateSchema,
  toMosqueAdminUpdateRequestBody,
  type MosqueAdminUpdateFormValues,
} from "../schemas/mosque-admin-update.schema.ts";
import { useCreateMosqueAdmin, useUpdateMosqueAdmin } from "../hooks/use-mosque-admins.ts";
import type { MosqueAdminResponse } from "../types/index.ts";

const PERMISSIONS: AdminPermission[] = [
  "FULL_ACCESS",
  "MANAGE_TEACHERS",
  "MANAGE_STUDENTS",
  "MANAGE_CIRCLES",
  "MANAGE_PAYMENTS",
  "VIEW_REPORTS",
];

type MosqueAdminFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mosqueAdmin?: MosqueAdminResponse | null;
};

export function MosqueAdminFormDialog({
  open,
  onOpenChange,
  mosqueAdmin,
}: MosqueAdminFormDialogProps) {
  const { t } = useTranslation("app");
  const isEdit = Boolean(mosqueAdmin);
  const createAdmin = useCreateMosqueAdmin();
  const updateAdmin = useUpdateMosqueAdmin();
  const isPending = createAdmin.isPending || updateAdmin.isPending;
  const { user } = useAuth();
  const isSuperAdmin = hasRole(user?.role, ["SUPER_ADMIN"]);
  const { data: mosquesPage } = useMosques({ page: 0, size: 100 });
  const [auditOpen, setAuditOpen] = useState(false);
  const [pendingCreate, setPendingCreate] =
    useState<MosqueAdminCreateFormValues | null>(null);
  const [pendingUpdate, setPendingUpdate] =
    useState<MosqueAdminUpdateFormValues | null>(null);

  const createForm = useForm<MosqueAdminCreateFormValues>({
    resolver: zodResolver(mosqueAdminCreateSchema),
    defaultValues: {
      userId: "",
      mosqueId: "",
      permission: "FULL_ACCESS",
      isPrimaryAdmin: false,
    },
  });

  const editForm = useForm<MosqueAdminUpdateFormValues>({
    resolver: zodResolver(mosqueAdminUpdateSchema),
    defaultValues: {
      permission: "FULL_ACCESS",
      isPrimaryAdmin: false,
    },
  });

  useEffect(() => {
    if (!open) {
      createForm.reset();
      editForm.reset();
      return;
    }
    if (mosqueAdmin) {
      editForm.reset({
        permission: mosqueAdmin.permission,
        isPrimaryAdmin: mosqueAdmin.isPrimaryAdmin,
      });
    } else {
      createForm.reset();
    }
  }, [open, mosqueAdmin, createForm, editForm]);

  async function runCreate(
    values: MosqueAdminCreateFormValues,
    auditReason?: string,
  ) {
    try {
      await createAdmin.mutateAsync({
        body: toMosqueAdminCreateRequestBody(values, user?.userId ?? ""),
        auditReason,
      });
      toast.success(t("mosqueAdmins.createSuccess"));
      setAuditOpen(false);
      setPendingCreate(null);
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, createForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function runEdit(
    values: MosqueAdminUpdateFormValues,
    auditReason?: string,
  ) {
    if (!mosqueAdmin) return;
    try {
      await updateAdmin.mutateAsync({
        id: mosqueAdmin.id,
        body: toMosqueAdminUpdateRequestBody(values),
        auditReason,
      });
      toast.success(t("mosqueAdmins.updateSuccess"));
      setAuditOpen(false);
      setPendingUpdate(null);
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, editForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleCreateSubmit(values: MosqueAdminCreateFormValues) {
    if (isSuperAdmin) {
      setPendingCreate(values);
      setPendingUpdate(null);
      setAuditOpen(true);
      return;
    }
    await runCreate(values);
  }

  async function handleEditSubmit(values: MosqueAdminUpdateFormValues) {
    if (!mosqueAdmin) return;
    if (isSuperAdmin) {
      setPendingUpdate(values);
      setPendingCreate(null);
      setAuditOpen(true);
      return;
    }
    await runEdit(values);
  }

  const createErrors = createForm.formState.errors;
  const editErrors = editForm.formState.errors;

  return (
    <>
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("mosqueAdmins.edit") : t("mosqueAdmins.create")}
      submitLabel={isEdit ? t("actions.save") : t("actions.create")}
      isPending={isPending}
      onSubmit={() => {
        if (isEdit) {
          void editForm.handleSubmit(handleEditSubmit)();
        } else {
          void createForm.handleSubmit(handleCreateSubmit)();
        }
      }}
    >
      {isEdit ? (
        <FieldGroup>
          <Field data-invalid={!!editErrors.permission}>
            <FieldLabel>{t("mosqueAdmins.permission")}</FieldLabel>
            <Controller
              name="permission"
              control={editForm.control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger aria-invalid={!!editErrors.permission}>
                    <SelectValue placeholder={t("mosqueAdmins.selectPermission")} />
                  </SelectTrigger>
                  <SelectContent>
                    {PERMISSIONS.map((permission) => (
                      <SelectItem key={permission} value={permission}>
                        {t(`mosqueAdmins.permissions.${permission}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {editErrors.permission ? (
              <FieldDescription className="text-destructive">
                {editErrors.permission.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field className="flex-row items-center justify-between gap-4">
            <FieldLabel htmlFor="mosque-admin-primary">
              {t("mosqueAdmins.isPrimaryAdmin")}
            </FieldLabel>
            <Controller
              name="isPrimaryAdmin"
              control={editForm.control}
              render={({ field }) => (
                <Switch
                  id="mosque-admin-primary"
                  checked={field.value ?? false}
                  onCheckedChange={field.onChange}
                />
              )}
            />
          </Field>
        </FieldGroup>
      ) : (
        <FieldGroup>
          <Field data-invalid={!!createErrors.userId}>
            <FieldLabel>{t("mosqueAdmins.userId")}</FieldLabel>
            <Controller
              name="userId"
              control={createForm.control}
              render={({ field }) => (
                <UserSearchSelect
                  value={field.value}
                  onValueChange={field.onChange}
                />
              )}
            />
            {createErrors.userId ? (
              <FieldDescription className="text-destructive">
                {createErrors.userId.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.mosqueId}>
            <FieldLabel>{t("mosqueAdmins.mosqueId")}</FieldLabel>
            <Controller
              name="mosqueId"
              control={createForm.control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger aria-invalid={!!createErrors.mosqueId}>
                    <SelectValue placeholder={t("mosqueAdmins.selectMosque")} />
                  </SelectTrigger>
                  <SelectContent>
                    {(mosquesPage?.content ?? []).map((mosque) => (
                      <SelectItem key={mosque.id} value={mosque.id}>
                        {mosque.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {createErrors.mosqueId ? (
              <FieldDescription className="text-destructive">
                {createErrors.mosqueId.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!createErrors.permission}>
            <FieldLabel>{t("mosqueAdmins.permission")}</FieldLabel>
            <Controller
              name="permission"
              control={createForm.control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger aria-invalid={!!createErrors.permission}>
                    <SelectValue placeholder={t("mosqueAdmins.selectPermission")} />
                  </SelectTrigger>
                  <SelectContent>
                    {PERMISSIONS.map((permission) => (
                      <SelectItem key={permission} value={permission}>
                        {t(`mosqueAdmins.permissions.${permission}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            {createErrors.permission ? (
              <FieldDescription className="text-destructive">
                {createErrors.permission.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field className="flex-row items-center justify-between gap-4">
            <FieldLabel htmlFor="mosque-admin-primary-create">
              {t("mosqueAdmins.isPrimaryAdmin")}
            </FieldLabel>
            <Controller
              name="isPrimaryAdmin"
              control={createForm.control}
              render={({ field }) => (
                <Switch
                  id="mosque-admin-primary-create"
                  checked={field.value ?? false}
                  onCheckedChange={field.onChange}
                />
              )}
            />
          </Field>
        </FieldGroup>
      )}
    </EntityFormDialog>

      <SuperAdminAuditReasonDialog
        open={auditOpen}
        onOpenChange={(open) => {
          setAuditOpen(open);
          if (!open) {
            setPendingCreate(null);
            setPendingUpdate(null);
          }
        }}
        title={t("superAdmin.auditReason.title")}
        confirmLabel={t("superAdmin.auditReason.confirm")}
        isPending={isPending}
        onConfirm={(reason) => {
          if (pendingCreate) return runCreate(pendingCreate, reason);
          if (pendingUpdate) return runEdit(pendingUpdate, reason);
        }}
      />
    </>
  );
}
