export { mosquesApi } from "./api/mosques-api.ts";
export {
  mosqueKeys,
  useCreateMosque,
  useDeleteMosque,
  useMosque,
  useMosques,
  useUpdateMosque,
} from "./hooks/index.ts";
export {
  mosqueCreateSchema,
  toMosqueCreateRequestBody,
  type MosqueCreateFormValues,
  type MosqueCreateRequestBody,
} from "./schemas/mosque-create.schema.ts";
export {
  mosqueUpdateSchema,
  toMosqueUpdateRequestBody,
  type MosqueUpdateFormValues,
  type MosqueUpdateRequestBody,
} from "./schemas/mosque-update.schema.ts";
export type {
  MosqueCreateRequest,
  MosqueResponse,
  MosqueUpdateRequest,
} from "./types/index.ts";
