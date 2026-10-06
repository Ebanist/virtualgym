import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { PlanItem } from '../../api/plans'
import { PlanItemRow } from './PlanItemRow'
import { planItemSchema } from './schemas'

const item: PlanItem = {
  id: 'i1',
  position: 0,
  exercise: { id: 'x1', name: 'Przysiad na suwnicy Smitha', primaryMuscle: 'QUADRICEPS', bodyweight: false },
  equipment: { id: 'e1', name: 'Suwnica Smitha', status: 'REMOVED_FROM_GYM', deleted: false },
  sets: 3,
  repsMin: 8,
  repsMax: 10,
  targetWeightKg: 40,
  restSeconds: 120,
  equipmentUnavailable: true,
}

function renderRow(props: Partial<Parameters<typeof PlanItemRow>[0]> = {}) {
  const onMove = vi.fn()
  const onDelete = vi.fn()
  render(
    <MemoryRouter>
      <ul>
        <PlanItemRow planId="p1" item={item} isFirst={false} isLast={false} onMove={onMove} onDelete={onDelete} {...props} />
      </ul>
    </MemoryRouter>,
  )
  return { onMove, onDelete }
}

describe('PlanItemRow', () => {
  it('shows parameters and removed-equipment warning', () => {
    renderRow()
    expect(screen.getByLabelText('3 serie po 8–10 powtórzeń, przerwa 120 s')).toHaveTextContent('3 × 8–10')
    expect(screen.getByText('40')).toBeInTheDocument()
    expect(screen.getByRole('note')).toHaveTextContent('Sprzęt oznaczony jako usunięty z siłowni')
  })

  it('moves item with up/down buttons', async () => {
    const { onMove } = renderRow()
    await userEvent.click(screen.getByRole('button', { name: 'Przesuń w górę: Przysiad na suwnicy Smitha' }))
    await userEvent.click(screen.getByRole('button', { name: 'Przesuń w dół: Przysiad na suwnicy Smitha' }))
    expect(onMove.mock.calls).toEqual([['UP'], ['DOWN']])
  })

  it('disables moving beyond edges', () => {
    renderRow({ isFirst: true, isLast: true })
    expect(screen.getByRole('button', { name: /w górę/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /w dół/ })).toBeDisabled()
  })
})

describe('planItemSchema', () => {
  const base = { equipmentId: '', sets: '3', repsMin: '8', repsMax: '12', targetWeightKg: '', restSeconds: '90' }

  it('coerces numbers and treats empty weight as missing', () => {
    const parsed = planItemSchema.parse(base)
    expect(parsed).toMatchObject({ sets: 3, repsMin: 8, repsMax: 12, targetWeightKg: '', restSeconds: 90 })
  })

  it('rejects reps range where max < min', () => {
    const result = planItemSchema.safeParse({ ...base, repsMin: '12', repsMax: '8' })
    expect(result.success).toBe(false)
    expect(result.error?.issues[0]?.path).toEqual(['repsMax'])
  })
})
