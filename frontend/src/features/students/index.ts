export { studentsApi } from "./api/students-api.ts";
export {
  studentKeys,
  useCreateStudent,
  useDeleteStudent,
  useStudent,
  useStudents,
  useUpdateStudent,
} from "./hooks/index.ts";
export {
  studentCreateSchema,
  studentUpdateSchema,
  type StudentCreateFormValues,
  type StudentUpdateFormValues,
} from "./schemas/student.schema.ts";
export type {
  StudentCreateRequest,
  StudentResponse,
  StudentUpdateRequest,
} from "./types/index.ts";
