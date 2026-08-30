import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { EntityFormDialog } from "@/components/shared/entity-form-dialog.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import { getPagesInJuz, halfPageId } from "@/features/mushaf/index.ts";
import { useUpsertLessonAssignment } from "@/features/memorization/hooks/use-mushaf-memorization.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { formatShortId } from "@/lib/format/ids.ts";
import { cn } from "@/lib/utils";

interface EnrolledCircle {
  circleId: string;
  circleName?: string;
}

interface AssignLessonDialogProps {
  studentId: string;
  circles: EnrolledCircle[];
  defaultCircleId?: string;
}

export function AssignLessonDialog({
  studentId,
  circles,
  defaultCircleId,
}: AssignLessonDialogProps) {
  const { t } = useTranslation("app");
  const [open, setOpen] = useState(false);
  const [circleId, setCircleId] = useState(defaultCircleId ?? circles[0]?.circleId ?? "");
  const [juz, setJuz] = useState(1);
  const [page, setPage] = useState(1);
  const [selectedHalves, setSelectedHalves] = useState<string[]>([]);
  const [note, setNote] = useState("");

  const upsertLesson = useUpsertLessonAssignment(studentId);

  const pagesInJuz = useMemo(() => getPagesInJuz(juz), [juz]);

  function toggleHalf(halfId: string) {
    setSelectedHalves((prev) =>
      prev.includes(halfId) ? prev.filter((id) => id !== halfId) : [...prev, halfId],
    );
  }

  function selectPage(nextPage: number) {
    setPage(nextPage);
    setSelectedHalves([]);
  }

  async function onSubmit() {
    if (!circleId || selectedHalves.length === 0) {
      toast.error(t("memorization.selectHalfRequired"));
      return;
    }
    try {
      await upsertLesson.mutateAsync({
        circleId,
        halfPageIds: selectedHalves,
        note: note.trim() || undefined,
      });
      toast.success(t("memorization.lessonAssigned"));
      setOpen(false);
      setSelectedHalves([]);
      setNote("");
    } catch (error) {
      const { message } = toMutationError(error, t);
      toast.error(message || t("memorization.lessonAssignError"));
    }
  }

  return (
    <>
      <Button onClick={() => setOpen(true)}>{t("memorization.assignLesson")}</Button>
      <EntityFormDialog
        open={open}
        onOpenChange={setOpen}
        title={t("memorization.assignLesson")}
        submitLabel={t("actions.save")}
        isPending={upsertLesson.isPending}
        onSubmit={() => void onSubmit()}
      >
        <FieldGroup>
          <Field>
            <FieldLabel>{t("memorization.circle")}</FieldLabel>
            <Select value={circleId} onValueChange={setCircleId}>
              <SelectTrigger>
                <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
              </SelectTrigger>
              <SelectContent>
                {circles.map((circle) => (
                  <SelectItem key={circle.circleId} value={circle.circleId}>
                    {circle.circleName ?? formatShortId(circle.circleId)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field>
            <FieldLabel>{t("memorization.juz")}</FieldLabel>
            <Select
              value={String(juz)}
              onValueChange={(value) => {
                const nextJuz = Number(value);
                setJuz(nextJuz);
                const firstPage = getPagesInJuz(nextJuz)[0] ?? 1;
                selectPage(firstPage);
              }}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Array.from({ length: 30 }, (_, i) => i + 1).map((n) => (
                  <SelectItem key={n} value={String(n)}>
                    {n} — {t(`memorization.juzNames.${n}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field>
            <FieldLabel>{t("memorization.pageLabel", { page: "" }).replace(/\s*$/, "")}</FieldLabel>
            <div className="grid max-h-40 grid-cols-6 gap-1 overflow-y-auto rounded-md border p-2">
              {pagesInJuz.map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => selectPage(p)}
                  className={cn(
                    "rounded px-1 py-0.5 text-sm tabular-nums transition-colors",
                    page === p
                      ? "bg-primary text-primary-foreground"
                      : "hover:bg-muted",
                  )}
                >
                  {p}
                </button>
              ))}
            </div>
          </Field>
          <Field>
            <FieldLabel>{t("memorization.selectHalves")}</FieldLabel>
            <div className="flex gap-2">
              {(["A", "B"] as const).map((half) => {
                const id = halfPageId(page, half);
                const selected = selectedHalves.includes(id);
                return (
                  <Button
                    key={half}
                    type="button"
                    variant={selected ? "default" : "outline"}
                    onClick={() => toggleHalf(id)}
                  >
                    {half === "A" ? t("memorization.halfA") : t("memorization.halfB")}
                  </Button>
                );
              })}
            </div>
          </Field>
          <Field>
            <FieldLabel>{t("memorization.teacherNotes")}</FieldLabel>
            <Textarea
              rows={3}
              value={note}
              onChange={(event) => setNote(event.target.value)}
            />
          </Field>
        </FieldGroup>
      </EntityFormDialog>
    </>
  );
}
