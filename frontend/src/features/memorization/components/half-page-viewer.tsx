import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Field, FieldLabel } from "@/components/ui/field.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs.tsx";
import { Textarea } from "@/components/ui/textarea.tsx";
import {
  expandAyahRange,
  getHalfPage,
  type PageHalf,
} from "@/features/mushaf/index.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import type { RecitationGrade } from "@/lib/types/api.ts";
import { cn } from "@/lib/utils";

import { AssessmentToolbar, ReadonlyStampSummary } from "./assessment-toolbar.tsx";
import { AyahStampList } from "./ayah-stamp-list.tsx";
import { MushafSpotlight } from "./mushaf-spotlight.tsx";
import {
  useCreateMemorizationAttempt,
  useMemorizationAttempt,
  usePageMetadata,
} from "../hooks/use-mushaf-memorization.ts";
import { useStampSession } from "../hooks/use-stamp-session.ts";
import type { MemorizationAttemptResponse } from "../types/index.ts";

const GRADES: RecitationGrade[] = [
  "EXCELLENT",
  "VERY_GOOD",
  "GOOD",
  "ACCEPTABLE",
  "POOR",
];

function todayLocalDate(): string {
  return new Date().toISOString().slice(0, 10);
}

interface HalfPageViewerProps {
  studentId: string;
  localePrefix: string;
  circleId: string;
  page: number;
  half: PageHalf;
  readonly: boolean;
}

interface HalfPageSessionProps {
  studentId: string;
  localePrefix: string;
  circleId: string;
  page: number;
  half: PageHalf;
  readonly: boolean;
  existingAttempt: MemorizationAttemptResponse | null | undefined;
  juz: number;
  onNavigateHalf: (half: PageHalf) => void;
}

function HalfPageSession({
  studentId,
  localePrefix,
  circleId,
  page,
  half,
  readonly,
  existingAttempt,
  juz,
  onNavigateHalf,
}: HalfPageSessionProps) {
  const { t } = useTranslation("app");
  const [grade, setGrade] = useState<RecitationGrade | undefined>(
    existingAttempt?.grade,
  );
  const [notes, setNotes] = useState(existingAttempt?.notes ?? "");
  const sessionDate = todayLocalDate();

  const { data: pageMeta } = usePageMetadata(page);
  const createAttempt = useCreateMemorizationAttempt(studentId);

  const halfEntry = useMemo(() => {
    const fromApi = pageMeta?.halves?.find((entry) => entry.half === half);
    return fromApi ?? getHalfPage(page, half);
  }, [pageMeta, page, half]);

  const ayahs = useMemo(() => expandAyahRange(halfEntry), [halfEntry]);

  const {
    stamps,
    activeType,
    setActiveType,
    tajweedCount,
    hifzCount,
    addImageStamp,
    addAyahStamp,
    undoLast,
  } = useStampSession({
    initialStamps: existingAttempt?.stamps ?? [],
    readonly,
  });

  function handleImageClick(x: number, y: number) {
    const firstAyah = ayahs[0];
    if (!firstAyah) return;
    addImageStamp(x, y, firstAyah.surah, firstAyah.ayah);
  }

  async function handleSave() {
    try {
      await createAttempt.mutateAsync({
        circleId,
        page,
        half,
        sessionDate,
        grade,
        notes: notes.trim() || undefined,
        stamps,
      });
      toast.success(t("memorization.attemptSaved"));
    } catch (error) {
      const { message } = toMutationError(error, t);
      toast.error(message || t("memorization.attemptSaveError"));
    }
  }

  const mushafPanel = (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2">
        <Button
          type="button"
          size="sm"
          variant={half === "A" ? "default" : "outline"}
          onClick={() => onNavigateHalf("A")}
        >
          {t("memorization.halfA")}
        </Button>
        <Button
          type="button"
          size="sm"
          variant={half === "B" ? "default" : "outline"}
          onClick={() => onNavigateHalf("B")}
        >
          {t("memorization.halfB")}
        </Button>
      </div>
      <MushafSpotlight
        page={page}
        activeHalf={half}
        stamps={stamps}
        readonly={readonly}
        onImageClick={readonly ? undefined : handleImageClick}
      />
    </div>
  );

  const ayahPanel = (
    <div className="flex min-h-0 flex-1 flex-col gap-3">
      <AyahStampList
        ayahs={ayahs}
        stamps={stamps}
        readonly={readonly}
        onAyahClick={readonly ? undefined : addAyahStamp}
      />
      <Field>
        <FieldLabel>{t("memorization.grade")}</FieldLabel>
        {readonly ? (
          <p className="text-sm text-muted-foreground">
            {grade ? t(`memorization.grades.${grade}`) : "—"}
          </p>
        ) : (
          <Select
            value={grade ?? ""}
            onValueChange={(value) =>
              setGrade((value as RecitationGrade) || undefined)
            }
          >
            <SelectTrigger>
              <SelectValue placeholder={t("onboarding.selectPlaceholder")} />
            </SelectTrigger>
            <SelectContent>
              {GRADES.map((g) => (
                <SelectItem key={g} value={g}>
                  {t(`memorization.grades.${g}`)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </Field>
      <Field>
        <FieldLabel>{t("memorization.teacherNotes")}</FieldLabel>
        <Textarea
          rows={3}
          value={notes}
          readOnly={readonly}
          disabled={readonly}
          onChange={
            readonly
              ? undefined
              : (event) => setNotes(event.target.value)
          }
        />
      </Field>
    </div>
  );

  return (
    <>
      {!readonly ? (
        <AssessmentToolbar
          activeType={activeType}
          onTypeChange={setActiveType}
          onUndo={undoLast}
          onSave={() => void handleSave()}
          tajweedCount={tajweedCount}
          hifzCount={hifzCount}
          isSaving={createAttempt.isPending}
          canUndo={stamps.length > 0}
        />
      ) : (
        <ReadonlyStampSummary
          stampCount={stamps.length}
          tajweedCount={tajweedCount}
          hifzCount={hifzCount}
        />
      )}

      <div className="hidden gap-4 lg:grid lg:grid-cols-[3fr_2fr]">
        {mushafPanel}
        {ayahPanel}
      </div>

      <Tabs defaultValue="mushaf" className="lg:hidden">
        <TabsList className="w-full">
          <TabsTrigger value="mushaf" className="flex-1">
            {t("memorization.tabMushaf")}
          </TabsTrigger>
          <TabsTrigger value="ayahs" className="flex-1">
            {t("memorization.tabAyahs")}
          </TabsTrigger>
        </TabsList>
        <TabsContent value="mushaf">{mushafPanel}</TabsContent>
        <TabsContent value="ayahs">{ayahPanel}</TabsContent>
      </Tabs>

      {!readonly && stamps.length > 0 ? (
        <p className={cn("text-sm text-muted-foreground")}>
          {t("memorization.savePreview", {
            tajweed: tajweedCount,
            hifz: hifzCount,
          })}
        </p>
      ) : null}

      <Button variant="outline" asChild className="lg:hidden">
        <Link
          to={`/${localePrefix}/memorization/student/${studentId}/juz/${juz}?circleId=${circleId}`}
        >
          {t("memorization.back")}
        </Link>
      </Button>
    </>
  );
}

export function HalfPageViewer({
  studentId,
  localePrefix,
  circleId,
  page,
  half,
  readonly,
}: HalfPageViewerProps) {
  const { t } = useTranslation("app");
  const navigate = useNavigate();

  const { data: existingAttempt, isLoading } = useMemorizationAttempt(
    studentId,
    page,
    half,
    circleId,
  );

  const juz = useMemo(() => getHalfPage(page, half).juz, [page, half]);

  function navigateHalf(nextHalf: PageHalf) {
    navigate(
      `/${localePrefix}/memorization/student/${studentId}/page/${page}/half/${nextHalf}?circleId=${circleId}`,
      { replace: true },
    );
  }

  const sessionKey = `${half}-${existingAttempt?.id ?? (isLoading ? "loading" : "new")}`;

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title={t("memorization.halfPageTitle", {
          page,
          half: half === "A" ? t("memorization.halfA") : t("memorization.halfB"),
        })}
        description={t("memorization.halfPageDescription")}
        actions={
          <Button variant="outline" asChild>
            <Link
              to={`/${localePrefix}/memorization/student/${studentId}/juz/${juz}?circleId=${circleId}`}
            >
              {t("memorization.back")}
            </Link>
          </Button>
        }
      />

      {!isLoading ? (
        <HalfPageSession
          key={sessionKey}
          studentId={studentId}
          localePrefix={localePrefix}
          circleId={circleId}
          page={page}
          half={half}
          readonly={readonly}
          existingAttempt={existingAttempt}
          juz={juz}
          onNavigateHalf={navigateHalf}
        />
      ) : null}
    </div>
  );
}
