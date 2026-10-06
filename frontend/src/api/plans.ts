import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type Plan = components['schemas']['PlanDto']
export type PlanSummary = components['schemas']['PlanSummaryDto']
export type PlanDay = components['schemas']['PlanDayDto']
export type PlanItem = components['schemas']['PlanItemDto']
export type PlanItemRequest = components['schemas']['PlanItemRequest']
export type CreatePlanRequest = components['schemas']['CreatePlanRequest']
export type Direction = 'UP' | 'DOWN'

export const planKeys = {
  all: ['plans'] as const,
  list: (archived: boolean) => ['plans', 'list', { archived }] as const,
  detail: (id: string) => ['plans', 'detail', id] as const,
}

export function usePlans(archived: boolean) {
  return useQuery({
    queryKey: planKeys.list(archived),
    queryFn: () => unwrap(api.GET('/api/v1/plans', { params: { query: { archived } } })),
  })
}

export function usePlan(id: string) {
  return useQuery({
    queryKey: planKeys.detail(id),
    queryFn: () => unwrap(api.GET('/api/v1/plans/{planId}', { params: { path: { planId: id } } })),
  })
}

export function useCreatePlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreatePlanRequest) => unwrap(api.POST('/api/v1/plans', { body })),
    onSuccess: (plan) => {
      queryClient.setQueryData(planKeys.detail(plan.id), plan)
      return queryClient.invalidateQueries({ queryKey: ['plans', 'list'] })
    },
  })
}

const path = (planId: string) => ({ params: { path: { planId } } })

/** Wywołania modyfikujące plan – każde zwraca cały, zaktualizowany plan. */
export const planActions = {
  update: (planId: string, name: string, description?: string) =>
    unwrap(api.PUT('/api/v1/plans/{planId}', { ...path(planId), body: { name, description } })),
  copy: (planId: string) => unwrap(api.POST('/api/v1/plans/{planId}/copy', { ...path(planId), body: {} })),
  archive: (planId: string) => unwrap(api.POST('/api/v1/plans/{planId}/archive', path(planId))),
  unarchive: (planId: string) => unwrap(api.POST('/api/v1/plans/{planId}/unarchive', path(planId))),
  addDay: (planId: string, name: string) =>
    unwrap(api.POST('/api/v1/plans/{planId}/days', { ...path(planId), body: { name } })),
  renameDay: (planId: string, dayId: string, name: string) =>
    unwrap(api.PUT('/api/v1/plans/{planId}/days/{dayId}', { params: { path: { planId, dayId } }, body: { name } })),
  deleteDay: (planId: string, dayId: string) =>
    unwrap(api.DELETE('/api/v1/plans/{planId}/days/{dayId}', { params: { path: { planId, dayId } } })),
  moveDay: (planId: string, dayId: string, direction: Direction) =>
    unwrap(
      api.POST('/api/v1/plans/{planId}/days/{dayId}/move', {
        params: { path: { planId, dayId }, query: { direction } },
      }),
    ),
  addItem: (planId: string, dayId: string, body: PlanItemRequest) =>
    unwrap(api.POST('/api/v1/plans/{planId}/days/{dayId}/items', { params: { path: { planId, dayId } }, body })),
  updateItem: (planId: string, itemId: string, body: PlanItemRequest) =>
    unwrap(api.PUT('/api/v1/plans/{planId}/items/{itemId}', { params: { path: { planId, itemId } }, body })),
  deleteItem: (planId: string, itemId: string) =>
    unwrap(api.DELETE('/api/v1/plans/{planId}/items/{itemId}', { params: { path: { planId, itemId } } })),
  moveItem: (planId: string, itemId: string, direction: Direction) =>
    unwrap(
      api.POST('/api/v1/plans/{planId}/items/{itemId}/move', {
        params: { path: { planId, itemId }, query: { direction } },
      }),
    ),
}

/** Mutacja na planie: odpowiedź (cały plan) trafia od razu do cache szczegółów. */
export function usePlanMutation<V>(fn: (vars: V) => Promise<Plan>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (plan) => {
      queryClient.setQueryData(planKeys.detail(plan.id), plan)
      return queryClient.invalidateQueries({ queryKey: ['plans', 'list'] })
    },
  })
}

export function useDeletePlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (planId: string) => unwrap(api.DELETE('/api/v1/plans/{planId}', path(planId))),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: planKeys.all }),
  })
}
