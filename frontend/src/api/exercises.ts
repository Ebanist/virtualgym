import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type Exercise = components['schemas']['ExerciseDto']
export type MuscleGroup = Exercise['primaryMuscle']
export type AvailableExercise = components['schemas']['AvailableExerciseDto']
export type EquipmentExercise = components['schemas']['EquipmentExerciseDto']
export type CreateExerciseRequest = components['schemas']['CreateExerciseRequest']
export type ExerciseVisibility = NonNullable<Exercise['visibility']>

export const MUSCLE_GROUPS: MuscleGroup[] = [
  'CHEST',
  'BACK',
  'LOWER_BACK',
  'TRAPS',
  'SHOULDERS',
  'BICEPS',
  'TRICEPS',
  'FOREARMS',
  'ABS',
  'OBLIQUES',
  'GLUTES',
  'QUADRICEPS',
  'HAMSTRINGS',
  'ADDUCTORS',
  'ABDUCTORS',
  'CALVES',
  'FULL_BODY',
  'CARDIO',
]

export const exerciseKeys = {
  all: ['exercises'] as const,
  library: (q: string, muscle?: MuscleGroup, gymId?: string) => ['exercises', 'library', { q, muscle, gymId }] as const,
  similar: (gymId: string, name: string) => ['exercises', 'similar', gymId, name] as const,
  detail: (id: string) => ['exercises', 'detail', id] as const,
  available: (gymId: string, q: string, muscle?: MuscleGroup) => ['exercises', 'available', gymId, { q, muscle }] as const,
  forEquipment: (equipmentId: string) => ['exercises', 'equipment', equipmentId] as const,
}

/** Biblioteka: globalne + widoczne własne; z {@code gymId} – tylko ćwiczenia widoczne w tej siłowni. */
export function useExerciseLibrary(q: string, muscle?: MuscleGroup, enabled = true, gymId?: string) {
  return useQuery({
    queryKey: exerciseKeys.library(q, muscle, gymId),
    queryFn: () =>
      unwrap(api.GET('/api/v1/exercises', { params: { query: { q: q || undefined, muscle, gymId } } })),
    placeholderData: keepPreviousData,
    enabled,
  })
}

function fetchAvailableExercises(gymId: string, q: string, muscle?: MuscleGroup) {
  return unwrap(
    api.GET('/api/v1/gyms/{gymId}/exercises/available', {
      params: { path: { gymId }, query: { q: q || undefined, muscle } },
    }),
  )
}

/** Świeża pozycja z listy dostępnych (np. zaraz po utworzeniu ćwiczenia). */
export async function findAvailableExercise(queryClient: QueryClient, gymId: string, exerciseId: string) {
  const list = await queryClient.fetchQuery({
    queryKey: exerciseKeys.available(gymId, '', undefined),
    queryFn: () => fetchAvailableExercises(gymId, ''),
    staleTime: 0,
  })
  return list.find((option) => option.exercise.id === exerciseId)
}

export function useAvailableExercises(gymId: string, q: string, muscle?: MuscleGroup) {
  return useQuery({
    queryKey: exerciseKeys.available(gymId, q, muscle),
    queryFn: () => fetchAvailableExercises(gymId, q, muscle),
    placeholderData: keepPreviousData,
  })
}

export function useSimilarExercises(gymId: string, name: string, enabled = true) {
  return useQuery({
    queryKey: exerciseKeys.similar(gymId, name),
    queryFn: () =>
      unwrap(api.GET('/api/v1/gyms/{gymId}/exercises/similar', { params: { path: { gymId }, query: { name } } })),
    enabled: enabled && name.trim().length >= 3,
  })
}

/** Szczegóły (dla autora zawiera powiązany sprzęt – potrzebne do edycji). */
export function useExercise(id: string | undefined) {
  return useQuery({
    queryKey: exerciseKeys.detail(id ?? ''),
    queryFn: () => unwrap(api.GET('/api/v1/exercises/{id}', { params: { path: { id: id ?? '' } } })),
    enabled: !!id,
  })
}

export function useUpdateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: CreateExerciseRequest }) =>
      unwrap(api.PUT('/api/v1/exercises/{id}', { params: { path: { id } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  })
}

export function useDeleteExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => unwrap(api.DELETE('/api/v1/exercises/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  })
}

export function useEquipmentExercises(equipmentId: string) {
  return useQuery({
    queryKey: exerciseKeys.forEquipment(equipmentId),
    queryFn: () =>
      unwrap(api.GET('/api/v1/equipment/{equipmentId}/exercises', { params: { path: { equipmentId } } })),
  })
}

export function useExerciseLinks(equipmentId: string) {
  const queryClient = useQueryClient()
  const invalidate = () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all })
  const link = useMutation({
    mutationFn: (exerciseId: string) =>
      unwrap(
        api.PUT('/api/v1/equipment/{equipmentId}/exercises/{exerciseId}', {
          params: { path: { equipmentId, exerciseId } },
        }),
      ),
    onSuccess: invalidate,
  })
  const unlink = useMutation({
    mutationFn: (exerciseId: string) =>
      unwrap(
        api.DELETE('/api/v1/equipment/{equipmentId}/exercises/{exerciseId}', {
          params: { path: { equipmentId, exerciseId } },
        }),
      ),
    onSuccess: invalidate,
  })
  return { link, unlink }
}

export function useCreateCustomExercise(gymId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateExerciseRequest) =>
      unwrap(api.POST('/api/v1/gyms/{gymId}/exercises', { params: { path: { gymId } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  })
}
