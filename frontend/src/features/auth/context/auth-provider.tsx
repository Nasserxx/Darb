import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import {
  changePassword as changePasswordApi,
  login as loginApi,
  logout as logoutApi,
  refresh as refreshApi,
  register as registerApi,
} from "../api/auth-api.ts";
import type { ChangePasswordRequestBody } from "../schemas/change-password.schema.ts";
import type { LoginRequestBody } from "../schemas/login.schema.ts";
import type { RegisterRequestBody } from "../schemas/register.schema.ts";
import {
  clearSession,
  getUserSession,
  subscribeToSync,
} from "../session/storage.ts";
import type { UserSession } from "../types/index.ts";
import { ApiError } from "../../../lib/api-client.ts";
import { normalizeRole } from "../utils/role.ts";

const DEFAULT_EXPIRES_IN_SEC = 900;
const REFRESH_LEAD_MS = 60_000;

export type AuthActionResult =
  | { ok: true }
  | { ok: false; message: string; fieldErrors?: Record<string, string> };

export interface AuthContextValue {
  user: UserSession | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (body: LoginRequestBody) => Promise<AuthActionResult>;
  logout: () => void;
  register: (body: RegisterRequestBody) => Promise<AuthActionResult>;
  changePassword: (body: ChangePasswordRequestBody) => Promise<AuthActionResult>;
}

// Context lives with provider; fast-refresh rule waived for co-located hook contract.
// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null);

function toActionError(error: unknown): AuthActionResult {
  if (error instanceof ApiError) {
    const fieldErrors = extractFieldErrors(error.body);
    return {
      ok: false,
      message: error.message,
      ...(fieldErrors ? { fieldErrors } : {}),
    };
  }
  if (error instanceof Error) {
    return { ok: false, message: error.message };
  }
  return { ok: false, message: "An unexpected error occurred" };
}

function extractFieldErrors(
  body: unknown,
): Record<string, string> | undefined {
  if (!body || typeof body !== "object" || !("data" in body)) {
    return undefined;
  }
  const data = (body as { data?: unknown }).data;
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return undefined;
  }
  const entries = Object.entries(data).filter(
    (entry): entry is [string, string] => typeof entry[1] === "string",
  );
  return entries.length > 0 ? Object.fromEntries(entries) : undefined;
}

function withNormalizedRole(session: UserSession): UserSession {
  return { ...session, role: normalizeRole(session.role) };
}

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<UserSession | null>(() => {
    const session = getUserSession();
    return session ? withNormalizedRole(session) : null;
  });
  const [isLoading, setIsLoading] = useState(false);

  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const refreshInFlightRef = useRef<Promise<boolean> | null>(null);
  const accessExpiresAtRef = useRef<number | null>(null);
  const refreshSessionRef = useRef<() => Promise<boolean>>(() =>
    Promise.resolve(false),
  );

  const clearRefreshTimer = useCallback(() => {
    if (refreshTimerRef.current !== null) {
      clearTimeout(refreshTimerRef.current);
      refreshTimerRef.current = null;
    }
  }, []);

  const applySession = useCallback(
    (session: UserSession, expiresInSec = DEFAULT_EXPIRES_IN_SEC) => {
      const normalized = withNormalizedRole(session);
      setUser(normalized);
      accessExpiresAtRef.current = Date.now() + expiresInSec * 1000;
      return normalized;
    },
    [],
  );

  const scheduleProactiveRefresh = useCallback(
    (expiresInSec = DEFAULT_EXPIRES_IN_SEC) => {
      clearRefreshTimer();
      const expiresAt =
        accessExpiresAtRef.current ?? Date.now() + expiresInSec * 1000;
      const delay = Math.max(expiresAt - Date.now() - REFRESH_LEAD_MS, 0);

      refreshTimerRef.current = setTimeout(() => {
        void refreshSessionRef.current();
      }, delay);
    },
    [clearRefreshTimer],
  );

  const refreshSession = useCallback(async (): Promise<boolean> => {
    if (refreshInFlightRef.current) {
      return refreshInFlightRef.current;
    }

    const promise = (async () => {
      try {
        const session = await refreshApi();
        const expiresIn = session.expiresIn ?? DEFAULT_EXPIRES_IN_SEC;
        applySession(session, expiresIn);
        scheduleProactiveRefresh(expiresIn);
        return true;
      } catch {
        clearRefreshTimer();
        clearSession();
        setUser(null);
        accessExpiresAtRef.current = null;
        return false;
      }
    })();

    refreshInFlightRef.current = promise;
    try {
      return await promise;
    } finally {
      refreshInFlightRef.current = null;
    }
  }, [applySession, clearRefreshTimer, scheduleProactiveRefresh]);

  useEffect(() => {
    refreshSessionRef.current = refreshSession;
  }, [refreshSession]);

  const logout = useCallback(() => {
    void logoutApi()
      .catch(() => {})
      .finally(() => {
        clearRefreshTimer();
        clearSession();
        setUser(null);
        accessExpiresAtRef.current = null;
      });
  }, [clearRefreshTimer]);

  const login = useCallback(
    async (body: LoginRequestBody): Promise<AuthActionResult> => {
      setIsLoading(true);
      try {
        const session = await loginApi(body);
        const expiresIn = session.expiresIn ?? DEFAULT_EXPIRES_IN_SEC;
        applySession(session, expiresIn);
        scheduleProactiveRefresh(expiresIn);
        return { ok: true };
      } catch (error) {
        return toActionError(error);
      } finally {
        setIsLoading(false);
      }
    },
    [applySession, scheduleProactiveRefresh],
  );

  const register = useCallback(
    async (body: RegisterRequestBody): Promise<AuthActionResult> => {
      setIsLoading(true);
      try {
        await registerApi(body);
        return { ok: true };
      } catch (error) {
        return toActionError(error);
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  const changePassword = useCallback(
    async (body: ChangePasswordRequestBody): Promise<AuthActionResult> => {
      setIsLoading(true);
      try {
        await changePasswordApi(body);
        return { ok: true };
      } catch (error) {
        return toActionError(error);
      } finally {
        setIsLoading(false);
      }
    },
    [],
  );

  useEffect(() => {
    if (user) {
      const expiresIn = user.expiresIn ?? DEFAULT_EXPIRES_IN_SEC;
      if (!accessExpiresAtRef.current) {
        accessExpiresAtRef.current = Date.now() + expiresIn * 1000;
      }
      scheduleProactiveRefresh(expiresIn);
    }

    return () => {
      clearRefreshTimer();
    };
  }, [user, scheduleProactiveRefresh, clearRefreshTimer]);

  // Sync session changes from other tabs via BroadcastChannel
  useEffect(() => {
    const unsubscribe = subscribeToSync((session) => {
      if (session) {
        const normalized = withNormalizedRole(session);
        setUser(normalized);
        accessExpiresAtRef.current =
          Date.now() + (session.expiresIn ?? DEFAULT_EXPIRES_IN_SEC) * 1000;
        scheduleProactiveRefresh(session.expiresIn ?? DEFAULT_EXPIRES_IN_SEC);
      } else {
        clearRefreshTimer();
        setUser(null);
        accessExpiresAtRef.current = null;
      }
    });
    return unsubscribe;
  }, [clearRefreshTimer, scheduleProactiveRefresh]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isLoading,
      login,
      logout,
      register,
      changePassword,
    }),
    [user, isLoading, login, logout, register, changePassword],
  );

  return (
    <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
  );
}
