import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type User = components['schemas']['UserDto']
export type AuthResponse = components['schemas']['AuthResponse']
export type LoginRequest = components['schemas']['LoginRequest']
export type RegisterRequest = components['schemas']['RegisterRequest']
export type UpdateProfileRequest = components['schemas']['UpdateProfileRequest']
export type ChangePasswordRequest = components['schemas']['ChangePasswordRequest']

export const authApi = {
  login: (body: LoginRequest) => unwrap(api.POST('/api/v1/auth/login', { body })),
  register: (body: RegisterRequest) => unwrap(api.POST('/api/v1/auth/register', { body })),
  logout: () => unwrap(api.POST('/api/v1/auth/logout')),
  requestPasswordReset: (email: string) =>
    unwrap(api.POST('/api/v1/auth/password-reset/request', { body: { email } })),
  confirmPasswordReset: (token: string, newPassword: string) =>
    unwrap(api.POST('/api/v1/auth/password-reset/confirm', { body: { token, newPassword } })),
}

export const profileApi = {
  me: () => unwrap(api.GET('/api/v1/me')),
  update: (body: UpdateProfileRequest) => unwrap(api.PATCH('/api/v1/me', { body })),
  changePassword: (body: ChangePasswordRequest) => unwrap(api.POST('/api/v1/me/password', { body })),
}
