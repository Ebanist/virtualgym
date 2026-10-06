import { z } from 'zod'

export const gymSchema = z.object({
  name: z.string().trim().min(2, { error: 'validation.length' }).max(120, { error: 'validation.length' }),
  city: z.string().trim().min(2, { error: 'validation.length' }).max(80, { error: 'validation.length' }),
  address: z.string().trim().min(3, { error: 'validation.length' }).max(200, { error: 'validation.length' }),
  description: z.string().trim().max(2000, { error: 'validation.length' }).optional(),
})
export type GymForm = z.infer<typeof gymSchema>
