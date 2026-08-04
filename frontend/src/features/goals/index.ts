export { goalsApi } from "./api/goals-api.ts";
export { goalKeys } from "./hooks/goal-keys.ts";
export {
  useCreateGoal,
  useGoal,
  useGoalsByStudent,
  useUpdateGoal,
} from "./hooks/use-goals.ts";
export {
  goalCreateSchema,
  toGoalCreateRequest,
  type GoalCreateFormValues,
} from "./schemas/goal-create.schema.ts";
export {
  goalUpdateSchema,
  toGoalUpdateRequest,
  type GoalUpdateFormValues,
} from "./schemas/goal-update.schema.ts";
export type {
  GoalCreateRequest,
  GoalResponse,
  GoalUpdateRequest,
} from "./types/index.ts";
