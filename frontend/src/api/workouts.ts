import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type Session = components['schemas']['SessionDto']
export type SessionExercise = components['schemas']['SessionExerciseDto']
export type SessionSet = components['schemas']['SessionSetDto']
export type SessionSummary = components['schemas']['SessionSummaryDto']
export type ExerciseHistoryEntry = components['schemas']['ExerciseHistoryEntryDto']
export type StartSessionRequest = components['schemas']['StartSessionRequest']
export type UpdateSetRequest = components['schemas']['UpdateSetRequest']
export type AddSessionExerciseRequest = components['schemas']['AddSessionExerciseRequest']

export const sessionKeys = {
  all: ['sessions'] as const,
  active: ['sessions', 'active'] as const,
  detail: (id: string) => ['sessions', 'detail', id] as const,
  history: (page: number) => ['sessions', 'history', page] as const,
}

export function useActiveSession() {
  return useQuery({
    queryKey: sessionKeys.active,
    // 204 (brak treningu) => null
    queryFn: async () => (await unwrap(api.GET('/api/v1/sessions/active'))) ?? null,
  })
}

export function useSession(id: string) {
  return useQuery({
    queryKey: sessionKeys.detail(id),
    queryFn: () => unwrap(api.GET('/api/v1/sessions/{sessionId}', { params: { path: { sessionId: id } } })),
  })
}

export function useHistory(page: number) {
  return useQuery({
    queryKey: sessionKeys.history(page),
    queryFn: () => unwrap(api.GET('/api/v1/sessions', { params: { query: { page, size: 20 } } })),
    placeholderData: keepPreviousData,
  })
}

export function useStartSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: StartSessionRequest) => unwrap(api.POST('/api/v1/sessions', { body })),
    onSuccess: (session) => {
      queryClient.setQueryData(sessionKeys.detail(session.id), session)
      queryClient.setQueryData(sessionKeys.active, session)
    },
  })
}

const sid = (sessionId: string) => ({ params: { path: { sessionId } } })

export const sessionActions = {
  updateSet: (sessionId: string, setId: string, body: UpdateSetRequest) =>
    unwrap(api.PATCH('/api/v1/sessions/{sessionId}/sets/{setId}', { params: { path: { sessionId, setId } }, body })),
  removeSet: (sessionId: string, setId: string) =>
    unwrap(api.DELETE('/api/v1/sessions/{sessionId}/sets/{setId}', { params: { path: { sessionId, setId } } })),
  addSet: (sessionId: string, sessionExerciseId: string) =>
    unwrap(
      api.POST('/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets', {
        params: { path: { sessionId, sessionExerciseId } },
      }),
    ),
  addExercise: (sessionId: string, body: AddSessionExerciseRequest) =>
    unwrap(api.POST('/api/v1/sessions/{sessionId}/exercises', { ...sid(sessionId), body })),
  removeExercise: (sessionId: string, sessionExerciseId: string) =>
    unwrap(
      api.DELETE('/api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}', {
        params: { path: { sessionId, sessionExerciseId } },
      }),
    ),
  finish: (sessionId: string, note?: string) =>
    unwrap(api.POST('/api/v1/sessions/{sessionId}/finish', { ...sid(sessionId), body: { note } })),
}

/** Mutacja treningu – odpowiedź (cały trening) trafia do cache. */
export function useSessionMutation<V>(fn: (vars: V) => Promise<Session>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (session) => {
      queryClient.setQueryData(sessionKeys.detail(session.id), session)
      if (session.status !== 'IN_PROGRESS') {
        queryClient.setQueryData(sessionKeys.active, null)
        return queryClient.invalidateQueries({ queryKey: ['sessions', 'history'] })
      }
    },
  })
}

export function useAbandonSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (sessionId: string) => unwrap(api.POST('/api/v1/sessions/{sessionId}/abandon', sid(sessionId))),
    onSuccess: () => {
      queryClient.setQueryData(sessionKeys.active, null)
      return queryClient.invalidateQueries({ queryKey: sessionKeys.all })
    },
  })
}
