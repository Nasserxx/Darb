export { teachersApi } from "./api/teachers-api.ts";
export {
  teacherKeys,
  useCreateTeacher,
  useDeleteTeacher,
  useTeacher,
  useTeachers,
  useUpdateTeacher,
} from "./hooks/index.ts";
export {
  teacherCreateSchema,
  toTeacherCreateRequestBody,
  type TeacherCreateFormValues,
  type TeacherCreateRequestBody,
} from "./schemas/teacher-create.schema.ts";
export {
  teacherUpdateSchema,
  toTeacherUpdateRequestBody,
  type TeacherUpdateFormValues,
  type TeacherUpdateRequestBody,
} from "./schemas/teacher-update.schema.ts";
export type {
  TeacherCreateRequest,
  TeacherResponse,
  TeacherUpdateRequest,
} from "./types/index.ts";
