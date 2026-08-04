import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useCircle } from "@/features/circles/hooks/use-circles.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { useStudentEnrollments } from "@/features/enrollments/hooks/use-enrollments.ts";
import { DEFAULT_LOCALE, translateRole } from "@/i18n/index.ts";
import {
  Award,
  BookOpen,
  CircleDot,
  ClipboardList,
  GraduationCap,
  MessageSquare,
  UserCheck,
  Users,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

interface DashboardLink {
  to: string;
  icon: LucideIcon;
  title: string;
  description: string;
}

export function DashboardPage() {
  const { t } = useTranslation(["app", "auth"]);
  const { user } = useAuth();
  const { profile } = useWorkspace();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  const role = (user?.role ?? "").toUpperCase().replace(/-/g, "_");

  let links: DashboardLink[];

  if (role === "TEACHER") {
    links = [
      {
        to: `/${localePrefix}/circles`,
        icon: CircleDot,
        title: t("nav.circles"),
        description: t("circles.description"),
      },
      {
        to: `/${localePrefix}/students`,
        icon: Users,
        title: t("nav.students"),
        description: t("students.description"),
      },
      {
        to: `/${localePrefix}/enrollments`,
        icon: ClipboardList,
        title: t("nav.enrollments"),
        description: t("enrollments.description"),
      },
      {
        to: `/${localePrefix}/attendance`,
        icon: UserCheck,
        title: t("nav.attendance"),
        description: t("attendance.description"),
      },
      {
        to: `/${localePrefix}/messages`,
        icon: MessageSquare,
        title: t("nav.messages"),
        description: t("messages.description"),
      },
    ];
  } else if (role === "STUDENT") {
    const studentId = profile?.studentId;
    const memorizationTarget = studentId
      ? `/${localePrefix}/memorization/student/${studentId}`
      : `/${localePrefix}/circles`;
    const goalsTarget = studentId
      ? `/${localePrefix}/goals/student/${studentId}`
      : `/${localePrefix}/circles`;
    const attendanceTarget = studentId
      ? `/${localePrefix}/attendance`
      : `/${localePrefix}/circles`;

    links = [
      {
        to: memorizationTarget,
        icon: BookOpen,
        title: t("nav.progress"),
        description: t("memorization.description"),
      },
      {
        to: goalsTarget,
        icon: CircleDot,
        title: t("goals.title"),
        description: t("goals.description"),
      },
      {
        to: `/${localePrefix}/achievements`,
        icon: Award,
        title: t("nav.achievements"),
        description: t("achievements.description"),
      },
      {
        to: `/${localePrefix}/messages`,
        icon: MessageSquare,
        title: t("nav.messages"),
        description: t("messages.description"),
      },
      {
        to: attendanceTarget,
        icon: UserCheck,
        title: t("nav.attendance"),
        description: t("attendance.description"),
      },
    ];
  } else if (role === "PARENT") {
    links = [
      {
        to: `/${localePrefix}/students`,
        icon: Users,
        title: t("nav.students"),
        description: t("students.description"),
      },
      {
        to: `/${localePrefix}/circles`,
        icon: CircleDot,
        title: t("nav.circles"),
        description: t("circles.studentDescription"),
      },
      {
        to: `/${localePrefix}/attendance`,
        icon: UserCheck,
        title: t("nav.attendance"),
        description: t("attendance.parentDescription"),
      },
      {
        to: `/${localePrefix}/achievements`,
        icon: Award,
        title: t("nav.achievements"),
        description: t("achievements.description"),
      },
      {
        to: `/${localePrefix}/messages`,
        icon: MessageSquare,
        title: t("nav.messages"),
        description: t("messages.description"),
      },
    ];
  } else {
    // SUPER_ADMIN / MOSQUE_ADMIN
    links = [
      {
        to: `/${localePrefix}/circles`,
        icon: CircleDot,
        title: t("nav.circles"),
        description: t("circles.description"),
      },
      {
        to: `/${localePrefix}/students`,
        icon: Users,
        title: t("nav.students"),
        description: t("students.description"),
      },
      {
        to: `/${localePrefix}/attendance`,
        icon: UserCheck,
        title: t("nav.attendance"),
        description: t("attendance.description"),
      },
      {
        to: `/${localePrefix}/messages`,
        icon: MessageSquare,
        title: t("nav.messages"),
        description: t("messages.description"),
      },
    ];
  }

  const { data: enrollmentsPage } = useStudentEnrollments(profile?.studentId);
  const activeEnrollment = enrollmentsPage?.content?.find(
    (e) => e.status === "ACTIVE",
  );
  const { data: circle } = useCircle(activeEnrollment?.circleId ?? "", {
    enabled: !!activeEnrollment?.circleId,
  });
  const teacherName = circle?.teacherName;

  return (
    <div className="auth-stagger flex flex-col gap-6">
      <PageHeader
        title={t("auth:dashboard.title")}
        description={
          user
            ? t("auth:dashboard.welcome", {
                name: user.fullName,
                role: translateRole(user.role),
              })
            : undefined
        }
      />
      <div className="grid gap-4 sm:grid-cols-2">
        {links.map((link) => (
          <Link key={link.to} to={link.to}>
            <Card className="h-full transition-colors hover:bg-muted/40">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-lg">
                  <link.icon className="text-primary" />
                  {link.title}
                </CardTitle>
                <CardDescription>{link.description}</CardDescription>
              </CardHeader>
              <CardContent />
            </Card>
          </Link>
        ))}
        {role === "STUDENT" && teacherName && (
          <Card className="h-full border-primary/20">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg">
                <GraduationCap className="text-primary" />
                {t("nav.myTeacher")}
              </CardTitle>
              <CardDescription>
                {t("circles.myTeacherDescription")}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="font-medium">{teacherName}</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}

