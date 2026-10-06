/**
 * Access token trzymamy wyłącznie w pamięci (nie w localStorage) – odporność na XSS.
 * Po przeładowaniu strony sesję odtwarza POST /auth/refresh (refresh token w ciasteczku httpOnly).
 */
type Listener = (token: string | null) => void

let accessToken: string | null = null
const listeners = new Set<Listener>()

export const tokenStore = {
  get: () => accessToken,
  set(token: string | null) {
    accessToken = token
    listeners.forEach((l) => l(token))
  },
  subscribe(listener: Listener) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  },
}
