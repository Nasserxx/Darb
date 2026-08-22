import {
  createStudentProfile,
  createTeacherProfile,
  E2E_PASSWORD,
  loginUser,
  onboardMosque,
  registerUser,
  uniqueEmail,
  type AuthSession,
} from "./api.ts";

export type TestUser = {
  email: string;
  password: string;
  fullName: string;
  role: string;
  userId: string;
  accessToken: string;
};

export type MosqueAdminFixture = TestUser & {
  mosqueId: string;
};

export async function createMosqueAdminWithMosque(): Promise<MosqueAdminFixture> {
  const email = uniqueEmail("mosque-admin");
  const fullName = "E2E Mosque Admin";

  await registerUser({ email, fullName, role: "mosque_admin" });
  const session = await loginUser(email);

  const { mosqueId } = await onboardMosque(session.accessToken, {
    name: `E2E Mosque ${Date.now()}`,
    city: "Riyadh",
    addressState: "Riyadh Province",
  });

  return {
    email,
    password: E2E_PASSWORD,
    fullName,
    role: session.role,
    userId: session.userId,
    accessToken: session.accessToken,
    mosqueId,
  };
}

export async function createTeacherForMosque(
  admin: MosqueAdminFixture,
): Promise<TestUser> {
  const email = uniqueEmail("teacher");
  const fullName = "E2E Teacher";

  await registerUser({ email, fullName, role: "teacher" });
  const session = await loginUser(email);

  await createTeacherProfile(admin.accessToken, {
    userId: session.userId,
    mosqueId: admin.mosqueId,
  });

  return {
    email,
    password: E2E_PASSWORD,
    fullName,
    role: session.role,
    userId: session.userId,
    accessToken: session.accessToken,
  };
}

export async function createStudentForMosque(
  admin: MosqueAdminFixture,
): Promise<TestUser> {
  const email = uniqueEmail("student");
  const fullName = "E2E Student";

  await registerUser({ email, fullName, role: "student" });
  const session = await loginUser(email);

  await createStudentProfile(admin.accessToken, {
    userId: session.userId,
    mosqueId: admin.mosqueId,
  });

  return {
    email,
    password: E2E_PASSWORD,
    fullName,
    role: session.role,
    userId: session.userId,
    accessToken: session.accessToken,
  };
}

/** Student account without a mosque profile — triggers onboarding in the UI. */
export async function createBareStudent(): Promise<TestUser> {
  const email = uniqueEmail("student-bare");
  const fullName = "E2E Bare Student";

  await registerUser({ email, fullName, role: "student" });
  const session: AuthSession = await loginUser(email);

  return {
    email,
    password: E2E_PASSWORD,
    fullName,
    role: session.role,
    userId: session.userId,
    accessToken: session.accessToken,
  };
}
