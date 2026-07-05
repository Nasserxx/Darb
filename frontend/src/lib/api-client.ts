import type { ApiResponse } from "./types/api.ts";
import type { AuthResponse } from "../features/auth/types/index.ts";
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  setSessionFromAuthResponse,
} from "../features/auth/session/storage.ts";

export type { ApiResponse } from "./types/api.ts";

const API_BASE_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export interface ApiFetchOptions extends Omit<RequestInit, "headers"> {
  headers?: HeadersInit;
  /** Attach Bearer access token from session storage. */
  auth?: boolean;
  /** Skip automatic refresh-and-retry on 401 (used internally). */
  skipRefresh?: boolean;
}

let refreshInFlight: Promise<AuthResponse | null> | null = null;

async function readResponseText(response: Response): Promise<string> {
  return response.text();
}

function parseJsonBody<T>(text: string): T | undefined {
  if (!text) {
    return undefined;
  }
  return JSON.parse(text) as T;
}

function extractErrorMessage(body: unknown, fallback: string): string {
  if (
    body &&
    typeof body === "object" &&
    "message" in body &&
    typeof (body as ApiResponse<unknown>).message === "string"
  ) {
    return (body as ApiResponse<unknown>).message;
  }
  return fallback;
}

async function refreshTokens(): Promise<AuthResponse | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearSession();
    return null;
  }

  const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  const text = await readResponseText(response);
  const body = parseJsonBody<ApiResponse<AuthResponse>>(text);

  if (!response.ok || !body?.success || !body.data) {
    clearSession();
    return null;
  }

  setSessionFromAuthResponse(body.data);
  return body.data;
}

function singleFlightRefresh(): Promise<AuthResponse | null> {
  if (!refreshInFlight) {
    refreshInFlight = refreshTokens().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

function buildHeaders(
  initHeaders: HeadersInit | undefined,
  auth: boolean,
  accessToken?: string,
): Headers {
  const headers = new Headers(initHeaders);
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (auth && accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  return headers;
}

function resolveUrl(path: string): string {
  return path.startsWith("http") ? path : `${API_BASE_URL}${path}`;
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const { auth = false, skipRefresh = false, headers: initHeaders, ...init } =
    options;

  const url = resolveUrl(path);
  let accessToken = auth ? getAccessToken() : null;

  const doRequest = (token: string | null) =>
    fetch(url, {
      ...init,
      headers: buildHeaders(initHeaders, auth, token ?? undefined),
    });

  let response = await doRequest(accessToken);

  if (response.status === 401 && auth && !skipRefresh) {
    const refreshed = await singleFlightRefresh();
    if (refreshed) {
      accessToken = refreshed.accessToken;
      response = await doRequest(accessToken);
    } else {
      throw new ApiError(401, "Session expired");
    }
  }

  const text = await readResponseText(response);
  const body = parseJsonBody<T | ApiResponse<unknown>>(text);

  if (!response.ok) {
    throw new ApiError(
      response.status,
      extractErrorMessage(body, response.statusText),
      body,
    );
  }

  return (body ?? (undefined as T)) as T;
}
