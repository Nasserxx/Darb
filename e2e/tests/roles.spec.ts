import { expect, test } from "@playwright/test";

import { loginViaUI, waitPastOnboarding } from "../helpers/auth.ts";
import { MOSQUE_ADMIN_REQUIRED_NAV } from "../helpers/nav.ts";
import {
  createBareStudent,
  createMosqueAdminWithMosque,
  createStudentForMosque,
  createTeacherForMosque,
} from "../helpers/users.ts";

const LOCALE = "en";

test.describe("Role matrix", () => {
  test("mosque_admin can access /en/admin and /en/students", async ({ page }) => {
    const admin = await createMosqueAdminWithMosque();
    await loginViaUI(page, admin.email, admin.password, LOCALE);
    await waitPastOnboarding(page, LOCALE);

    await page.goto(`/${LOCALE}/admin`);
    await expect(
      page.getByRole("heading", { name: "Mosque command center" }),
    ).toBeVisible();

    await page.goto(`/${LOCALE}/students`);
    await expect(page.getByRole("heading", { name: "Students" })).toBeVisible();
  });

  test("mosque_admin is redirected from /en/mosque-admins to /en/forbidden", async ({
    page,
  }) => {
    const admin = await createMosqueAdminWithMosque();
    await loginViaUI(page, admin.email, admin.password, LOCALE);
    await waitPastOnboarding(page, LOCALE);

    await page.goto(`/${LOCALE}/mosque-admins`);
    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/forbidden`));
    await expect(page.getByRole("heading", { name: "Access denied" })).toBeVisible();
  });

  test("teacher can access /en/circles", async ({ page }) => {
    const admin = await createMosqueAdminWithMosque();
    const teacher = await createTeacherForMosque(admin);
    await loginViaUI(page, teacher.email, teacher.password, LOCALE);
    await waitPastOnboarding(page, LOCALE);

    await page.goto(`/${LOCALE}/circles`);
    await expect(page.getByRole("heading", { name: "Study circles" })).toBeVisible();
  });

  test("student lands on dashboard or onboarding after login", async ({ page }) => {
    const admin = await createMosqueAdminWithMosque();

    const onboardedStudent = await createStudentForMosque(admin);
    await loginViaUI(page, onboardedStudent.email, onboardedStudent.password, LOCALE);
    await expect(page).toHaveURL(
      new RegExp(`/${LOCALE}/(dashboard|onboarding)(/)?$`),
    );

    await page.context().clearCookies();
    await page.evaluate(() => sessionStorage.clear());

    const bareStudent = await createBareStudent();
    await loginViaUI(page, bareStudent.email, bareStudent.password, LOCALE);
    await expect(page).toHaveURL(
      new RegExp(`/${LOCALE}/(dashboard|onboarding)(/)?$`),
    );
  });

  test("mosque_admin sidebar shows Dashboard, Students, and Circles", async ({
    page,
  }) => {
    const admin = await createMosqueAdminWithMosque();
    await loginViaUI(page, admin.email, admin.password, LOCALE);
    await waitPastOnboarding(page, LOCALE);
    await page.goto(`/${LOCALE}/admin`);

    for (const label of MOSQUE_ADMIN_REQUIRED_NAV) {
      await expect(page.getByRole("link", { name: label })).toBeVisible();
    }
  });
});
