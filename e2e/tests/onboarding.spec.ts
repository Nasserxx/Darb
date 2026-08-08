import { expect, test } from "@playwright/test";

import {
  loginUser,
  registerUser,
  seedMosqueWithInviteCode,
  uniqueEmail,
} from "../helpers/api.ts";
import {
  expectOnboardingRedirect,
  landingPathForRole,
  loginViaUI,
  registerViaUI,
} from "../helpers/auth.ts";

const LOCALE = "en";

test.describe("Onboarding", () => {
  test("existing mosque admin login skips onboarding and reaches admin", async ({
    page,
  }) => {
    const { founderEmail } = await seedMosqueWithInviteCode();
    await loginViaUI(page, founderEmail, undefined, LOCALE);
    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/admin`));
    await expect(
      page.getByRole("heading", { name: "Mosque command center" }),
    ).toBeVisible();
  });

  test("mosque admin registers, creates mosque, sees invite code, reaches admin dashboard", async ({
    page,
  }) => {
    const email = uniqueEmail("founder-ui");

    await registerViaUI(page, {
      email,
      role: "mosque_admin",
      fullName: "Founder Admin",
      locale: LOCALE,
    });
    await loginViaUI(page, email, undefined, LOCALE);
    await expectOnboardingRedirect(page, LOCALE);

    await expect(page.getByRole("tab", { name: "Create your mosque" })).toBeVisible();

    const mosqueName = `Founder Mosque ${Date.now()}`;
    await page.locator("#onboard-mosque-name").fill(mosqueName);
    await page.locator("#onboard-mosque-city").fill("Riyadh");
    await page.getByRole("button", { name: "Create mosque" }).click();

    await expect(page.getByText("Your mosque is ready.", { exact: true }).first()).toBeVisible();
    const inviteCode = (
      await page.locator('[data-slot="alert-description"].font-mono').innerText()
    ).trim();
    expect(inviteCode.length).toBeGreaterThanOrEqual(8);

    await page.getByRole("button", { name: "Continue to dashboard" }).click();
    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/admin`));
    await expect(
      page.getByRole("heading", { name: "Mosque command center" }),
    ).toBeVisible();
  });

  test("second mosque admin joins via invite code", async ({ page }) => {
    const { inviteCode, mosqueName } = await seedMosqueWithInviteCode();
    const joinerEmail = uniqueEmail("joiner-ui");

    await registerViaUI(page, {
      email: joinerEmail,
      role: "mosque_admin",
      fullName: "Joiner Admin",
      locale: LOCALE,
    });
    await loginViaUI(page, joinerEmail, undefined, LOCALE);
    await expectOnboardingRedirect(page, LOCALE);

    await page.getByRole("tab", { name: "Join with invite" }).click();
    await page.locator("#onboard-invite-code").fill(inviteCode);

    await expect(page.getByText("You are joining")).toBeVisible();
    await expect(page.getByText(mosqueName)).toBeVisible();

    await page.getByRole("button", { name: "Join mosque" }).click();
    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/admin`));
    await expect(
      page.getByRole("heading", { name: "Mosque command center" }),
    ).toBeVisible();
  });

  test("teacher joins via invite code and reaches circles", async ({ page }) => {
    const { teacherInviteCode, mosqueName } = await seedMosqueWithInviteCode();
    expect(teacherInviteCode).toBeTruthy();

    const email = uniqueEmail("teacher-ui");
    await registerViaUI(page, {
      email,
      role: "teacher",
      fullName: "Onboard Teacher",
      locale: LOCALE,
    });
    await loginViaUI(page, email, undefined, LOCALE);
    await expectOnboardingRedirect(page, LOCALE);

    await page.locator("#member-invite-code").fill(teacherInviteCode!);
    await expect(page.getByText("You are joining")).toBeVisible();
    await expect(page.getByText(mosqueName)).toBeVisible();
    await page.getByRole("button", { name: "Join mosque" }).click();

    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/circles`));
  });

  test("student joins via invite code and reaches dashboard", async ({ page }) => {
    const { studentInviteCode, mosqueName } = await seedMosqueWithInviteCode();
    expect(studentInviteCode).toBeTruthy();

    const email = uniqueEmail("student-ui");
    await registerViaUI(page, {
      email,
      role: "student",
      fullName: "Onboard Student",
      locale: LOCALE,
    });
    await loginViaUI(page, email, undefined, LOCALE);
    await expectOnboardingRedirect(page, LOCALE);

    await page.locator("#member-invite-code").fill(studentInviteCode!);
    await expect(page.getByText("You are joining")).toBeVisible();
    await expect(page.getByText(mosqueName)).toBeVisible();
    await page.getByRole("button", { name: "Join mosque" }).click();

    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/dashboard`));
  });

  test("existing teacher login skips onboarding", async ({ page, request }) => {
    const { teacherInviteCode } = await seedMosqueWithInviteCode();
    const email = uniqueEmail("assigned-teacher");
    await registerUser({ email, role: "teacher", fullName: "Assigned Teacher" });
    const session = await loginUser(email);
    const apiBase =
      process.env.API_URL ??
      process.env.API_BASE_URL ??
      process.env.VITE_API_URL ??
      "http://localhost:8089";
    await request.post(`${apiBase}/api/v1/teachers/join`, {
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
        "Content-Type": "application/json",
      },
      data: { inviteCode: teacherInviteCode },
    });

    await loginViaUI(page, email, undefined, LOCALE);
    await expect(page).toHaveURL(new RegExp(landingPathForRole("teacher", LOCALE)));
  });
});
