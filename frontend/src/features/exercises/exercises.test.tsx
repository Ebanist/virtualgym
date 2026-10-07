import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Exercise } from '../../api/exercises'
import { ExerciseCard } from './ExerciseCard'
import { exerciseOrigin, matchesOrigin } from './origin'
import { customExerciseSchema } from './schemas'

const exercise: Exercise = {
  id: 'x1',
  name: 'Ściąganie drążka wyciągu górnego do klatki',
  primaryMuscle: 'BACK',
  secondaryMuscles: ['BICEPS'],
  description: 'Opis ćwiczenia',
  bodyweight: false,
  scope: 'GLOBAL',
  mine: false,
  equipmentTypes: [{ id: 't1', code: 'LAT_PULLDOWN', name: 'Wyciąg górny', category: 'CABLE' }],
}

describe('ExerciseCard', () => {
  it('shows muscles and required equipment types', () => {
    render(<ExerciseCard exercise={exercise} />)
    expect(screen.getByText('Plecy')).toBeInTheDocument()
    expect(screen.getByText('· Biceps')).toBeInTheDocument()
    expect(screen.getByText('Wyciąg górny')).toBeInTheDocument()
    expect(screen.queryByText('Masa ciała')).not.toBeInTheDocument()
    expect(screen.getByText('Biblioteka')).toBeInTheDocument()
  })
})

describe('customExerciseSchema', () => {
  const base = { name: 'Moje ćwiczenie', primaryMuscle: 'CHEST', secondaryMuscles: [], bodyweight: false, visibility: 'PRIVATE' }

  it('requires equipment unless bodyweight', () => {
    const result = customExerciseSchema.safeParse({ ...base, equipmentIds: [] })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.path).toEqual(['equipmentIds'])
    expect(customExerciseSchema.safeParse({ ...base, bodyweight: true, equipmentIds: [] }).success).toBe(true)
    expect(customExerciseSchema.safeParse({ ...base, equipmentIds: ['e1'] }).success).toBe(true)
  })
})

describe('exerciseOrigin', () => {
  it('distinguishes library, mine and community exercises', () => {
    expect(exerciseOrigin({ scope: 'GLOBAL', mine: false })).toBe('library')
    expect(exerciseOrigin({ scope: 'CUSTOM', mine: true })).toBe('mine')
    expect(exerciseOrigin({ scope: 'CUSTOM', mine: false })).toBe('community')
    expect(matchesOrigin({ scope: 'CUSTOM', mine: true }, '')).toBe(true)
    expect(matchesOrigin({ scope: 'CUSTOM', mine: true }, 'library')).toBe(false)
  })
})
