import { expect, test, type Page } from "@playwright/test";

import { E2E_PASSWORD, registerUser, uniqueEmail } from "../helpers/api.ts";
import { expectOnboardingRedirect } from "../helpers/auth.ts";

const LOCALE = "en";
const JOIN_INTENT_KEY = "darb.joinIntent";
const TEST_INVITE_CODE = "TESTCODE";
const JOIN_ROLE = "teacher";

function joinPath(
  code: string,
  role: string,
  locale: string = LOCALE,
): string {
  const params = new URLSearchParams({ code, role });
  return `/${locale}/join?${params}`;
}

async function clearBrowserStorage(page: Page): Promise<void> {
  await page.addInitScript(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
  });
}

async function readJoinIntent(
  page: Page,
): Promise<{ code: string; role: string } | null> {
  return page.evaluate((key) => {
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as { code: string; role: string };
    } catch {
      return null;
    }
  }, JOIN_INTENT_KEY);
}

test.describe("Join deep-link", () => {
  test.beforeEach(async ({ context, page }) => {
    await context.clearCookies();
    await clearBrowserStorage(page);
  });

  test("unauthenticated /en/join saves intent and redirects to login", async ({
    page,
  }) => {
    await page.goto(joinPath(TEST_INVITE_CODE, JOIN_ROLE));

    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/login$`));
    await expect(
      page.getByRole("heading", { name: "Sign in" }),
    ).toBeVisible();

    const intent = await readJoinIntent(page);
    expect(intent).toEqual({ code: TEST_INVITE_CODE, role: JOIN_ROLE });
  });

  test("after login, join intent pre-fills onboarding invite code", async ({
    page,
  }) => {
    const email = uniqueEmail("join-redirect");
    await registerUser({
      email,
      role: "teacher",
      fullName: "Join Redirect Teacher",
    });

    await page.goto(joinPath(TEST_INVITE_CODE, JOIN_ROLE));
    await expect(page).toHaveURL(new RegExp(`/${LOCALE}/login$`));

    // Stay on this tab: loginViaUI's page.goto would re-run storage-clearing init scripts.
    await page.getByLabel("Email").fill(email);
    await page.getByLabel("Password", { exact: true }).fill(E2E_PASSWORD);
    await page.getByRole("button", { name: "Sign in" }).click();
    await page.waitForURL((url) => !url.pathname.endsWith("/login"), {
      timeout: 30_000,
    });
    await page.waitForResponse(
      (res) =>
        res.url().includes("/api/v1/me/profile") &&
        (res.status() === 200 || res.status() === 404),
      { timeout: 15_000 },
    );
    await expectOnboardingRedirect(page, LOCALE);

    await expect(page.locator("#member-invite-code")).toHaveValue(
      TEST_INVITE_CODE,
    );
  });
});
