import type { AuthResponse, UserSession } from "../types/index.ts";

const ACCESS_TOKEN_KEY = "darb.accessToken";
const REFRESH_TOKEN_KEY = "darb.refreshToken";
const USER_KEY = "darb.user";

// BroadcastChannel for cross-tab sync
const SYNC_CHANNEL = "darb:auth";
let channel: BroadcastChannel | null = null;

function getChannel(): BroadcastChannel | null {
  if (typeof window === "undefined") return null;
  if (!channel) {
    try {
      channel = new BroadcastChannel(SYNC_CHANNEL);
    } catch {
      return null;
    }
  }
  return channel;
}

type SyncMessage =
  | { type: "SESSION_UPDATED"; session: UserSession }
  | { type: "SESSION_CLEARED" };

function broadcast(msg: SyncMessage): void {
  const ch = getChannel();
  if (ch) {
    ch.postMessage(msg);
  }
}

function readJson<T>(key: string): T | null {
  const raw = localStorage.getItem(key);
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
  localStorage.setItem(key, JSON.stringify(value));
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
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
  broadcast({ type: "SESSION_UPDATED", session });
  return session;
}

export function clearSession(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  broadcast({ type: "SESSION_CLEARED" });
}

export function subscribeToSync(
  onUpdate: (session: UserSession | null) => void,
): () => void {
  const ch = getChannel();
  if (!ch) return () => {};

  const handler = (event: MessageEvent<SyncMessage>) => {
    if (event.data.type === "SESSION_UPDATED") {
      const session = event.data.session;
      // Update localStorage from the synced session
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
      onUpdate(session);
    } else if (event.data.type === "SESSION_CLEARED") {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      onUpdate(null);
    }
  };

  ch.addEventListener("message", handler);
  return () => ch.removeEventListener("message", handler);
}
