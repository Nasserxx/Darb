export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  fullName: string;
  role: string;
  email: string;
}

export interface UserSession {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  fullName: string;
  role: string;
  email: string;
}

export type RegisterRole = "student" | "teacher" | "parent" | "mosque_admin";

export type Gender = "MALE" | "FEMALE";
