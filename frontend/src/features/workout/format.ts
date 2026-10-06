import type { TFunction } from 'i18next'
import type { ExerciseHistoryEntry, SessionExercise } from '../../api/workouts'

/** „12 × 40 kg, 10 × 42.5 kg” */
export function formatResults(sets: ExerciseHistoryEntry['sets']) {
  return sets
    .map((s) => {
      const reps = s.reps ?? '–'
      return s.weightKg !== undefined && s.weightKg !== null ? `${reps} × ${s.weightKg} kg` : `${reps}`
    })
    .join(', ')
}

/** „Cel: 3 × 8–12 · 40 kg” */
export function formatTarget(t: TFunction, e: SessionExercise) {
  if (e.targetRepsMin === undefined || e.targetRepsMax === undefined) return null
  const reps = e.targetRepsMin === e.targetRepsMax ? `${e.targetRepsMin}` : `${e.targetRepsMin}–${e.targetRepsMax}`
  const weight = e.targetWeightKg !== undefined ? ` · ${e.targetWeightKg} kg` : ''
  return t('workout.target', { value: `${e.sets.length} × ${reps}${weight}` })
}

export function parseNumber(value: string): number | undefined {
  const normalized = value.replace(',', '.').trim()
  if (normalized === '') return undefined
  const n = Number(normalized)
  return Number.isFinite(n) && n >= 0 ? n : undefined
}
