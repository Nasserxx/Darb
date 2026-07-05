import { useTranslation } from "react-i18next";

import { Badge } from "@/components/ui/badge";
import type { StudentResponse } from "@/features/students/types/index.ts";
import { formatShortId } from "@/lib/format/ids.ts";

type StudentDetailCardProps = {
  student: StudentResponse;
};

export function StudentDetailCard({ student }: StudentDetailCardProps) {
  const { t } = useTranslation("app");

  const fields = [
    { label: t("students.userId"), value: formatShortId(student.userId) },
    { label: t("students.mosqueId"), value: formatShortId(student.mosqueId) },
    { label: t("students.nationalId"), value: student.nationalId ?? "—" },
    { label: t("students.memorizedJuz"), value: student.memorizedJuz ?? "—" },
    { label: t("students.totalAbsences"), value: student.totalAbsences },
    { label: t("students.totalLateArrivals"), value: student.totalLateArrivals },
    { label: t("students.enrolledAt"), value: new Date(student.enrolledAt).toLocaleString() },
  ];

  return (
    <div className="rounded-lg border border-border bg-card">
      <div className="flex items-center justify-between gap-4 border-b border-border px-6 py-4">
        <h2 className="font-serif text-xl font-semibold">{t("students.detail")}</h2>
        <Badge variant="outline">{t(`enums.enrollmentStatus.${student.status}`)}</Badge>
      </div>
      <dl className="grid gap-4 p-6 sm:grid-cols-2">
        {fields.map((field) => (
          <div key={field.label} className="flex flex-col gap-1">
            <dt className="text-sm text-muted-foreground">{field.label}</dt>
            <dd className="text-sm font-medium text-foreground">{field.value}</dd>
          </div>
        ))}
        {student.medicalNotes ? (
          <div className="flex flex-col gap-1 sm:col-span-2">
            <dt className="text-sm text-muted-foreground">{t("students.medicalNotes")}</dt>
            <dd className="text-sm text-foreground">{student.medicalNotes}</dd>
          </div>
        ) : null}
      </dl>
    </div>
  );
}
