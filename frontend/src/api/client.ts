import createClient from 'openapi-fetch'
import type { paths } from './schema'
import { tokenStore } from './tokenStore'

/**
 * Klient API: typy generowane z OpenAPI backendu (openapi-typescript), wywołania przez openapi-fetch.
 * Własny `fetch` dokleja access token i – przy 401 – raz próbuje odświeżyć sesję, po czym ponawia żądanie.
 */

interface RefreshedSession {
  accessToken: string
}

let refreshInFlight: Promise<RefreshedSession | null> | null = null
let onSessionExpired: (() => void) | null = null

export function setSessionExpiredHandler(handler: (() => void) | null) {
  onSessionExpired = handler
}

/** Odświeża access token (pojedyncze żądanie nawet przy wielu równoległych 401). */
export function refreshSession<T extends RefreshedSession>(): Promise<T | null> {
  if (!refreshInFlight) {
    refreshInFlight = fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'same-origin' })
      .then(async (res) => {
        if (!res.ok) {
          tokenStore.set(null)
          return null
        }
        const body = (await res.json()) as T
        tokenStore.set(body.accessToken)
        return body
      })
      .catch(() => null)
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight as Promise<T | null>
}

function withAuth(request: Request): Request {
  const token = tokenStore.get()
  if (!token) {
    return request
  }
  const headers = new Headers(request.headers)
  headers.set('Authorization', `Bearer ${token}`)
  return new Request(request, { headers })
}

export async function authFetch(input: Request): Promise<Response> {
  const isAuthEndpoint = new URL(input.url, window.location.origin).pathname.startsWith('/api/v1/auth/')
  const retry = input.clone()
  const response = await fetch(isAuthEndpoint ? input : withAuth(input))
  if (response.status !== 401 || isAuthEndpoint) {
    return response
  }
  const session = await refreshSession()
  if (!session) {
    onSessionExpired?.()
    return response
  }
  return fetch(withAuth(retry))
}

export const api = createClient<paths>({
  baseUrl: window.location.origin,
  credentials: 'same-origin',
  fetch: authFetch,
})
