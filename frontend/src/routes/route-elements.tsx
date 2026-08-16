import { lazy, Suspense } from "react";
import { Navigate } from "react-router-dom";

import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { resolvePostAuthPath } from "@/lib/navigation/post-auth.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { Skeleton } from "@/components/ui/skeleton";
import { AppLayout } from "./app-layout.tsx";
import { ProtectedRoute } from "./protected-route.tsx";

export const LoginPage = lazy(() =>
  import("../pages/auth/login-page.tsx").then((m) => ({ default: m.LoginPage })),
);
export const RegisterPage = lazy(() =>
  import("../pages/auth/register-page.tsx").then((m) => ({ default: m.RegisterPage })),
);
export const JoinRedirectPage = lazy(() =>
  import("../pages/app/join-redirect-page.tsx").then((m) => ({
    default: m.JoinRedirectPage,
  })),
);
export const OnboardingPage = lazy(() =>
  import("../pages/app/onboarding-page.tsx").then((m) => ({ default: m.OnboardingPage })),
);
export const ForbiddenPage = lazy(() =>
  import("../pages/app/forbidden-page.tsx").then((m) => ({ default: m.ForbiddenPage })),
);
export const AdminDashboardPage = lazy(() =>
  import("../pages/app/admin/admin-dashboard-page.tsx").then((m) => ({
    default: m.AdminDashboardPage,
  })),
);
export const DashboardPage = lazy(() =>
  import("../pages/app/dashboard-page.tsx").then((m) => ({ default: m.DashboardPage })),
);
export const ChangePasswordPage = lazy(() =>
  import("../pages/app/change-password-page.tsx").then((m) => ({
    default: m.ChangePasswordPage,
  })),
);
export const StudentsPage = lazy(() =>
  import("../pages/app/students/students-page.tsx").then((m) => ({
    default: m.StudentsPage,
  })),
);
export const StudentDetailPage = lazy(() =>
  import("../pages/app/students/student-detail-page.tsx").then((m) => ({
    default: m.StudentDetailPage,
  })),
);
export const ParentStudentsPage = lazy(() =>
  import("../pages/app/parent-students/parent-students-page.tsx").then((m) => ({
    default: m.ParentStudentsPage,
  })),
);
export const CirclesPage = lazy(() =>
  import("../pages/app/circles/circles-page.tsx").then((m) => ({
    default: m.CirclesPage,
  })),
);
export const EnrollmentsPage = lazy(() =>
  import("../pages/app/enrollments/enrollments-page.tsx").then((m) => ({
    default: m.EnrollmentsPage,
  })),
);
export const MessagesPage = lazy(() =>
  import("../pages/app/messages/messages-page.tsx").then((m) => ({
    default: m.MessagesPage,
  })),
);
export const CircleMessagesPage = lazy(() =>
  import("../pages/app/messages/circle-messages-page.tsx").then((m) => ({
    default: m.CircleMessagesPage,
  })),
);
export const NotificationsPage = lazy(() =>
  import("../pages/app/notifications/notifications-page.tsx").then((m) => ({
    default: m.NotificationsPage,
  })),
);
export const PaymentsPage = lazy(() =>
  import("../pages/app/payments/payments-page.tsx").then((m) => ({
    default: m.PaymentsPage,
  })),
);
export const ReportsPage = lazy(() =>
  import("../pages/app/reports/reports-page.tsx").then((m) => ({
    default: m.ReportsPage,
  })),
);
export const ProfilePage = lazy(() =>
  import("../pages/app/settings/profile-page.tsx").then((m) => ({
    default: m.ProfilePage,
  })),
);
export const MosquesPage = lazy(() =>
  import("../pages/app/mosques/mosques-page.tsx").then((m) => ({
    default: m.MosquesPage,
  })),
);
export const MosqueDetailPage = lazy(() =>
  import("../pages/app/mosques/mosque-detail-page.tsx").then((m) => ({
    default: m.MosqueDetailPage,
  })),
);
export const MosqueAdminsPage = lazy(() =>
  import("../pages/app/mosque-admins/mosque-admins-page.tsx").then((m) => ({
    default: m.MosqueAdminsPage,
  })),
);
export const TeachersPage = lazy(() =>
  import("../pages/app/teachers/teachers-page.tsx").then((m) => ({
    default: m.TeachersPage,
  })),
);
export const AttendancePage = lazy(() =>
  import("../pages/app/attendance/attendance-page.tsx").then((m) => ({
    default: m.AttendancePage,
  })),
);
export const CircleAttendancePage = lazy(() =>
  import("../pages/app/attendance/circle-attendance-page.tsx").then((m) => ({
    default: m.CircleAttendancePage,
  })),
);
export const StudentMemorizationPage = lazy(() =>
  import("../pages/app/memorization/student-memorization-page.tsx").then((m) => ({
    default: m.StudentMemorizationPage,
  })),
);
export const StudentGoalsPage = lazy(() =>
  import("../pages/app/goals/student-goals-page.tsx").then((m) => ({
    default: m.StudentGoalsPage,
  })),
);
export const AchievementsPage = lazy(() =>
  import("../pages/app/achievements/achievements-page.tsx").then((m) => ({
    default: m.AchievementsPage,
  })),
);

function PageFallback() {
  return (
    <div className="flex flex-col gap-3 p-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-32 w-full" />
    </div>
  );
}

export function LazyPage({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<PageFallback />}>{children}</Suspense>;
}

export function RootRedirect() {
  const { isAuthenticated, user, isLoading } = useAuth();
  const { profileStatus, isLoading: workspaceLoading } = useWorkspace();
  if (isLoading || (isAuthenticated && workspaceLoading)) return null;
  if (isAuthenticated && user) {
    const path = resolvePostAuthPath({
      locale: DEFAULT_LOCALE,
      role: user.role,
      profileStatus,
    });
    if (path) {
      return <Navigate to={path} replace />;
    }
  }
  return <Navigate to={`/${DEFAULT_LOCALE}/login`} replace />;
}

export function ProtectedAppPage({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <AppLayout>
        <LazyPage>{children}</LazyPage>
      </AppLayout>
    </ProtectedRoute>
  );
}
