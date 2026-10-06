import { z } from 'zod'
import { MUSCLE_GROUPS } from '../../api/exercises'

export const customExerciseSchema = z
  .object({
    name: z.string().trim().min(2, { error: 'validation.length' }).max(120, { error: 'validation.length' }),
    primaryMuscle: z.enum(MUSCLE_GROUPS, { error: 'validation.required' }),
    secondaryMuscles: z.array(z.enum(MUSCLE_GROUPS)),
    description: z.string().trim().max(2000, { error: 'validation.length' }).optional(),
    bodyweight: z.boolean(),
    equipmentIds: z.array(z.string()),
  })
  .refine((v) => v.bodyweight || v.equipmentIds.length > 0, {
    error: 'exercises.equipmentRequired',
    path: ['equipmentIds'],
  })
export type CustomExerciseForm = z.infer<typeof customExerciseSchema>
