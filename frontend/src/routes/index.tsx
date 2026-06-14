/* eslint-disable react-refresh/only-export-components -- route configuration module */
import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";

const ChangePasswordPage = lazy(() =>
  import("../pages/app/change-password-page.tsx").then((m) => ({
    default: m.ChangePasswordPage,
  })),
);
import { DashboardPage } from "../pages/app/dashboard-page.tsx";
import { LoginPage } from "../pages/auth/login-page.tsx";
import { RegisterPage } from "../pages/auth/register-page.tsx";
import { GuestRoute } from "./guest-route.tsx";
import { LocaleLayout } from "./locale-layout.tsx";
import { ProtectedRoute } from "./protected-route.tsx";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/en/login" replace />,
  },
  {
    path: "/:locale",
    element: <LocaleLayout />,
    children: [
      {
        path: "login",
        element: (
          <GuestRoute>
            <LoginPage />
          </GuestRoute>
        ),
      },
      {
        path: "register",
        element: (
          <GuestRoute>
            <RegisterPage />
          </GuestRoute>
        ),
      },
      {
        path: "dashboard",
        element: (
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "settings/change-password",
        element: (
          <ProtectedRoute>
            <Suspense fallback={null}>
              <ChangePasswordPage />
            </Suspense>
          </ProtectedRoute>
        ),
      },
    ],
  },
]);
