import { expect, type Page } from "@playwright/test";

import { E2E_PASSWORD, getWorkspaceProfile, type RegisterRole } from "./api.ts";

const DEFAULT_LOCALE = "en";

const ROLE_LABELS: Record<RegisterRole, string> = {
  student: "Student",
  teacher: "Teacher",
  parent: "Parent",
  mosque_admin: "Mosque administrator",
};

export function loginPath(locale: string = DEFAULT_LOCALE): string {
  return `/${locale}/login`;
}

export function dashboardPath(locale: string = DEFAULT_LOCALE): string {
  return `/${locale}/dashboard`;
}

export function onboardingPath(locale: string = DEFAULT_LOCALE): string {
  return `/${locale}/onboarding`;
}

export function landingPathForRole(
  role: RegisterRole,
  locale: string = DEFAULT_LOCALE,
): string {
  switch (role) {
    case "mosque_admin":
      return `/${locale}/admin`;
    case "teacher":
      return `/${locale}/circles`;
    case "student":
      return `/${locale}/dashboard`;
    default:
      return `/${locale}/dashboard`;
  }
}

export async function loginViaUI(
  page: Page,
  email: string,
  password: string = E2E_PASSWORD,
  locale: string = DEFAULT_LOCALE,
): Promise<void> {
  await page.goto(loginPath(locale));
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL((url) => !url.pathname.endsWith("/login"), {
    timeout: 30_000,
  });
  // ponytail: wait for workspace profile API instead of fixed sleep
  await page.waitForResponse(
    (res) =>
      res.url().includes("/api/v1/me/profile") &&
      (res.status() === 200 || res.status() === 404),
    { timeout: 15_000 },
  );
}

export async function loginViaBrowser(
  page: Page,
  email: string,
  options: { password?: string; locale?: string; expectDashboard?: boolean } = {},
): Promise<void> {
  const locale = options.locale ?? DEFAULT_LOCALE;
  const password = options.password ?? E2E_PASSWORD;
  const expectDashboard = options.expectDashboard ?? true;

  await loginViaUI(page, email, password, locale);

  if (expectDashboard) {
    await expect(page).toHaveURL(new RegExp(`/${locale}/(dashboard|onboarding|admin)`));
  }
}

export async function registerViaUI(
  page: Page,
  options: {
    email: string;
    password?: string;
    role: RegisterRole;
    fullName?: string;
    locale?: string;
  },
): Promise<void> {
  const locale = options.locale ?? DEFAULT_LOCALE;
  const password = options.password ?? E2E_PASSWORD;

  await page.goto(`/${locale}/register`);
  await page.getByRole("heading", { name: "Create account" }).waitFor();

  await page.getByLabel("Full name").fill(options.fullName ?? "E2E User");
  await page.getByLabel("Email", { exact: true }).fill(options.email);
  await page.getByLabel("Password", { exact: true }).fill(password);
  await page.getByLabel("Confirm password").fill(password);

  await page.getByLabel("Role").click();
  await page.getByRole("option", { name: ROLE_LABELS[options.role] }).click();

  await page.getByRole("button", { name: "Create account" }).click();
  await expect(page).toHaveURL(new RegExp(`/${locale}/login`));
}

export async function waitPastOnboarding(
  page: Page,
  locale: string = DEFAULT_LOCALE,
): Promise<void> {
  await expect(page).not.toHaveURL(new RegExp(`/${locale}/onboarding`), {
    timeout: 20_000,
  });
}

export async function expectOnboardingRedirect(
  page: Page,
  locale: string = DEFAULT_LOCALE,
): Promise<void> {
  await expect(page).toHaveURL(new RegExp(`/${locale}/onboarding`), {
    timeout: 15_000,
  });
}

/** Poll workspace profile until mosque membership is ASSIGNED (post-login). */
export async function waitForAssignedProfile(
  page: Page,
  options: { timeout?: number; intervalMs?: number } = {},
): Promise<void> {
  const timeout = options.timeout ?? 30_000;
  const intervalMs = options.intervalMs ?? 500;
  const deadline = Date.now() + timeout;

  while (Date.now() < deadline) {
    const token = await page.evaluate(() =>
      sessionStorage.getItem("darb.accessToken"),
    );
    if (token) {
      const profile = await getWorkspaceProfile(token);
      if (profile?.membershipStatus === "ASSIGNED") {
        return;
      }
    }
    await page.waitForTimeout(intervalMs);
  }

  throw new Error(`Timed out waiting for ASSIGNED workspace profile (${timeout}ms)`);
}

export function forbiddenPath(locale: string = DEFAULT_LOCALE): string {
  return `/${locale}/forbidden`;
}

export async function assertRouteAccess(
  page: Page,
  options: {
    locale?: string;
    path: string;
    allowed: boolean;
    heading?: string;
  },
): Promise<void> {
  const locale = options.locale ?? DEFAULT_LOCALE;
  const route = `/${locale}${options.path}`;

  await page.goto(route);

  if (options.allowed) {
    const escaped = options.path.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    await expect(page).toHaveURL(new RegExp(`/${locale}${escaped}(/)?$`));
    if (options.heading) {
      await expect(
        page.getByRole("heading", { name: options.heading }),
      ).toBeVisible();
    }
  } else {
    await expect(page).toHaveURL(new RegExp(`/${locale}/forbidden`));
    await expect(
      page.getByRole("heading", { name: "Access denied" }),
    ).toBeVisible();
  }
}
