import { test, expect, type Page } from "@playwright/test";

import { createTestEmail, TEST_PASSWORD } from "../helpers/api.ts";
import {
  dashboardPath,
  loginPath,
  loginViaBrowser,
} from "../helpers/auth.ts";

async function clearBrowserStorage(page: Page): Promise<void> {
  await page.addInitScript(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });
}

async function registerStudentViaUi(
  page: Page,
  email: string,
  fullName = "E2E Student",
): Promise<void> {
  await page.goto("/en/register");
  await page.getByRole("heading", { name: "Create account" }).waitFor();

  await page.getByLabel("Full name").fill(fullName);
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Password", { exact: true }).fill(TEST_PASSWORD);
  await page.getByLabel("Confirm password").fill(TEST_PASSWORD);
  await page.getByRole("button", { name: "Create account" }).click();

  await page.waitForURL(/\/en\/login/);
}

test.describe("Auth pages", () => {
  test("login page renders heading, email, and password fields", async ({
    page,
  }) => {
    await page.goto(loginPath());

    await expect(
      page.getByRole("heading", { name: "Sign in" }),
    ).toBeVisible();
    await expect(page.getByLabel("Email")).toBeVisible();
    await expect(page.getByLabel("Password", { exact: true })).toBeVisible();
  });

  test("register page renders all fields", async ({ page }) => {
    await page.goto("/en/register");

    await expect(
      page.getByRole("heading", { name: "Create account" }),
    ).toBeVisible();
    await expect(page.getByLabel("Full name")).toBeVisible();
    await expect(page.getByLabel("Email", { exact: true })).toBeVisible();
    await expect(page.getByLabel(/Phone/)).toBeVisible();
    await expect(page.getByLabel("Password", { exact: true })).toBeVisible();
    await expect(page.getByLabel("Confirm password")).toBeVisible();
    await expect(page.getByLabel("Role")).toBeVisible();
    await expect(page.getByLabel(/Gender/)).toBeVisible();
    await expect(page.getByLabel(/Date of birth/)).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Create account" }),
    ).toBeVisible();
  });
});

test.describe("Auth redirects and access control", () => {
  test.beforeEach(async ({ context, page }) => {
    await context.clearCookies();
    await clearBrowserStorage(page);
  });

  test("unauthenticated /en/dashboard redirects to /en/login", async ({
    page,
  }) => {
    await page.goto(dashboardPath());

    await expect(page).toHaveURL(/\/en\/login$/);
  });

  test("register new student via UI, then login", async ({ page }) => {
    const email = createTestEmail("student");

    await registerStudentViaUi(page, email);
    await expect(page.getByLabel("Email")).toHaveValue(email);

    await loginViaBrowser(page, email, { expectDashboard: false });
    await expect(page).toHaveURL(/\/en\/(dashboard|onboarding)/);
  });

  test("invalid login shows error instead of silent failure", async ({
    page,
  }) => {
    await page.goto(loginPath());

    await page.getByLabel("Email").fill(createTestEmail("invalid"));
    await page.getByLabel("Password", { exact: true }).fill("wrong-password-xyz");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page.getByText("Invalid email or password.")).toBeVisible();
    await expect(page).toHaveURL(/\/en\/login/);
  });

  test("guest cannot access protected route", async ({ page }) => {
    await page.goto("/en/onboarding");

    await expect(page).toHaveURL(/\/en\/login/);
  });
});

test.describe("Auth locale", () => {
  test.beforeEach(async ({ context, page }) => {
    await context.clearCookies();
    await clearBrowserStorage(page);
  });

  test("locale /ar/login has dir=rtl", async ({ page }) => {
    await page.goto(loginPath("ar"));

    await expect(page.locator("html")).toHaveAttribute("dir", "rtl");
  });

  test("locale /de/login has German heading", async ({ page }) => {
    await page.goto(loginPath("de"));

    await expect(
      page.getByRole("heading", { name: "Anmelden" }),
    ).toBeVisible();
  });
});
