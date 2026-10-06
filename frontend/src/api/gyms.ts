import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type GymSummary = components['schemas']['GymSummaryDto']
export type Gym = components['schemas']['GymDto']
export type CreateGymRequest = components['schemas']['CreateGymRequest']

export const gymKeys = {
  all: ['gyms'] as const,
  search: (q: string, city: string, page: number) => ['gyms', 'search', { q, city, page }] as const,
  similar: (name: string, city: string) => ['gyms', 'similar', { name, city }] as const,
  detail: (id: string) => ['gyms', 'detail', id] as const,
  mine: ['gyms', 'mine'] as const,
}

export const GYM_PAGE_SIZE = 20

export function useGymSearch(q: string, city: string, page: number) {
  return useQuery({
    queryKey: gymKeys.search(q, city, page),
    queryFn: () =>
      unwrap(api.GET('/api/v1/gyms', { params: { query: { q, city, page, size: GYM_PAGE_SIZE } } })),
    placeholderData: keepPreviousData,
  })
}

export function useSimilarGyms(name: string, city: string) {
  const enabled = name.trim().length >= 3 && city.trim().length >= 2
  return useQuery({
    queryKey: gymKeys.similar(name, city),
    queryFn: () => unwrap(api.GET('/api/v1/gyms/similar', { params: { query: { name, city } } })),
    enabled,
  })
}

export function useGym(id: string) {
  return useQuery({
    queryKey: gymKeys.detail(id),
    queryFn: () => unwrap(api.GET('/api/v1/gyms/{gymId}', { params: { path: { gymId: id } } })),
  })
}

export function useMyGyms() {
  return useQuery({
    queryKey: gymKeys.mine,
    queryFn: () => unwrap(api.GET('/api/v1/me/gyms')),
  })
}

export function useCreateGym() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateGymRequest) => unwrap(api.POST('/api/v1/gyms', { body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: gymKeys.all }),
  })
}

export function useMembership(gymId: string) {
  const queryClient = useQueryClient()
  const invalidate = () => queryClient.invalidateQueries({ queryKey: gymKeys.all })
  const join = useMutation({
    mutationFn: () =>
      unwrap(api.POST('/api/v1/gyms/{gymId}/membership', { params: { path: { gymId } } })),
    onSuccess: invalidate,
  })
  const leave = useMutation({
    mutationFn: () =>
      unwrap(api.DELETE('/api/v1/gyms/{gymId}/membership', { params: { path: { gymId } } })),
    onSuccess: invalidate,
  })
  return { join, leave }
}
