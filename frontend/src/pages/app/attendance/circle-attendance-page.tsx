import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { toast } from "sonner";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Badge } from "@/components/ui/badge.tsx";
import { Button } from "@/components/ui/button.tsx";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field.tsx";
import { Input } from "@/components/ui/input.tsx";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select.tsx";
import { Skeleton } from "@/components/ui/skeleton.tsx";
import { Spinner } from "@/components/ui/spinner.tsx";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table.tsx";
import {
  useAttendanceByCircle,
  useCreateAttendance,
  useUpdateAttendance,
} from "@/features/attendance/hooks/use-attendance.ts";
import type { AttendanceStatus } from "@/lib/types/api.ts";
import { useCircle } from "@/features/circles/hooks/use-circles.ts";
import { useEnrollments } from "@/features/enrollments/hooks/use-enrollments.ts";
import { useStudents } from "@/features/students/hooks/use-students.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { toMutationError } from "@/lib/errors/map-api-error.ts";
import { ArrowLeftIcon } from "lucide-react";

const ATTENDANCE_STATUSES: AttendanceStatus[] = [
  "PRESENT",
  "LATE",
  "ABSENT",
  "EXCUSED",
  "HOLIDAY",
];

function todayLocalDate(): string {
  return new Date().toISOString().slice(0, 10);
}

type RosterRow = {
  enrollmentId: string;
  studentId: string;
  status: AttendanceStatus;
  attendanceId?: string;
};

export function CircleAttendancePage() {
  const { t } = useTranslation("app");
  const { locale, circleId } = useParams<{ locale: string; circleId: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;
  const [sessionDate, setSessionDate] = useState(todayLocalDate);
  const [roster, setRoster] = useState<RosterRow[]>([]);
  const [isSaving, setIsSaving] = useState(false);

  const { data: circle, isLoading: circleLoading } = useCircle(circleId ?? "", {
    enabled: Boolean(circleId),
  });
  const { data: enrollmentsPage, isLoading: enrollmentsLoading } =
    useEnrollments({ page: 0, size: 500 });
  const { data: studentsPage, isLoading: studentsLoading } = useStudents({
    page: 0,
    size: 500,
  });
  const { data: attendancePage, isLoading: attendanceLoading } =
    useAttendanceByCircle(circleId, { page: 0, size: 500 });

  const createAttendance = useCreateAttendance();
  const updateAttendance = useUpdateAttendance();

  const studentLabels = useMemo(() => {
    const map = new Map<string, string>();
    for (const student of studentsPage?.content ?? []) {
      map.set(student.id, student.id.slice(0, 8));
    }
    return map;
  }, [studentsPage?.content]);

  const activeEnrollments = useMemo(
    () =>
      (enrollmentsPage?.content ?? []).filter(
        (enrollment) =>
          enrollment.circleId === circleId && enrollment.status === "ACTIVE",
      ),
    [circleId, enrollmentsPage?.content],
  );

  const attendanceForDate = useMemo(
    () =>
      (attendancePage?.content ?? []).filter(
        (record) => record.sessionDate === sessionDate,
      ),
    [attendancePage?.content, sessionDate],
  );

  useEffect(() => {
    const nextRoster: RosterRow[] = activeEnrollments.map((enrollment) => {
      const existing = attendanceForDate.find(
        (record) => record.enrollmentId === enrollment.id,
      );
      return {
        enrollmentId: enrollment.id,
        studentId: enrollment.studentId,
        status: existing?.status ?? "PRESENT",
        attendanceId: existing?.id,
      };
    });
    setRoster(nextRoster);
  }, [activeEnrollments, attendanceForDate]);

  const isLoading =
    circleLoading || enrollmentsLoading || studentsLoading || attendanceLoading;

  function updateRowStatus(enrollmentId: string, status: AttendanceStatus) {
    setRoster((prev) =>
      prev.map((row) =>
        row.enrollmentId === enrollmentId ? { ...row, status } : row,
      ),
    );
  }

  async function handleSave() {
    if (!circleId) return;

    setIsSaving(true);
    let savedCount = 0;

    try {
      for (const row of roster) {
        if (row.attendanceId) {
          await updateAttendance.mutateAsync({
            id: row.attendanceId,
            body: { status: row.status },
          });
        } else {
          await createAttendance.mutateAsync({
            enrollmentId: row.enrollmentId,
            circleId,
            sessionDate,
            status: row.status,
          });
        }
        savedCount++;
      }
      toast.success(t("attendance.saved", { count: savedCount }));
    } catch (error) {
      const { message } = toMutationError(error, t);
      toast.error(message || t("attendance.saveError"));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <Button variant="ghost" size="sm" className="w-fit" asChild>
        <Link to={`/${localePrefix}/attendance`}>
          <ArrowLeftIcon data-icon="inline-start" />
          {t("attendance.back")}
        </Link>
      </Button>

      <PageHeader
        title={circle?.name ?? t("attendance.rosterTitle")}
        description={t("attendance.rosterDescription")}
        actions={
          <Button disabled={isSaving || roster.length === 0} onClick={() => void handleSave()}>
            {isSaving ? <Spinner data-icon="inline-start" /> : null}
            {t("attendance.saveRoster")}
          </Button>
        }
      />

      <FieldGroup className="max-w-xs">
        <Field>
          <FieldLabel htmlFor="session-date">{t("attendance.sessionDate")}</FieldLabel>
          <Input
            id="session-date"
            type="date"
            value={sessionDate}
            onChange={(event) => setSessionDate(event.target.value)}
          />
        </Field>
      </FieldGroup>

      {isLoading ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, index) => (
            <Skeleton key={index} className="h-10 w-full" />
          ))}
        </div>
      ) : roster.length === 0 ? (
        <p className="rounded-lg border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
          {t("attendance.noEnrollments")}
        </p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("attendance.student")}</TableHead>
                <TableHead>{t("attendance.status")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {roster.map((row) => (
                <TableRow key={row.enrollmentId}>
                  <TableCell>
                    <span className="font-medium">
                      {studentLabels.get(row.studentId) ?? row.studentId.slice(0, 8)}
                    </span>
                    {row.attendanceId ? (
                      <Badge variant="outline" className="ml-2">
                        {t("actions.edit")}
                      </Badge>
                    ) : null}
                  </TableCell>
                  <TableCell>
                    <Select
                      value={row.status}
                      onValueChange={(value) =>
                        updateRowStatus(row.enrollmentId, value as AttendanceStatus)
                      }
                    >
                      <SelectTrigger className="w-40">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {ATTENDANCE_STATUSES.map((status) => (
                          <SelectItem key={status} value={status}>
                            {t(`attendance.statuses.${status}`)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
}
