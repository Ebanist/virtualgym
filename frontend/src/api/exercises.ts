import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type Exercise = components['schemas']['ExerciseDto']
export type MuscleGroup = Exercise['primaryMuscle']
export type AvailableExercise = components['schemas']['AvailableExerciseDto']
export type EquipmentExercise = components['schemas']['EquipmentExerciseDto']
export type CreateExerciseRequest = components['schemas']['CreateExerciseRequest']

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
  library: (q: string, muscle?: MuscleGroup) => ['exercises', 'library', { q, muscle }] as const,
  available: (gymId: string, q: string, muscle?: MuscleGroup) => ['exercises', 'available', gymId, { q, muscle }] as const,
  forEquipment: (equipmentId: string) => ['exercises', 'equipment', equipmentId] as const,
}

export function useExerciseLibrary(q: string, muscle?: MuscleGroup, enabled = true) {
  return useQuery({
    queryKey: exerciseKeys.library(q, muscle),
    queryFn: () => unwrap(api.GET('/api/v1/exercises', { params: { query: { q: q || undefined, muscle } } })),
    placeholderData: keepPreviousData,
    enabled,
  })
}

export function useAvailableExercises(gymId: string, q: string, muscle?: MuscleGroup) {
  return useQuery({
    queryKey: exerciseKeys.available(gymId, q, muscle),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/gyms/{gymId}/exercises/available', {
          params: { path: { gymId }, query: { q: q || undefined, muscle } },
        }),
      ),
    placeholderData: keepPreviousData,
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
