import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { SuperAdminAuditReasonDialog } from "@/components/shared/super-admin-audit-reason-dialog.tsx";
import { UserSearchSelect } from "@/components/shared/user-search-select.tsx";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import {
  useCreateParentStudent,
  useUpdateParentStudent,
} from "@/features/parent-students/hooks/use-parent-students.ts";
import {
  PARENT_RELATIONSHIPS,
  parentStudentFormSchema,
  type ParentStudentFormValues,
} from "@/features/parent-students/schemas/parent-student.schema.ts";
import type { ParentStudentResponse } from "@/features/parent-students/types/index.ts";
import { useStudents } from "@/features/students/hooks/use-students.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import {
  AddressFields,
  emptyAddressValues,
  optionalAddressSchema,
  toUserUpdateRequestBody,
  useUpdateUser,
  useUser,
  type AddressFormValues,
} from "@/features/users/index.ts";
import { hasAddressValues } from "@/lib/address.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { hasRole } from "@/lib/navigation/role-permissions.ts";

type ParentStudentFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  link?: ParentStudentResponse | null;
};

const emptyLinkValues: ParentStudentFormValues = {
  parentUserId: "",
  studentId: "",
  relationship: undefined,
  isPrimary: false,
  receivesNotifications: true,
};

export function ParentStudentFormDialog({
  open,
  onOpenChange,
  link,
}: ParentStudentFormDialogProps) {
  const { t } = useTranslation("app");
  const { user } = useAuth();
  const isSuperAdmin = hasRole(user?.role, ["SUPER_ADMIN"]);
  const { mosqueId } = useWorkspace();
  const isStudent = user?.role === "student";
  const isEdit = Boolean(link);
  const createMutation = useCreateParentStudent();
  const updateMutation = useUpdateParentStudent();
  const updateUser = useUpdateUser();
  const isPending =
    createMutation.isPending ||
    updateMutation.isPending ||
    updateUser.isPending;
  const { data: studentsPage } = useStudents({ page: 0, size: 500 });
  const [auditOpen, setAuditOpen] = useState(false);
  const [pendingValues, setPendingValues] =
    useState<ParentStudentFormValues | null>(null);

  const form = useForm<ParentStudentFormValues>({
    resolver: zodResolver(parentStudentFormSchema),
    defaultValues: emptyLinkValues,
  });
  const { errors } = form.formState;

  const addressForm = useForm<AddressFormValues>({
    resolver: zodResolver(optionalAddressSchema),
    defaultValues: emptyAddressValues,
  });

  const watchedParentUserId = form.watch("parentUserId");
  const watchedStudentId = form.watch("studentId");
  const selectedStudent = (studentsPage?.content ?? []).find(
    (row) => row.id === watchedStudentId,
  );
  const occupancyMosqueId =
    !isEdit
      ? (selectedStudent?.mosqueId ?? mosqueId ?? undefined)
      : undefined;
  const canEditAddress =
    isEdit && Boolean(link) && watchedParentUserId === link?.parentUserId;
  const linkedUserId = canEditAddress ? (link?.parentUserId ?? "") : "";
  const { data: linkedUser } = useUser(linkedUserId, {
    enabled: open && Boolean(linkedUserId),
  });

  useEffect(() => {
    if (!open) {
      form.reset(emptyLinkValues);
      addressForm.reset(emptyAddressValues);
      return;
    }
    if (link) {
      form.reset({
        parentUserId: link.parentUserId,
        studentId: link.studentId,
        relationship: link.relationship ?? undefined,
        isPrimary: link.isPrimary,
        receivesNotifications: link.receivesNotifications,
      });
    } else {
      const ownStudentId =
        isStudent && studentsPage?.content[0]
          ? studentsPage.content[0].id
          : "";
      form.reset({ ...emptyLinkValues, studentId: ownStudentId });
    }
  }, [open, link, form, addressForm, isStudent, studentsPage]);

  useEffect(() => {
    if (!open || !canEditAddress) {
      if (!canEditAddress) addressForm.reset(emptyAddressValues);
      return;
    }
    if (linkedUser) {
      addressForm.reset({
        addressCountry: linkedUser.addressCountry ?? "",
        city: linkedUser.city ?? "",
        addressStreet: linkedUser.addressStreet ?? "",
        addressHouseNumber: linkedUser.addressHouseNumber ?? "",
        addressPostalCode: linkedUser.addressPostalCode ?? "",
        addressState: linkedUser.addressState ?? "",
      });
    } else {
      addressForm.reset(emptyAddressValues);
    }
  }, [open, canEditAddress, linkedUser, addressForm]);

  async function persistLink(
    values: ParentStudentFormValues,
    auditReason?: string,
  ) {
    if (isEdit && link) {
      const addressValid = canEditAddress
        ? await addressForm.trigger()
        : true;
      if (!addressValid) return;
      try {
        await updateMutation.mutateAsync({
          id: link.id,
          body: values,
          auditReason,
        });
        if (canEditAddress && values.parentUserId === link.parentUserId) {
          await updateUser.mutateAsync({
            id: link.parentUserId,
            body: toUserUpdateRequestBody(addressForm.getValues()),
          });
        }
        toast.success(t("parentStudents.updateSuccess"));
        setAuditOpen(false);
        setPendingValues(null);
        onOpenChange(false);
      } catch (error) {
        const result = toMutationError(error, t);
        if (result.fieldErrors) {
          applyFieldErrors(result.fieldErrors, form.setError, t);
        }
        toast.error(result.message);
      }
      return;
    }

    try {
      await createMutation.mutateAsync({ body: values, auditReason });
      toast.success(t("membership.invite.sent"));
      setAuditOpen(false);
      setPendingValues(null);
      onOpenChange(false);
    } catch (error) {
      const result = toMutationError(error, t);
      if (result.fieldErrors) {
        applyFieldErrors(result.fieldErrors, form.setError, t);
      }
      toast.error(result.message);
    }
  }

  async function handleSubmit() {
    const valid = await form.trigger();
    if (!valid) return;

    const values = form.getValues();

    if (isSuperAdmin) {
      setPendingValues(values);
      setAuditOpen(true);
      return;
    }

    await persistLink(values);
  }

  const addressDefaultOpen = linkedUser
    ? hasAddressValues(linkedUser)
    : false;

  return (
    <>
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("parentStudents.edit") : t("parentStudents.create")}
      submitLabel={t("actions.save")}
      onSubmit={() => void handleSubmit()}
      isPending={isPending}
    >
      <FieldGroup>
        <Field data-invalid={!!errors.parentUserId}>
          <FieldLabel>{t("parentStudents.parentUserId")}</FieldLabel>
          <Controller
            name="parentUserId"
            control={form.control}
            render={({ field }) => (
              <UserSearchSelect
                value={field.value}
                onValueChange={field.onChange}
                occupancyMosqueId={occupancyMosqueId}
                occupancyRole="PARENT"
              />
            )}
          />
          {errors.parentUserId ? (
            <p className="text-sm text-destructive">
              {errors.parentUserId.message}
            </p>
          ) : null}
        </Field>
        <Field data-invalid={!!errors.studentId}>
          <FieldLabel>{t("parentStudents.studentId")}</FieldLabel>
          {isStudent ? (
            <p className="text-sm">
              {studentsPage?.content[0]?.fullName ??
                formatShortId(form.watch("studentId") || "—")}
            </p>
          ) : (
            <Controller
              name="studentId"
              control={form.control}
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger aria-invalid={!!errors.studentId}>
                    <SelectValue placeholder={t("payments.studentPlaceholder")} />
                  </SelectTrigger>
                  <SelectContent>
                    {(studentsPage?.content ?? []).map((student) => (
                      <SelectItem key={student.id} value={student.id}>
                        {student.fullName ?? formatShortId(student.id)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
          )}
          {errors.studentId ? (
            <p className="text-sm text-destructive">
              {errors.studentId.message}
            </p>
          ) : null}
        </Field>
        <Field>
          <FieldLabel>{t("parentStudents.relationship")}</FieldLabel>
          <Controller
            name="relationship"
            control={form.control}
            render={({ field }) => (
              <Select
                value={field.value}
                onValueChange={field.onChange}
              >
                <SelectTrigger>
                  <SelectValue
                    placeholder={t("parentStudents.relationshipPlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  {PARENT_RELATIONSHIPS.map((rel) => (
                    <SelectItem key={rel} value={rel}>
                      {t(`enums.parentRelationship.${rel}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field className="flex flex-row items-center gap-3">
          <Controller
            name="isPrimary"
            control={form.control}
            render={({ field }) => (
              <Checkbox
                id="isPrimary"
                checked={field.value ?? false}
                onCheckedChange={(checked) => field.onChange(checked === true)}
              />
            )}
          />
          <FieldLabel htmlFor="isPrimary" className="font-normal">
            {t("parentStudents.isPrimary")}
          </FieldLabel>
        </Field>
        <Field className="flex flex-row items-center gap-3">
          <Controller
            name="receivesNotifications"
            control={form.control}
            render={({ field }) => (
              <Checkbox
                id="receivesNotifications"
                checked={field.value ?? false}
                onCheckedChange={(checked) => field.onChange(checked === true)}
              />
            )}
          />
          <FieldLabel htmlFor="receivesNotifications" className="font-normal">
            {t("parentStudents.receivesNotifications")}
          </FieldLabel>
        </Field>
        {canEditAddress ? (
          <div className="flex flex-col gap-2">
            <p className="text-sm text-muted-foreground">
              {t("profile.editingAddress", {
                name: link?.parentName ?? linkedUser?.fullName ?? "…",
              })}
            </p>
            <AddressFields
              register={addressForm.register}
              errors={addressForm.formState.errors}
              watch={addressForm.watch}
              setValue={addressForm.setValue}
              idPrefix="parent-user"
              defaultOpen={addressDefaultOpen}
            />
          </div>
        ) : null}
      </FieldGroup>
    </EntityFormDialog>

      <SuperAdminAuditReasonDialog
        open={auditOpen}
        onOpenChange={(open) => {
          setAuditOpen(open);
          if (!open) setPendingValues(null);
        }}
        title={t("superAdmin.auditReason.title")}
        confirmLabel={t("superAdmin.auditReason.confirm")}
        isPending={isPending}
        onConfirm={(reason) => {
          if (!pendingValues) return;
          return persistLink(pendingValues, reason);
        }}
      />
    </>
  );
}
