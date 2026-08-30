import { zodResolver } from "@hookform/resolvers/zod";
import { CopyIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { UserSearchSelect } from "@/components/shared/user-search-select.tsx";
import { Button } from "@/components/ui/button.tsx";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Switch } from "@/components/ui/switch.tsx";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { useMosques } from "@/features/mosques/hooks/use-mosques.ts";
import { useScopedMosqueForAdmin } from "@/features/workspace/hooks/use-scoped-mosque-for-admin.ts";
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

import {
  teacherCreateSchema,
  teacherProvisionSchema,
  toTeacherCreateRequestBody,
  toTeacherProvisionRequest,
  type TeacherCreateFormValues,
  type TeacherProvisionFormValues,
} from "../schemas/teacher-create.schema.ts";
import {
  teacherUpdateSchema,
  toTeacherUpdateRequestBody,
  type TeacherUpdateFormValues,
} from "../schemas/teacher-update.schema.ts";
import {
  useCreateTeacher,
  useProvisionTeacher,
  useUpdateTeacher,
} from "../hooks/use-teachers.ts";
import type { TeacherResponse } from "../types/index.ts";

type TeacherFormDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  teacher?: TeacherResponse | null;
};

type CreateMode = "provision" | "invite";
type CredentialsPanel = { email: string; password: string };

export function TeacherFormDialog({ open, onOpenChange, teacher }: TeacherFormDialogProps) {
  const { t } = useTranslation("app");
  const { mosqueId, showMosqueField } = useScopedMosqueForAdmin();
  const isEdit = Boolean(teacher);
  const [createMode, setCreateMode] = useState<CreateMode>("provision");
  const [credentials, setCredentials] = useState<CredentialsPanel | null>(null);
  const createTeacher = useCreateTeacher();
  const provisionTeacher = useProvisionTeacher();
  const updateTeacher = useUpdateTeacher();
  const updateUser = useUpdateUser();
  const isPending =
    createTeacher.isPending ||
    provisionTeacher.isPending ||
    updateTeacher.isPending ||
    updateUser.isPending;
  // ponytail: useMosques has no enabled; skip fetch later if list cost matters
  const { data: mosquesPage } = useMosques(
    { page: 0, size: 100 },
    { enabled: showMosqueField },
  );
  const linkedUserId = teacher?.userId ?? "";
  const { data: linkedUser } = useUser(linkedUserId, {
    enabled: open && isEdit && Boolean(linkedUserId),
  });

  const createForm = useForm<TeacherCreateFormValues>({
    resolver: zodResolver(teacherCreateSchema),
    defaultValues: {
      userId: "",
      mosqueId: mosqueId ?? "",
      specialization: "",
      bio: "",
      yearsExperience: undefined,
      ijazahChain: "",
    },
  });

  const provisionForm = useForm<TeacherProvisionFormValues>({
    resolver: zodResolver(teacherProvisionSchema),
    defaultValues: {
      mosqueId: mosqueId ?? "",
      fullName: "",
      email: "",
      password: "",
      confirmPassword: "",
      phone: "",
      gender: undefined,
      dateOfBirth: "",
      specialization: "",
      bio: "",
      yearsExperience: undefined,
      ijazahChain: "",
      ...emptyAddressValues,
    },
  });

  const editForm = useForm<TeacherUpdateFormValues>({
    resolver: zodResolver(teacherUpdateSchema),
    defaultValues: {
      specialization: "",
      bio: "",
      yearsExperience: undefined,
      ijazahChain: "",
      isAvailable: true,
    },
  });

  const addressForm = useForm<AddressFormValues>({
    resolver: zodResolver(optionalAddressSchema),
    defaultValues: emptyAddressValues,
  });

  const inviteMosqueId = createForm.watch("mosqueId");

  useEffect(() => {
    if (!open) {
      createForm.reset();
      provisionForm.reset();
      editForm.reset();
      addressForm.reset(emptyAddressValues);
      setCreateMode("provision");
      setCredentials(null);
      return;
    }
    setCreateMode("provision");
    setCredentials(null);
    if (teacher) {
      editForm.reset({
        specialization: teacher.specialization ?? "",
        bio: teacher.bio ?? "",
        yearsExperience: teacher.yearsExperience ?? undefined,
        ijazahChain: teacher.ijazahChain ?? "",
        isAvailable: teacher.isAvailable,
      });
    } else {
      createForm.reset({
        userId: "",
        mosqueId: mosqueId ?? "",
        specialization: "",
        bio: "",
        yearsExperience: undefined,
        ijazahChain: "",
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
        specialization: "",
        bio: "",
        yearsExperience: undefined,
        ijazahChain: "",
        ...emptyAddressValues,
      });
    }
  }, [open, teacher, mosqueId, createForm, provisionForm, editForm, addressForm]);

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

  async function handleCreateSubmit(values: TeacherCreateFormValues) {
    try {
      await createTeacher.mutateAsync(toTeacherCreateRequestBody(values));
      toast.success(t("membership.invite.sent"));
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, createForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleProvisionSubmit(values: TeacherProvisionFormValues) {
    try {
      await provisionTeacher.mutateAsync(toTeacherProvisionRequest(values));
      setCredentials({ email: values.email, password: values.password });
      toast.success(t("membership.provision.success"));
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, provisionForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleEditSubmit(values: TeacherUpdateFormValues) {
    if (!teacher) return;
    const addressValid = await addressForm.trigger();
    if (!addressValid) return;
    try {
      await updateTeacher.mutateAsync({
        id: teacher.id,
        body: toTeacherUpdateRequestBody(values),
      });
      if (teacher.userId) {
        await updateUser.mutateAsync({
          id: teacher.userId,
          body: toUserUpdateRequestBody(addressForm.getValues()),
        });
      }
      toast.success(t("teachers.updateSuccess"));
      onOpenChange(false);
    } catch (error) {
      const { message, fieldErrors } = toMutationError(error, t);
      if (fieldErrors) {
        applyFieldErrors(fieldErrors, editForm.setError, t);
      }
      toast.error(message);
    }
  }

  async function handleSubmit() {
    if (credentials) {
      onOpenChange(false);
      return;
    }

    if (isEdit) {
      void editForm.handleSubmit(handleEditSubmit)();
      return;
    }

    if (createMode === "invite") {
      const valid = await createForm.trigger();
      if (!valid) return;
      await handleCreateSubmit(createForm.getValues());
      return;
    }

    const valid = await provisionForm.trigger();
    if (!valid) return;
    await handleProvisionSubmit(provisionForm.getValues());
  }

  async function copyField(label: string, value: string) {
    try {
      await navigator.clipboard.writeText(value);
      toast.success(t("membership.provision.copied", { field: label }));
    } catch {
      toast.error(t("membership.provision.copyFailed"));
    }
  }

  const createErrors = createForm.formState.errors;
  const provisionErrors = provisionForm.formState.errors;
  const editErrors = editForm.formState.errors;
  const addressDefaultOpen = linkedUser
    ? hasAddressValues(linkedUser)
    : false;

  const submitLabel = credentials
    ? t("membership.provision.done")
    : isEdit
      ? t("actions.save")
      : t("actions.create");

  return (
    <EntityFormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? t("teachers.edit") : t("teachers.create")}
      submitLabel={submitLabel}
      isPending={isPending}
      onSubmit={() => void handleSubmit()}
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
      ) : isEdit ? (
        <FieldGroup>
          <Field data-invalid={!!editErrors.specialization}>
            <FieldLabel htmlFor="teacher-specialization">
              {t("teachers.specialization")}
            </FieldLabel>
            <Input
              id="teacher-specialization"
              aria-invalid={!!editErrors.specialization}
              {...editForm.register("specialization")}
            />
            {editErrors.specialization ? (
              <FieldDescription className="text-destructive">
                {editErrors.specialization.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.yearsExperience}>
            <FieldLabel htmlFor="teacher-years">{t("teachers.yearsExperience")}</FieldLabel>
            <Input
              id="teacher-years"
              type="number"
              min={0}
              aria-invalid={!!editErrors.yearsExperience}
              {...editForm.register("yearsExperience", {
                setValueAs: (value) => (value === "" ? undefined : Number(value)),
              })}
            />
            {editErrors.yearsExperience ? (
              <FieldDescription className="text-destructive">
                {editErrors.yearsExperience.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.ijazahChain}>
            <FieldLabel htmlFor="teacher-ijazah">{t("teachers.ijazahChain")}</FieldLabel>
            <Input
              id="teacher-ijazah"
              aria-invalid={!!editErrors.ijazahChain}
              {...editForm.register("ijazahChain")}
            />
            {editErrors.ijazahChain ? (
              <FieldDescription className="text-destructive">
                {editErrors.ijazahChain.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field data-invalid={!!editErrors.bio}>
            <FieldLabel htmlFor="teacher-bio">{t("teachers.bio")}</FieldLabel>
            <Textarea
              id="teacher-bio"
              aria-invalid={!!editErrors.bio}
              {...editForm.register("bio")}
            />
            {editErrors.bio ? (
              <FieldDescription className="text-destructive">
                {editErrors.bio.message}
              </FieldDescription>
            ) : null}
          </Field>

          <Field className="flex-row items-center justify-between gap-4">
            <FieldLabel htmlFor="teacher-available">{t("teachers.isAvailable")}</FieldLabel>
            <Controller
              name="isAvailable"
              control={editForm.control}
              render={({ field }) => (
                <Switch
                  id="teacher-available"
                  checked={field.value ?? false}
                  onCheckedChange={field.onChange}
                />
              )}
            />
          </Field>

          {teacher?.userId ? (
            <div className="flex flex-col gap-2">
              <p className="text-sm text-muted-foreground">
                {t("profile.editingAddress", {
                  name: teacher.userName ?? linkedUser?.fullName ?? "…",
                })}
              </p>
              <AddressFields
                register={addressForm.register}
                errors={addressForm.formState.errors}
                watch={addressForm.watch}
                setValue={addressForm.setValue}
                idPrefix="teacher-user"
                defaultOpen={addressDefaultOpen}
              />
            </div>
          ) : null}
        </FieldGroup>
      ) : (
        <Tabs
          value={createMode}
          onValueChange={(value) => setCreateMode(value as CreateMode)}
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

          <TabsContent value="provision">
            <FieldGroup>
              {showMosqueField ? (
                <Field data-invalid={!!provisionErrors.mosqueId}>
                  <FieldLabel>{t("teachers.mosqueId")}</FieldLabel>
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
                <FieldLabel htmlFor="teacher-provision-name">
                  {t("profile.fullName")}
                </FieldLabel>
                <Input
                  id="teacher-provision-name"
                  aria-invalid={!!provisionErrors.fullName}
                  {...provisionForm.register("fullName")}
                />
                {provisionErrors.fullName ? (
                  <FieldDescription className="text-destructive">
                    {provisionErrors.fullName.message}
                  </FieldDescription>
                ) : null}
              </Field>
              <Field data-invalid={!!provisionErrors.email}>
                <FieldLabel htmlFor="teacher-provision-email">
                  {t("profile.email")}
                </FieldLabel>
                <Input
                  id="teacher-provision-email"
                  type="email"
                  autoComplete="off"
                  aria-invalid={!!provisionErrors.email}
                  {...provisionForm.register("email")}
                />
                {provisionErrors.email ? (
                  <FieldDescription className="text-destructive">
                    {provisionErrors.email.message}
                  </FieldDescription>
                ) : null}
              </Field>
              <Field data-invalid={!!provisionErrors.password}>
                <FieldLabel htmlFor="teacher-provision-password">
                  {t("membership.provision.password")}
                </FieldLabel>
                <Input
                  id="teacher-provision-password"
                  type="password"
                  autoComplete="new-password"
                  aria-invalid={!!provisionErrors.password}
                  {...provisionForm.register("password")}
                />
                {provisionErrors.password ? (
                  <FieldDescription className="text-destructive">
                    {provisionErrors.password.message}
                  </FieldDescription>
                ) : null}
              </Field>
              <Field data-invalid={!!provisionErrors.confirmPassword}>
                <FieldLabel htmlFor="teacher-provision-confirm">
                  {t("membership.provision.confirmPassword")}
                </FieldLabel>
                <Input
                  id="teacher-provision-confirm"
                  type="password"
                  autoComplete="new-password"
                  aria-invalid={!!provisionErrors.confirmPassword}
                  {...provisionForm.register("confirmPassword")}
                />
                {provisionErrors.confirmPassword ? (
                  <FieldDescription className="text-destructive">
                    {provisionErrors.confirmPassword.message}
                  </FieldDescription>
                ) : null}
              </Field>
              <Field data-invalid={!!provisionErrors.specialization}>
                <FieldLabel htmlFor="teacher-provision-spec">
                  {t("teachers.specialization")}
                </FieldLabel>
                <Input
                  id="teacher-provision-spec"
                  {...provisionForm.register("specialization")}
                />
              </Field>
              <Field data-invalid={!!provisionErrors.yearsExperience}>
                <FieldLabel htmlFor="teacher-provision-years">
                  {t("teachers.yearsExperience")}
                </FieldLabel>
                <Input
                  id="teacher-provision-years"
                  type="number"
                  min={0}
                  {...provisionForm.register("yearsExperience", {
                    setValueAs: (value) =>
                      value === "" ? undefined : Number(value),
                  })}
                />
              </Field>
              <Field data-invalid={!!provisionErrors.ijazahChain}>
                <FieldLabel htmlFor="teacher-provision-ijazah">
                  {t("teachers.ijazahChain")}
                </FieldLabel>
                <Input
                  id="teacher-provision-ijazah"
                  {...provisionForm.register("ijazahChain")}
                />
              </Field>
              <Field data-invalid={!!provisionErrors.bio}>
                <FieldLabel htmlFor="teacher-provision-bio">
                  {t("teachers.bio")}
                </FieldLabel>
                <Textarea
                  id="teacher-provision-bio"
                  {...provisionForm.register("bio")}
                />
              </Field>
              <AddressFields
                register={provisionForm.register}
                errors={provisionForm.formState.errors}
                watch={provisionForm.watch}
                setValue={provisionForm.setValue}
                idPrefix="teacher-provision"
              />
            </FieldGroup>
          </TabsContent>

          <TabsContent value="invite">
            <FieldGroup>
              <Field data-invalid={!!createErrors.userId}>
                <FieldLabel>{t("teachers.userId")}</FieldLabel>
                <Controller
                  name="userId"
                  control={createForm.control}
                  render={({ field }) => (
                    <UserSearchSelect
                      value={field.value}
                      onValueChange={field.onChange}
                      occupancyMosqueId={
                        inviteMosqueId || mosqueId || undefined
                      }
                      occupancyRole="TEACHER"
                    />
                  )}
                />
                {createErrors.userId ? (
                  <FieldDescription className="text-destructive">
                    {createErrors.userId.message}
                  </FieldDescription>
                ) : null}
              </Field>

              {showMosqueField ? (
                <Field data-invalid={!!createErrors.mosqueId}>
                  <FieldLabel>{t("teachers.mosqueId")}</FieldLabel>
                  <Controller
                    name="mosqueId"
                    control={createForm.control}
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger aria-invalid={!!createErrors.mosqueId}>
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

              <Field data-invalid={!!createErrors.specialization}>
                <FieldLabel htmlFor="teacher-specialization">
                  {t("teachers.specialization")}
                </FieldLabel>
                <Input
                  id="teacher-specialization"
                  aria-invalid={!!createErrors.specialization}
                  {...createForm.register("specialization")}
                />
              </Field>

              <Field data-invalid={!!createErrors.yearsExperience}>
                <FieldLabel htmlFor="teacher-years">{t("teachers.yearsExperience")}</FieldLabel>
                <Input
                  id="teacher-years"
                  type="number"
                  min={0}
                  aria-invalid={!!createErrors.yearsExperience}
                  {...createForm.register("yearsExperience", {
                    setValueAs: (value) =>
                      value === "" ? undefined : Number(value),
                  })}
                />
              </Field>

              <Field data-invalid={!!createErrors.ijazahChain}>
                <FieldLabel htmlFor="teacher-ijazah">{t("teachers.ijazahChain")}</FieldLabel>
                <Input
                  id="teacher-ijazah"
                  aria-invalid={!!createErrors.ijazahChain}
                  {...createForm.register("ijazahChain")}
                />
              </Field>

              <Field data-invalid={!!createErrors.bio}>
                <FieldLabel htmlFor="teacher-bio">{t("teachers.bio")}</FieldLabel>
                <Textarea
                  id="teacher-bio"
                  aria-invalid={!!createErrors.bio}
                  {...createForm.register("bio")}
                />
              </Field>
            </FieldGroup>
          </TabsContent>
        </Tabs>
      )}
    </EntityFormDialog>
  );
}
