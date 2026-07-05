import { ApiError, apiFetch } from "@/lib/api-client.ts";
import type { ApiResponse } from "@/lib/types/api.ts";

import type { WorkspaceProfile } from "../context/workspace-provider.tsx";

const BASE_PATH = "/api/v1/me";

export const workspaceApi = {
  getProfile: async (): Promise<WorkspaceProfile | null> => {
    try {
      const response = await apiFetch<ApiResponse<WorkspaceProfile>>(
        `${BASE_PATH}/profile`,
        { auth: true },
      );
      return response.data ?? null;
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        return null;
      }
      throw error;
    }
  },
};
