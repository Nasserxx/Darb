import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";

import { useAuth } from "@/features/auth/hooks/use-auth.ts";
import { useWorkspace } from "@/features/workspace/context/workspace-provider.tsx";
import { resolvePostAuthPath } from "@/lib/navigation/post-auth.ts";
import { DEFAULT_LOCALE } from "@/i18n/index.ts";
import { AppLayout } from "./app-layout.tsx";
import { GuestRoute } from "./guest-route.tsx";
import { LocaleLayout } from "./locale-layout.tsx";
import { ProtectedRoute } from "./protected-route.tsx";
import { RoleRoute } from "./role-route.tsx";
import { Skeleton } from "@/components/ui/skeleton";

const LoginPage = lazy(() =>
  import("../pages/auth/login-page.tsx").then((m) => ({ default: m.LoginPage })),
);
const RegisterPage = lazy(() =>
  import("../pages/auth/register-page.tsx").then((m) => ({ default: m.RegisterPage })),
);
const JoinRedirectPage = lazy(() =>
  import("../pages/app/join-redirect-page.tsx").then((m) => ({
    default: m.JoinRedirectPage,
  })),
);
const OnboardingPage = lazy(() =>
  import("../pages/app/onboarding-page.tsx").then((m) => ({ default: m.OnboardingPage })),
);
const ForbiddenPage = lazy(() =>
  import("../pages/app/forbidden-page.tsx").then((m) => ({ default: m.ForbiddenPage })),
);
const AdminDashboardPage = lazy(() =>
  import("../pages/app/admin/admin-dashboard-page.tsx").then((m) => ({
    default: m.AdminDashboardPage,
  })),
);
const DashboardPage = lazy(() =>
  import("../pages/app/dashboard-page.tsx").then((m) => ({ default: m.DashboardPage })),
);
const ChangePasswordPage = lazy(() =>
  import("../pages/app/change-password-page.tsx").then((m) => ({
    default: m.ChangePasswordPage,
  })),
);
const StudentsPage = lazy(() =>
  import("../pages/app/students/students-page.tsx").then((m) => ({
    default: m.StudentsPage,
  })),
);
const StudentDetailPage = lazy(() =>
  import("../pages/app/students/student-detail-page.tsx").then((m) => ({
    default: m.StudentDetailPage,
  })),
);
const ParentStudentsPage = lazy(() =>
  import("../pages/app/parent-students/parent-students-page.tsx").then((m) => ({
    default: m.ParentStudentsPage,
  })),
);
const CirclesPage = lazy(() =>
  import("../pages/app/circles/circles-page.tsx").then((m) => ({
    default: m.CirclesPage,
  })),
);
const EnrollmentsPage = lazy(() =>
  import("../pages/app/enrollments/enrollments-page.tsx").then((m) => ({
    default: m.EnrollmentsPage,
  })),
);
const MessagesPage = lazy(() =>
  import("../pages/app/messages/messages-page.tsx").then((m) => ({
    default: m.MessagesPage,
  })),
);
const CircleMessagesPage = lazy(() =>
  import("../pages/app/messages/circle-messages-page.tsx").then((m) => ({
    default: m.CircleMessagesPage,
  })),
);
const NotificationsPage = lazy(() =>
  import("../pages/app/notifications/notifications-page.tsx").then((m) => ({
    default: m.NotificationsPage,
  })),
);
const PaymentsPage = lazy(() =>
  import("../pages/app/payments/payments-page.tsx").then((m) => ({
    default: m.PaymentsPage,
  })),
);
const ReportsPage = lazy(() =>
  import("../pages/app/reports/reports-page.tsx").then((m) => ({
    default: m.ReportsPage,
  })),
);
const ProfilePage = lazy(() =>
  import("../pages/app/settings/profile-page.tsx").then((m) => ({
    default: m.ProfilePage,
  })),
);
const MosquesPage = lazy(() =>
  import("../pages/app/mosques/mosques-page.tsx").then((m) => ({
    default: m.MosquesPage,
  })),
);
const MosqueAdminsPage = lazy(() =>
  import("../pages/app/mosque-admins/mosque-admins-page.tsx").then((m) => ({
    default: m.MosqueAdminsPage,
  })),
);
const TeachersPage = lazy(() =>
  import("../pages/app/teachers/teachers-page.tsx").then((m) => ({
    default: m.TeachersPage,
  })),
);
const AttendancePage = lazy(() =>
  import("../pages/app/attendance/attendance-page.tsx").then((m) => ({
    default: m.AttendancePage,
  })),
);
const CircleAttendancePage = lazy(() =>
  import("../pages/app/attendance/circle-attendance-page.tsx").then((m) => ({
    default: m.CircleAttendancePage,
  })),
);
const StudentMemorizationPage = lazy(() =>
  import("../pages/app/memorization/student-memorization-page.tsx").then((m) => ({
    default: m.StudentMemorizationPage,
  })),
);
const StudentGoalsPage = lazy(() =>
  import("../pages/app/goals/student-goals-page.tsx").then((m) => ({
    default: m.StudentGoalsPage,
  })),
);
const AchievementsPage = lazy(() =>
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

function LazyPage({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<PageFallback />}>{children}</Suspense>;
}

function RootRedirect() {
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

function ProtectedAppPage({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <AppLayout>
        <LazyPage>{children}</LazyPage>
      </AppLayout>
    </ProtectedRoute>
  );
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <RootRedirect />,
  },
  {
    path: "/:locale",
    element: <LocaleLayout />,
    children: [
      {
        path: "login",
        element: (
          <GuestRoute>
            <LazyPage>
              <LoginPage />
            </LazyPage>
          </GuestRoute>
        ),
      },
      {
        path: "register",
        element: (
          <GuestRoute>
            <LazyPage>
              <RegisterPage />
            </LazyPage>
          </GuestRoute>
        ),
      },
      {
        path: "join",
        element: (
          <LazyPage>
            <JoinRedirectPage />
          </LazyPage>
        ),
      },
      {
        path: "onboarding",
        element: (
          <ProtectedRoute>
            <LazyPage>
              <OnboardingPage />
            </LazyPage>
          </ProtectedRoute>
        ),
      },
      {
        path: "forbidden",
        element: (
          <ProtectedAppPage>
            <ForbiddenPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "admin",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <AdminDashboardPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "dashboard",
        element: (
          <ProtectedAppPage>
            <DashboardPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "settings/change-password",
        element: (
          <ProtectedAppPage>
            <ChangePasswordPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "students",
        element: (
          <ProtectedAppPage>
            <RoleRoute
              allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "PARENT"]}
            >
              <StudentsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "students/:id",
        element: (
          <ProtectedAppPage>
            <RoleRoute
              allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "PARENT"]}
            >
              <StudentDetailPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "parent-students",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <ParentStudentsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "circles",
        element: (
          <ProtectedAppPage>
            <RoleRoute
              allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT"]}
            >
              <CirclesPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "enrollments",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"]}>
              <EnrollmentsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "messages",
        element: (
          <ProtectedAppPage>
            <MessagesPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "messages/circle/:id",
        element: (
          <ProtectedAppPage>
            <CircleMessagesPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "notifications",
        element: (
          <ProtectedAppPage>
            <NotificationsPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "payments",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <PaymentsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "reports",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <ReportsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "attendance",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"]}>
              <AttendancePage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "attendance/circle/:circleId",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER"]}>
              <CircleAttendancePage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "memorization/student/:studentId",
        element: (
          <ProtectedAppPage>
            <RoleRoute
              allowed={[
                "SUPER_ADMIN",
                "MOSQUE_ADMIN",
                "TEACHER",
                "STUDENT",
                "PARENT",
              ]}
            >
              <StudentMemorizationPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "goals/student/:studentId",
        element: (
          <ProtectedAppPage>
            <RoleRoute
              allowed={[
                "SUPER_ADMIN",
                "MOSQUE_ADMIN",
                "TEACHER",
                "STUDENT",
                "PARENT",
              ]}
            >
              <StudentGoalsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "achievements",
        element: (
          <ProtectedAppPage>
            <RoleRoute
              allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT"]}
            >
              <AchievementsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "settings/profile",
        element: (
          <ProtectedAppPage>
            <ProfilePage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "mosques",
        element: (
          <ProtectedAppPage>
            <MosquesPage />
          </ProtectedAppPage>
        ),
      },
      {
        path: "mosque-admins",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN"]}>
              <MosqueAdminsPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "teachers",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <TeachersPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
    ],
  },
]);
