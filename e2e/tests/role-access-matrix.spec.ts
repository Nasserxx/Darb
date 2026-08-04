import { test } from "@playwright/test";

import {
  assertRouteAccess,
  loginViaUI,
  waitForAssignedProfile,
} from "../helpers/auth.ts";
import {
  createMosqueAdminWithMosque,
  createStudentForMosque,
  createTeacherForMosque,
  type TestUser,
} from "../helpers/users.ts";

const LOCALE = "en";

type SeedRole = "mosque_admin" | "teacher" | "student";

type RouteExpectation = {
  path: string;
  allowed: boolean;
  heading?: string;
};

type RoleAccessRow = {
  role: SeedRole;
  prepare: () => Promise<TestUser>;
  cases: RouteExpectation[];
};

/**
 * Mirrors RoleRoute `allowed` in frontend/src/routes/index.tsx.
 * Extend this table when new role-gated routes are added.
 */
const ROLE_ACCESS_MATRIX: RoleAccessRow[] = [
  {
    role: "mosque_admin",
    prepare: () => createMosqueAdminWithMosque(),
    cases: [
      { path: "/admin", allowed: true, heading: "Mosque command center" },
      { path: "/payments", allowed: true, heading: "Payments" },
      { path: "/mosque-admins", allowed: false },
    ],
  },
  {
    role: "teacher",
    prepare: async () => {
      const admin = await createMosqueAdminWithMosque();
      return createTeacherForMosque(admin);
    },
    cases: [
      { path: "/circles", allowed: true, heading: "Study circles" },
      { path: "/payments", allowed: false },
    ],
  },
  {
    role: "student",
    prepare: async () => {
      const admin = await createMosqueAdminWithMosque();
      return createStudentForMosque(admin);
    },
    cases: [
      { path: "/dashboard", allowed: true, heading: "Dashboard" },
      { path: "/admin", allowed: false },
    ],
  },
];

test.describe("Role access matrix", () => {
  for (const { role, prepare, cases } of ROLE_ACCESS_MATRIX) {
    test.describe(role, () => {
      for (const routeCase of cases) {
        const label = routeCase.allowed ? "allows" : "forbids";

        test(`${label} ${routeCase.path}`, async ({ page }) => {
          const user = await prepare();
          await loginViaUI(page, user.email, user.password, LOCALE);
          await waitForAssignedProfile(page);

          await assertRouteAccess(page, {
            locale: LOCALE,
            path: routeCase.path,
            allowed: routeCase.allowed,
            heading: routeCase.heading,
          });
        });
      }
    });
  }
});
