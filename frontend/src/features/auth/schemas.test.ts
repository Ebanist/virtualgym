import { describe, expect, it } from 'vitest'
import { registerSchema, resetPasswordSchema } from './schemas'

describe('auth schemas', () => {
  it('requires letter and digit in password', () => {
    const result = registerSchema.safeParse({ email: 'a@b.pl', password: 'onlyletters', displayName: 'Ania' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.message).toBe('validation.password')
  })

  it('accepts valid registration data and trims email', () => {
    const result = registerSchema.safeParse({ email: ' a@b.pl ', password: 'Zażółć123', displayName: 'Ania' })
    expect(result.success).toBe(true)
    expect(result.data?.email).toBe('a@b.pl')
  })

  it('detects password mismatch', () => {
    const result = resetPasswordSchema.safeParse({ newPassword: 'Secret123', confirmPassword: 'Secret124' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.path).toEqual(['confirmPassword'])
  })
})
