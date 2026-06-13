export { AuthProvider, type AuthActionResult, type AuthContextValue } from "./context/auth-provider.tsx";
export { useAuth } from "./hooks/use-auth.ts";
export type {
  ApiResponse,
  AuthResponse,
  Gender,
  RegisterRole,
  UserSession,
} from "./types/index.ts";
export type { LoginRequestBody } from "./schemas/login.schema.ts";
export type { RegisterRequestBody } from "./schemas/register.schema.ts";
export type { ChangePasswordRequestBody } from "./schemas/change-password.schema.ts";
export { normalizeRole, type RoleKey } from "./utils/role.ts";
export { login, register, refresh, changePassword } from "./api/auth-api.ts";
