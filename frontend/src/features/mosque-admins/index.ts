export { mosqueAdminsApi } from "./api/mosque-admins-api.ts";
export {
  mosqueAdminKeys,
  useCreateMosqueAdmin,
  useDeleteMosqueAdmin,
  useMosqueAdmin,
  useMosqueAdmins,
  useUpdateMosqueAdmin,
} from "./hooks/index.ts";
export {
  mosqueAdminCreateSchema,
  toMosqueAdminCreateRequestBody,
  type MosqueAdminCreateFormValues,
  type MosqueAdminCreateRequestBody,
} from "./schemas/mosque-admin-create.schema.ts";
export {
  mosqueAdminUpdateSchema,
  toMosqueAdminUpdateRequestBody,
  type MosqueAdminUpdateFormValues,
  type MosqueAdminUpdateRequestBody,
} from "./schemas/mosque-admin-update.schema.ts";
export type {
  AdminPermission,
  MosqueAdminCreateRequest,
  MosqueAdminResponse,
  MosqueAdminUpdateRequest,
} from "./types/index.ts";
