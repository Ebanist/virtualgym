import type { Exercise } from '../../api/exercises'

/** Pochodzenie ćwiczenia: biblioteka, moje (dodane przeze mnie) lub społeczność (publiczne innych członków). */
export type ExerciseOrigin = 'library' | 'mine' | 'community'

export const EXERCISE_ORIGINS: ExerciseOrigin[] = ['library', 'mine', 'community']

export function exerciseOrigin(exercise: Pick<Exercise, 'scope' | 'mine'>): ExerciseOrigin {
  if (exercise.scope === 'GLOBAL') return 'library'
  return exercise.mine ? 'mine' : 'community'
}

export function matchesOrigin(exercise: Pick<Exercise, 'scope' | 'mine'>, filter: ExerciseOrigin | '') {
  return filter === '' || exerciseOrigin(exercise) === filter
}
