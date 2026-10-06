import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi, type AuthResponse, type LoginRequest, type RegisterRequest, type User } from '../api/auth'
import { refreshSession, setSessionExpiredHandler } from '../api/client'
import { tokenStore } from '../api/tokenStore'

type Status = 'loading' | 'authenticated' | 'anonymous'

interface AuthContextValue {
  status: Status
  user: User | null
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => Promise<void>
  /** Ustawia sesję z odpowiedzi API (np. po zmianie hasła, która wydaje nowe tokeny). */
  applySession: (response: AuthResponse) => void
  setUser: (user: User) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<Status>('loading')
  const [user, setUser] = useState<User | null>(null)

  const applySession = useCallback((response: AuthResponse) => {
    tokenStore.set(response.accessToken)
    setUser(response.user)
    setStatus('authenticated')
  }, [])

  const clearSession = useCallback(() => {
    tokenStore.set(null)
    setUser(null)
    setStatus('anonymous')
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    let cancelled = false
    void refreshSession<AuthResponse>().then((session) => {
      if (cancelled) return
      if (session) applySession(session)
      else setStatus('anonymous')
    })
    setSessionExpiredHandler(clearSession)
    return () => {
      cancelled = true
      setSessionExpiredHandler(null)
    }
  }, [applySession, clearSession])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      applySession,
      setUser,
      login: async (body) => applySession(await authApi.login(body)),
      register: async (body) => applySession(await authApi.register(body)),
      logout: async () => {
        try {
          await authApi.logout()
        } finally {
          clearSession()
        }
      },
    }),
    [status, user, applySession, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
