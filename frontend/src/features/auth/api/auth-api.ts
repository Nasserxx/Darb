import { apiFetch } from "../../../lib/api-client.ts";
import type { ApiResponse, AuthResponse } from "../types/index.ts";
import type { ChangePasswordRequestBody } from "../schemas/change-password.schema.ts";
import type { LoginRequestBody } from "../schemas/login.schema.ts";
import type { RegisterRequestBody } from "../schemas/register.schema.ts";
import {
  getRefreshToken,
  setSessionFromAuthResponse,
} from "../session/storage.ts";

export async function register(body: RegisterRequestBody): Promise<void> {
  await apiFetch<void>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function login(body: LoginRequestBody): Promise<AuthResponse> {
  const response = await apiFetch<ApiResponse<AuthResponse>>(
    "/api/v1/auth/login",
    {
      method: "POST",
      body: JSON.stringify(body),
    },
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || "Login failed");
  }

  return setSessionFromAuthResponse(response.data);
}

export async function refresh(
  refreshToken?: string,
): Promise<AuthResponse> {
  const token = refreshToken ?? getRefreshToken();
  if (!token) {
    throw new Error("No refresh token available");
  }

  const response = await apiFetch<ApiResponse<AuthResponse>>(
    "/api/v1/auth/refresh",
    {
      method: "POST",
      body: JSON.stringify({ refreshToken: token }),
    },
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || "Token refresh failed");
  }

  return setSessionFromAuthResponse(response.data);
}

export async function changePassword(
  body: ChangePasswordRequestBody,
): Promise<void> {
  await apiFetch<ApiResponse<void>>("/api/v1/auth/change-password", {
    method: "POST",
    auth: true,
    body: JSON.stringify(body),
  });
}

export async function logout(): Promise<void> {
  await apiFetch<ApiResponse<void>>("/api/v1/auth/logout", {
    method: "POST",
    auth: true,
  });
}
