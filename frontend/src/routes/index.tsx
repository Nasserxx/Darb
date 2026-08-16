import { createBrowserRouter } from "react-router-dom";

import { GuestRoute } from "./guest-route.tsx";
import { LocaleLayout } from "./locale-layout.tsx";
import { ProtectedRoute } from "./protected-route.tsx";
import { RoleRoute } from "./role-route.tsx";
import {
  AchievementsPage,
  AdminDashboardPage,
  AttendancePage,
  ChangePasswordPage,
  CircleAttendancePage,
  CircleMessagesPage,
  CirclesPage,
  DashboardPage,
  EnrollmentsPage,
  ForbiddenPage,
  JoinRedirectPage,
  LazyPage,
  LoginPage,
  MessagesPage,
  MosqueAdminsPage,
  MosqueDetailPage,
  MosquesPage,
  NotificationsPage,
  OnboardingPage,
  ParentStudentsPage,
  PaymentsPage,
  ProfilePage,
  ProtectedAppPage,
  RegisterPage,
  ReportsPage,
  RootRedirect,
  StudentDetailPage,
  StudentGoalsPage,
  StudentMemorizationPage,
  StudentsPage,
  TeachersPage,
} from "./route-elements.tsx";

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
              allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"]}
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
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"]}>
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
              allowed={["SUPER_ADMIN", "MOSQUE_ADMIN", "TEACHER", "STUDENT", "PARENT"]}
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
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <MosquesPage />
            </RoleRoute>
          </ProtectedAppPage>
        ),
      },
      {
        path: "mosques/:id",
        element: (
          <ProtectedAppPage>
            <RoleRoute allowed={["SUPER_ADMIN", "MOSQUE_ADMIN"]}>
              <MosqueDetailPage />
            </RoleRoute>
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
