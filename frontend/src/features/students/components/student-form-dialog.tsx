import { zodResolver } from "@hookform/resolvers/zod";
import { CopyIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { UserSearchSelect } from "@/components/shared/user-search-select.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { Textarea } from "@/components/ui/textarea";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import {
  useCreateStudent,
  useProvisionStudent,
  useUpdateStudent,
} from "@/features/students/hooks/use-students.ts";
import {
  studentInviteSchema,
  studentProvisionSchema,
  studentUpdateSchema,
  toStudentProvisionRequest,
  type StudentInviteFormValues,
  type StudentProvisionFormValues,
  type StudentUpdateFormValues,
} from "@/features/students/schemas/student.schema.ts";
import type { StudentResponse } from "@/features/students/types/index.ts";
import {
  AddressFields,
  emptyAddressValues,
  optionalAddressSchema,
  toUserUpdateRequestBody,
  useUpdateUser,
  useUser,
  type AddressFormValues,
} from "@/features/users/index.ts";
import { useScopedMosqueForAdmin } from "@/features/workspace/hooks/use-scoped-mosque-for-admin.ts";
import { hasAddressValues } from "@/lib/address.ts";
import {
  applyFieldErrors,
  toMutationError,
} from "@/lib/errors/map-api-error.ts";

type StudentFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  student?: StudentResponse | null;
};

type CreateMode = "provision" | "invite";

type CredentialsPanel = { email: string; password: string };

export function StudentFormDialog({
  open,
  onOpenChange,
  student,
}: StudentFormDialogProps) {
  const { t } = useTranslation("app");
  const { mosqueId, showMosqueField } = useScopedMosqueForAdmin();
  const isEdit = Boolean(student);
  const [createMode, setCreateMode] = useState<CreateMode>("provision");
  const [credentials, setCredentials] = useState<CredentialsPanel | null>(null);
  const createMutation = useCreateStudent();
  const provisionMutation = useProvisionStudent();
  const updateMutation = useUpdateStudent();
  const updateUser = useUpdateUser();
  const isPending =
    createMutation.isPending ||
    provisionMutation.isPending ||
    updateMutation.isPending ||
    updateUser.isPending;
  // ponytail: useMosques has no enabled; skip fetch later if list cost matters
  const { data: mosquesPage } = useMosques(
    { page: 0, size: 100 },
    { enabled: showMosqueField },
  );
  const linkedUserId = student?.userId ?? "";
  const { data: linkedUser } = useUser(linkedUserId, {
    enabled: open && isEdit && Boolean(linkedUserId),
  });

  const inviteForm = useForm<StudentInviteFormValues>({
    resolver: zodResolver(studentInviteSchema),
    defaultValues: {
      userId: "",
      mosqueId: mosqueId ?? "",
      medicalNotes: "",
      memorizedJuz: undefined,
    },
  });

  const provisionForm = useForm<StudentProvisionFormValues>({
    resolver: zodResolver(studentProvisionSchema),
    defaultValues: {
      mosqueId: mosqueId ?? "",
      fullName: "",
      email: "",
      password: "",
      confirmPassword: "",
      phone: "",
      gender: undefined,
      dateOfBirth: "",
      medicalNotes: "",
      memorizedJuz: undefined,
      ...emptyAddressValues,
    },
  });

  const updateForm = useForm<StudentUpdateFormValues>({
    resolver: zodResolver(studentUpdateSchema),
    defaultValues: {
      medicalNotes: "",
      memorizedJuz: undefined,
      totalAbsences: undefined,
      totalLateArrivals: undefined,
    },
  });

  const addressForm = useForm<AddressFormValues>({
    resolver: zodResolver(optionalAddressSchema),
    defaultValues: emptyAddressValues,
  });

  const inviteMosqueId = inviteForm.watch("mosqueId");

  useEffect(() => {
    if (!open) return;
    setCreateMode("provision");
    setCredentials(null);
    if (student) {
      updateForm.reset({
        medicalNotes: student.medicalNotes ?? "",
        memorizedJuz: student.memorizedJuz ?? undefined,
        totalAbsences: student.totalAbsences,
        totalLateArrivals: student.totalLateArrivals,
      });
    } else {
      inviteForm.reset({
        userId: "",
        mosqueId: mosqueId ?? "",
        medicalNotes: "",
        memorizedJuz: undefined,
      });
      provisionForm.reset({
        mosqueId: mosqueId ?? "",
        fullName: "",
        email: "",
        password: "",
        confirmPassword: "",
        phone: "",
        gender: undefined,
        dateOfBirth: "",
        medicalNotes: "",
        memorizedJuz: undefined,
        ...emptyAddressValues,
      });
    }
  }, [open, student, mosqueId, inviteForm, provisionForm, updateForm]);

  useEffect(() => {
    if (!open || !isEdit) return;
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
  }, [open, isEdit, linkedUser, addressForm]);

  async function handleSubmit() {
    if (credentials) {
      onOpenChange(false);
      return;
    }

    if (isEdit && student) {
      const valid = await updateForm.trigger();
      const addressValid = await addressForm.trigger();
      if (!valid || !addressValid) return;
      const values = updateForm.getValues();
      try {
        await updateMutation.mutateAsync({ id: student.id, body: values });
        if (student.userId) {
          await updateUser.mutateAsync({
            id: student.userId,
            body: toUserUpdateRequestBody(addressForm.getValues()),
          });
        }
        toast.success(t("students.updateSuccess"));
        onOpenChange(false);
      } catch (error) {
        const result = toMutationError(error, t);
        if (result.fieldErrors) {
          applyFieldErrors(result.fieldErrors, updateForm.setError, t);
        }
        toast.error(result.message);
      }
      return;
    }

    if (createMode === "invite") {
      const valid = await inviteForm.trigger();
      if (!valid) return;
      try {
        await createMutation.mutateAsync(inviteForm.getValues());
        toast.success(t("membership.invite.sent"));
        onOpenChange(false);
      } catch (error) {
        const result = toMutationError(error, t);
        if (result.fieldErrors) {
          applyFieldErrors(result.fieldErrors, inviteForm.setError, t);
        }
        toast.error(result.message);
      }
      return;
    }

    const valid = await provisionForm.trigger();
    if (!valid) return;
    const values = provisionForm.getValues();
    try {
      await provisionMutation.mutateAsync(toStudentProvisionRequest(values));
      setCredentials({ email: values.email, password: values.password });
      toast.success(t("membership.provision.success"));
    } catch (error) {
      const result = toMutationError(error, t);
      if (result.fieldErrors) {
        applyFieldErrors(result.fieldErrors, provisionForm.setError, t);
      }
      toast.error(result.message);
    }
  }

  async function copyField(label: string, value: string) {
    try {
      await navigator.clipboard.writeText(value);
      toast.success(t("membership.provision.copied", { field: label }));
    } catch {
      toast.error(t("membership.provision.copyFailed"));
    }
  }

  const errors = isEdit
    ? updateForm.formState.errors
    : inviteForm.formState.errors;
  const provisionErrors = provisionForm.formState.errors;
  const addressDefaultOpen = linkedUser
    ? hasAddressValues(linkedUser)
    : false;

  const submitLabel = credentials
    ? t("membership.provision.done")
    : t("actions.save");

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("students.edit") : t("students.create")}
      submitLabel={submitLabel}
      onSubmit={() => void handleSubmit()}
      isPending={isPending}
    >
      {credentials ? (
        <FieldGroup>
          <p className="text-sm text-muted-foreground">
            {t("membership.provision.credentialsHint")}
          </p>
          <Field>
            <FieldLabel>{t("profile.email")}</FieldLabel>
            <div className="flex gap-2">
              <Input readOnly value={credentials.email} />
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() =>
                  void copyField(t("profile.email"), credentials.email)
                }
              >
                <CopyIcon />
              </Button>
            </div>
          </Field>
          <Field>
            <FieldLabel>{t("membership.provision.password")}</FieldLabel>
            <div className="flex gap-2">
              <Input readOnly value={credentials.password} />
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={() =>
                  void copyField(
                    t("membership.provision.password"),
                    credentials.password,
                  )
                }
              >
                <CopyIcon />
              </Button>
            </div>
          </Field>
        </FieldGroup>
      ) : (
        <FieldGroup>
          {!isEdit ? (
            <Tabs
              value={createMode}
              onValueChange={(value) =>
                setCreateMode(value as CreateMode)
              }
              className="flex flex-col gap-4"
            >
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="provision">
                  {t("membership.provision.tabCreate")}
                </TabsTrigger>
                <TabsTrigger value="invite">
                  {t("membership.invite.tabInvite")}
                </TabsTrigger>
              </TabsList>

              <TabsContent value="provision" className="flex flex-col gap-4">
                {showMosqueField ? (
                  <Field data-invalid={!!provisionErrors.mosqueId}>
                    <FieldLabel>{t("students.mosqueId")}</FieldLabel>
                    <Controller
                      name="mosqueId"
                      control={provisionForm.control}
                      render={({ field }) => (
                        <Select value={field.value} onValueChange={field.onChange}>
                          <SelectTrigger aria-invalid={!!provisionErrors.mosqueId}>
                            <SelectValue placeholder={t("teachers.selectMosque")} />
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
                  </Field>
                ) : null}
                <Field data-invalid={!!provisionErrors.fullName}>
                  <FieldLabel htmlFor="student-provision-name">
                    {t("profile.fullName")}
                  </FieldLabel>
                  <Input
                    id="student-provision-name"
                    {...provisionForm.register("fullName")}
                    aria-invalid={!!provisionErrors.fullName}
                  />
                </Field>
                <Field data-invalid={!!provisionErrors.email}>
                  <FieldLabel htmlFor="student-provision-email">
                    {t("profile.email")}
                  </FieldLabel>
                  <Input
                    id="student-provision-email"
                    type="email"
                    autoComplete="off"
                    {...provisionForm.register("email")}
                    aria-invalid={!!provisionErrors.email}
                  />
                </Field>
                <Field data-invalid={!!provisionErrors.password}>
                  <FieldLabel htmlFor="student-provision-password">
                    {t("membership.provision.password")}
                  </FieldLabel>
                  <Input
                    id="student-provision-password"
                    type="password"
                    autoComplete="new-password"
                    {...provisionForm.register("password")}
                    aria-invalid={!!provisionErrors.password}
                  />
                </Field>
                <Field data-invalid={!!provisionErrors.confirmPassword}>
                  <FieldLabel htmlFor="student-provision-confirm">
                    {t("membership.provision.confirmPassword")}
                  </FieldLabel>
                  <Input
                    id="student-provision-confirm"
                    type="password"
                    autoComplete="new-password"
                    {...provisionForm.register("confirmPassword")}
                    aria-invalid={!!provisionErrors.confirmPassword}
                  />
                </Field>
                <Field data-invalid={!!provisionErrors.memorizedJuz}>
                  <FieldLabel htmlFor="student-provision-juz">
                    {t("students.memorizedJuz")}
                  </FieldLabel>
                  <Input
                    id="student-provision-juz"
                    type="number"
                    min={0}
                    {...provisionForm.register("memorizedJuz", {
                      valueAsNumber: true,
                    })}
                  />
                </Field>
                <Field data-invalid={!!provisionErrors.medicalNotes}>
                  <FieldLabel htmlFor="student-provision-notes">
                    {t("students.medicalNotes")}
                  </FieldLabel>
                  <Textarea
                    id="student-provision-notes"
                    {...provisionForm.register("medicalNotes")}
                  />
                </Field>
                <AddressFields
                  register={provisionForm.register}
                  errors={provisionForm.formState.errors}
                  watch={provisionForm.watch}
                  setValue={provisionForm.setValue}
                  idPrefix="student-provision"
                />
              </TabsContent>

              <TabsContent value="invite" className="flex flex-col gap-4">
                <Field data-invalid={!!inviteForm.formState.errors.userId}>
                  <FieldLabel>{t("students.userId")}</FieldLabel>
                  <Controller
                    name="userId"
                    control={inviteForm.control}
                    render={({ field }) => (
                      <UserSearchSelect
                        value={field.value}
                        onValueChange={field.onChange}
                        occupancyMosqueId={
                          inviteMosqueId || mosqueId || undefined
                        }
                        occupancyRole="STUDENT"
                      />
                    )}
                  />
                  {inviteForm.formState.errors.userId ? (
                    <p className="text-sm text-destructive">
                      {inviteForm.formState.errors.userId.message}
                    </p>
                  ) : null}
                </Field>
                {showMosqueField ? (
                  <Field data-invalid={!!inviteForm.formState.errors.mosqueId}>
                    <FieldLabel>{t("students.mosqueId")}</FieldLabel>
                    <Controller
                      name="mosqueId"
                      control={inviteForm.control}
                      render={({ field }) => (
                        <Select value={field.value} onValueChange={field.onChange}>
                          <SelectTrigger
                            aria-invalid={!!inviteForm.formState.errors.mosqueId}
                          >
                            <SelectValue placeholder={t("teachers.selectMosque")} />
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
                  </Field>
                ) : null}
                <Field data-invalid={!!errors.memorizedJuz}>
                  <FieldLabel htmlFor="memorizedJuz">
                    {t("students.memorizedJuz")}
                  </FieldLabel>
                  <Input
                    id="memorizedJuz"
                    type="number"
                    min={0}
                    {...inviteForm.register("memorizedJuz", {
                      valueAsNumber: true,
                    })}
                  />
                </Field>
                <Field data-invalid={!!errors.medicalNotes}>
                  <FieldLabel htmlFor="medicalNotes">
                    {t("students.medicalNotes")}
                  </FieldLabel>
                  <Textarea
                    id="medicalNotes"
                    {...inviteForm.register("medicalNotes")}
                  />
                </Field>
              </TabsContent>
            </Tabs>
          ) : (
            <>
              <Field data-invalid={!!errors.memorizedJuz}>
                <FieldLabel htmlFor="memorizedJuz">
                  {t("students.memorizedJuz")}
                </FieldLabel>
                <Input
                  id="memorizedJuz"
                  type="number"
                  min={0}
                  {...updateForm.register("memorizedJuz", {
                    valueAsNumber: true,
                  })}
                  aria-invalid={!!updateForm.formState.errors.memorizedJuz}
                />
              </Field>
              <Field data-invalid={!!errors.medicalNotes}>
                <FieldLabel htmlFor="medicalNotes">
                  {t("students.medicalNotes")}
                </FieldLabel>
                <Textarea
                  id="medicalNotes"
                  {...updateForm.register("medicalNotes")}
                  aria-invalid={!!updateForm.formState.errors.medicalNotes}
                />
              </Field>
              <Field data-invalid={!!updateForm.formState.errors.totalAbsences}>
                <FieldLabel htmlFor="totalAbsences">
                  {t("students.totalAbsences")}
                </FieldLabel>
                <Input
                  id="totalAbsences"
                  type="number"
                  min={0}
                  {...updateForm.register("totalAbsences", {
                    valueAsNumber: true,
                  })}
                />
              </Field>
              <Field
                data-invalid={!!updateForm.formState.errors.totalLateArrivals}
              >
                <FieldLabel htmlFor="totalLateArrivals">
                  {t("students.totalLateArrivals")}
                </FieldLabel>
                <Input
                  id="totalLateArrivals"
                  type="number"
                  min={0}
                  {...updateForm.register("totalLateArrivals", {
                    valueAsNumber: true,
                  })}
                />
              </Field>
              {student?.userId ? (
                <div className="flex flex-col gap-2">
                  <p className="text-sm text-muted-foreground">
                    {t("profile.editingAddress", {
                      name: student.fullName ?? linkedUser?.fullName ?? "…",
                    })}
                  </p>
                  <AddressFields
                    register={addressForm.register}
                    errors={addressForm.formState.errors}
                    watch={addressForm.watch}
                    setValue={addressForm.setValue}
                    idPrefix="student-user"
                    defaultOpen={addressDefaultOpen}
                  />
                </div>
              ) : null}
            </>
          )}
        </FieldGroup>
      )}
    </EntityFormDialog>
  );
}
