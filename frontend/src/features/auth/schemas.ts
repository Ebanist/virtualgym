import { z } from 'zod'

// Reguły zgodne z walidacją backendu (PasswordPolicy, RegisterRequest). Komunikaty to klucze i18n.
export const passwordSchema = z
  .string()
  .min(8, { error: 'validation.password' })
  .max(100, { error: 'validation.password' })
  .regex(/^(?=.*\p{L})(?=.*\d).*$/u, { error: 'validation.password' })

const emailSchema = z.string().trim().min(1, { error: 'validation.required' }).pipe(z.email({ error: 'validation.email' }))

export const displayNameSchema = z
  .string()
  .trim()
  .min(2, { error: 'validation.length' })
  .max(50, { error: 'validation.length' })

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, { error: 'validation.required' }),
})
export type LoginForm = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  email: emailSchema,
  password: passwordSchema,
  displayName: displayNameSchema,
})
export type RegisterForm = z.infer<typeof registerSchema>

export const forgotPasswordSchema = z.object({ email: emailSchema })
export type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>

export const resetPasswordSchema = z
  .object({ newPassword: passwordSchema, confirmPassword: z.string() })
  .refine((v) => v.newPassword === v.confirmPassword, {
    error: 'validation.passwordMismatch',
    path: ['confirmPassword'],
  })
export type ResetPasswordForm = z.infer<typeof resetPasswordSchema>
