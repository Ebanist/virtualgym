import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Exercise } from '../../api/exercises'
import { ExerciseCard } from './ExerciseCard'
import { customExerciseSchema } from './schemas'

const exercise: Exercise = {
  id: 'x1',
  name: 'Ściąganie drążka wyciągu górnego do klatki',
  primaryMuscle: 'BACK',
  secondaryMuscles: ['BICEPS'],
  description: 'Opis ćwiczenia',
  bodyweight: false,
  scope: 'GLOBAL',
  equipmentTypes: [{ id: 't1', code: 'LAT_PULLDOWN', name: 'Wyciąg górny', category: 'CABLE' }],
}

describe('ExerciseCard', () => {
  it('shows muscles and required equipment types', () => {
    render(<ExerciseCard exercise={exercise} />)
    expect(screen.getByText('Plecy · Biceps')).toBeInTheDocument()
    expect(screen.getByText('Wyciąg górny')).toBeInTheDocument()
    expect(screen.queryByText('Masa ciała')).not.toBeInTheDocument()
  })
})

describe('customExerciseSchema', () => {
  const base = { name: 'Moje ćwiczenie', primaryMuscle: 'CHEST', secondaryMuscles: [], bodyweight: false }

  it('requires equipment unless bodyweight', () => {
    const result = customExerciseSchema.safeParse({ ...base, equipmentIds: [] })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.path).toEqual(['equipmentIds'])
    expect(customExerciseSchema.safeParse({ ...base, bodyweight: true, equipmentIds: [] }).success).toBe(true)
    expect(customExerciseSchema.safeParse({ ...base, equipmentIds: ['e1'] }).success).toBe(true)
  })
})
