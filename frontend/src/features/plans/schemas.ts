import { z } from 'zod'

export const planSchema = z.object({
  gymId: z.string().min(1, { error: 'validation.required' }),
  name: z.string().trim().min(2, { error: 'validation.length' }).max(100, { error: 'validation.length' }),
  description: z.string().trim().max(2000, { error: 'validation.length' }).optional(),
})
export type PlanForm = z.infer<typeof planSchema>

const int = (min: number, max: number) =>
  z.coerce.number({ error: 'validation.invalid' }).int({ error: 'validation.invalid' }).min(min, { error: 'validation.invalid' }).max(max, { error: 'validation.invalid' })

export const planItemSchema = z
  .object({
    /** '' = bez sprzętu (masa ciała). */
    equipmentId: z.string(),
    sets: int(1, 20),
    repsMin: int(1, 100),
    repsMax: int(1, 100),
    targetWeightKg: z
      .union([z.literal(''), z.coerce.number({ error: 'validation.invalid' }).min(0, { error: 'validation.invalid' }).max(1000, { error: 'validation.invalid' })])
      .optional(),
    restSeconds: int(0, 900),
    note: z.string().trim().max(500, { error: 'validation.length' }).optional(),
  })
  .refine((v) => v.repsMax >= v.repsMin, { error: 'plans.repsRange', path: ['repsMax'] })
export type PlanItemFormInput = z.input<typeof planItemSchema>
export type PlanItemForm = z.output<typeof planItemSchema>

export const DEFAULT_ITEM: PlanItemFormInput = {
  equipmentId: '',
  sets: 3,
  repsMin: 8,
  repsMax: 12,
  targetWeightKg: '',
  restSeconds: 90,
  note: '',
}
