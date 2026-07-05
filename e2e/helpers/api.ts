const API_BASE_URL =
  process.env.API_URL ??
  process.env.API_BASE_URL ??
  process.env.VITE_API_URL ??
  "http://localhost:8080";

export const E2E_PASSWORD = "P@ssw0rd1!";
export const TEST_PASSWORD = E2E_PASSWORD;

export type RegisterRole = "student" | "teacher" | "parent" | "mosque_admin";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data?: T;
};

export type AuthSession = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: string;
  email: string;
};

type OnboardData = {
  inviteCode?: string;
  teacherInviteCode?: string;
  studentInviteCode?: string;
  mosque: { id: string; name: string; city?: string };
};

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@e2e.test.darb`;
}

/** Alias used by auth.spec.ts */
export function createTestEmail(prefix: string): string {
  return uniqueEmail(prefix);
}

async function request<T>(
  path: string,
  init: RequestInit & { token?: string } = {},
): Promise<ApiResponse<T>> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (init.token) {
    headers.set("Authorization", `Bearer ${init.token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  const text = await response.text();
  const body = (
    text ? JSON.parse(text) : { success: response.ok, message: response.statusText }
  ) as ApiResponse<T>;
  if (!response.ok) {
    throw new Error(`${response.status} ${body.message ?? response.statusText}`);
  }
  return body;
}

export async function registerUser(options: {
  email: string;
  password?: string;
  role: RegisterRole;
  fullName?: string;
}): Promise<void> {
  const response = await request<void>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify({
      fullName: options.fullName ?? "E2E User",
      email: options.email,
      password: options.password ?? E2E_PASSWORD,
      role: options.role,
    }),
  });
  if (!response.success) {
    throw new Error(response.message || "Registration failed");
  }
}

export async function loginUser(
  email: string,
  password: string = E2E_PASSWORD,
): Promise<AuthSession> {
  const response = await request<AuthSession>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  if (!response.success || !response.data?.accessToken) {
    throw new Error(response.message || "Login failed");
  }
  return response.data;
}

export async function onboardMosque(
  token: string,
  body: { name: string; city?: string; timezone?: string },
): Promise<{
  mosqueId: string;
  inviteCode?: string;
  teacherInviteCode?: string;
  studentInviteCode?: string;
  mosqueName: string;
  city?: string;
}> {
  const response = await request<OnboardData>("/api/v1/mosques/onboard", {
    method: "POST",
    token,
    body: JSON.stringify(body),
  });
  if (!response.success || !response.data?.mosque?.id) {
    throw new Error(response.message || "Onboard failed");
  }
  return {
    mosqueId: response.data.mosque.id,
    inviteCode: response.data.inviteCode,
    teacherInviteCode: response.data.teacherInviteCode,
    studentInviteCode: response.data.studentInviteCode,
    mosqueName: response.data.mosque.name,
    city: response.data.mosque.city,
  };
}

export async function createTeacherProfile(
  adminToken: string,
  body: { userId: string; mosqueId: string },
): Promise<void> {
  const response = await request<unknown>("/api/v1/teachers", {
    method: "POST",
    token: adminToken,
    body: JSON.stringify(body),
  });
  if (!response.success) {
    throw new Error(response.message || "Teacher profile creation failed");
  }
}

export async function createStudentProfile(
  adminToken: string,
  body: { userId: string; mosqueId: string },
): Promise<void> {
  const response = await request<unknown>("/api/v1/students", {
    method: "POST",
    token: adminToken,
    body: JSON.stringify(body),
  });
  if (!response.success) {
    throw new Error(response.message || "Student profile creation failed");
  }
}

export type WorkspaceProfileData = {
  profileId?: string;
  mosqueId?: string | null;
  membershipStatus?: "ASSIGNED" | "PENDING" | "NONE";
};

export async function getWorkspaceProfile(
  token: string,
): Promise<WorkspaceProfileData | null> {
  try {
    const response = await request<WorkspaceProfileData>("/api/v1/me/profile", {
      token,
    });
    return response.data ?? null;
  } catch {
    return null;
  }
}

/** Seeds a founder mosque via API and returns the admin invite code for join flows. */
export async function seedMosqueWithInviteCode(options?: {
  mosqueName?: string;
  city?: string;
}): Promise<{
  founderEmail: string;
  inviteCode: string;
  teacherInviteCode?: string;
  studentInviteCode?: string;
  mosqueName: string;
  city: string;
}> {
  const mosqueName = options?.mosqueName ?? `E2E Mosque ${Date.now()}`;
  const city = options?.city ?? "Riyadh";
  const founderEmail = uniqueEmail("founder");

  await registerUser({
    email: founderEmail,
    role: "mosque_admin",
    fullName: "E2E Founder",
  });
  const session = await loginUser(founderEmail);
  const onboarded = await onboardMosque(session.accessToken, { name: mosqueName, city });

  if (!onboarded.inviteCode) {
    throw new Error("Expected invite code after mosque onboarding");
  }

  return {
    founderEmail,
    inviteCode: onboarded.inviteCode,
    teacherInviteCode: onboarded.teacherInviteCode,
    studentInviteCode: onboarded.studentInviteCode,
    mosqueName,
    city,
  };
}
