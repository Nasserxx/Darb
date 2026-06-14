import type { AuthResponse, UserSession } from "../types/index.ts";

const ACCESS_TOKEN_KEY = "darb.accessToken";
const REFRESH_TOKEN_KEY = "darb.refreshToken";
const USER_KEY = "darb.user";

function readJson<T>(key: string): T | null {
  const raw = sessionStorage.getItem(key);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function writeJson(key: string, value: unknown): void {
  sessionStorage.setItem(key, JSON.stringify(value));
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getRefreshToken(): string | null {
  return sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string): void {
  sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
}

export function getStoredUser(): Omit<
  UserSession,
  "accessToken" | "refreshToken"
> | null {
  return readJson<Omit<UserSession, "accessToken" | "refreshToken">>(USER_KEY);
}

export function getUserSession(): UserSession | null {
  const accessToken = getAccessToken();
  const refreshToken = getRefreshToken();
  const user = getStoredUser();
  if (!accessToken || !refreshToken || !user) {
    return null;
  }
  return { ...user, accessToken, refreshToken };
}

export function setSessionFromAuthResponse(auth: AuthResponse): UserSession {
  const session: UserSession = {
    accessToken: auth.accessToken,
    refreshToken: auth.refreshToken,
    tokenType: auth.tokenType,
    expiresIn: auth.expiresIn,
    userId: auth.userId,
    fullName: auth.fullName,
    role: auth.role,
    email: auth.email,
  };
  setAccessToken(session.accessToken);
  setRefreshToken(session.refreshToken);
  writeJson(USER_KEY, {
    tokenType: session.tokenType,
    expiresIn: session.expiresIn,
    userId: session.userId,
    fullName: session.fullName,
    role: session.role,
    email: session.email,
  });
  return session;
}

export function clearSession(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
}
