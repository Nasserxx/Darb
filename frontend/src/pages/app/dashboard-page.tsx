import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";

import { PageHeader } from "@/components/shared/page-header.tsx";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { DEFAULT_LOCALE, translateRole } from "@/i18n/index.ts";
import { Award, BookOpen, CircleDot, MessageSquare } from "lucide-react";

export function DashboardPage() {
  const { t } = useTranslation(["app", "auth"]);
  const { user } = useAuth();
  const { profile } = useWorkspace();
  const { locale } = useParams<{ locale: string }>();
  const localePrefix = locale ?? DEFAULT_LOCALE;

  const studentId = profile?.studentId;

  const links = [
    {
      to: studentId
        ? `/${localePrefix}/memorization/student/${studentId}`
        : `/${localePrefix}/circles`,
      icon: BookOpen,
      title: t("nav.progress"),
      description: t("memorization.description"),
    },
    {
      to: studentId
        ? `/${localePrefix}/goals/student/${studentId}`
        : `/${localePrefix}/circles`,
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
  ];

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
      </div>
    </div>
  );
}
