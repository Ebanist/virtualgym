import { act, render, renderHook, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatResults, parseNumber } from './format'
import { SetRow } from './SetRow'
import { formatSeconds, useRestTimer } from './useRestTimer'

function renderSet(props: Partial<Parameters<typeof SetRow>[0]> = {}) {
  const onSave = vi.fn()
  const onCompleted = vi.fn()
  render(
    <table>
      <tbody>
        <SetRow set={{ id: 's1', setNumber: 1, weightKg: 40, completed: false }} repsHint={12} onSave={onSave} onCompleted={onCompleted} {...props} />
      </tbody>
    </table>,
  )
  return { onSave, onCompleted }
}

describe('SetRow', () => {
  it('checking an empty set uses target reps and starts rest', async () => {
    const { onSave, onCompleted } = renderSet()
    await userEvent.click(screen.getByRole('button', { name: 'Odhacz serię 1' }))
    expect(onSave).toHaveBeenCalledWith({ reps: 12, weightKg: 40, completed: true })
    expect(onCompleted).toHaveBeenCalledOnce()
    expect(screen.getByLabelText('Powtórzenia w serii 1')).toHaveValue('12')
  })

  it('saves typed values (comma decimal) on blur', async () => {
    const { onSave } = renderSet()
    const weight = screen.getByLabelText('Ciężar w serii 1')
    await userEvent.clear(weight)
    await userEvent.type(weight, '42,5')
    await userEvent.type(screen.getByLabelText('Powtórzenia w serii 1'), '10')
    await userEvent.tab()
    expect(onSave).toHaveBeenLastCalledWith({ reps: 10, weightKg: 42.5, completed: false })
  })

  it('unchecking a completed set does not start rest', async () => {
    const { onSave, onCompleted } = renderSet({ set: { id: 's1', setNumber: 1, reps: 8, weightKg: 40, completed: true } })
    await userEvent.click(screen.getByRole('button', { name: 'Cofnij odhaczenie serii 1' }))
    expect(onSave).toHaveBeenCalledWith({ reps: 8, weightKg: 40, completed: false })
    expect(onCompleted).not.toHaveBeenCalled()
  })
})

describe('useRestTimer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    localStorage.clear()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('counts down from end timestamp and finishes', () => {
    const { result } = renderHook(() => useRestTimer())
    act(() => result.current.start(90))
    expect(result.current.running).toBe(true)
    expect(result.current.remaining).toBe(90)

    act(() => vi.advanceTimersByTime(30_000))
    expect(result.current.remaining).toBe(60)

    act(() => result.current.addSeconds(15))
    act(() => vi.advanceTimersByTime(1_000))
    expect(result.current.remaining).toBe(74)

    act(() => vi.advanceTimersByTime(75_000))
    expect(result.current.running).toBe(false)
    expect(result.current.finished).toBe(true)
  })

  it('survives remount via localStorage', () => {
    const first = renderHook(() => useRestTimer())
    act(() => first.result.current.start(60))
    first.unmount()
    act(() => vi.advanceTimersByTime(10_000))
    const second = renderHook(() => useRestTimer())
    act(() => vi.advanceTimersByTime(250))
    expect(second.result.current.running).toBe(true)
    expect(second.result.current.remaining).toBe(50)
  })
})

describe('format helpers', () => {
  it('formats seconds and results', () => {
    expect(formatSeconds(75)).toBe('1:15')
    expect(formatResults([{ setNumber: 1, reps: 12, weightKg: 40 }, { setNumber: 2, reps: 10 }])).toBe('12 × 40 kg, 10')
    expect(parseNumber('')).toBeUndefined()
    expect(parseNumber('2,5')).toBe(2.5)
    expect(parseNumber('-1')).toBeUndefined()
  })
})
