import { z } from 'zod'
import { displayNameSchema, passwordSchema } from '../auth/schemas'

export const profileSchema = z.object({ displayName: displayNameSchema })
export type ProfileForm = z.infer<typeof profileSchema>

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, { error: 'validation.required' }),
    newPassword: passwordSchema,
    confirmPassword: z.string(),
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    error: 'validation.passwordMismatch',
    path: ['confirmPassword'],
  })
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>
