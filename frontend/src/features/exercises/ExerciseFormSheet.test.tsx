import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, renderWithProviders } from '../../test/utils'
import { ExerciseFormSheet } from './ExerciseFormSheet'

const equipmentPage = {
  content: [
    { id: 'eq-treadmill', name: 'Bieżnia', category: 'CARDIO', status: 'ACTIVE', openReportCount: 0 },
    { id: 'eq-bike', name: 'Rower', category: 'CARDIO', status: 'ACTIVE', openReportCount: 0 },
  ],
  page: 0,
  size: 100,
  totalElements: 2,
  totalPages: 1,
}

describe('ExerciseFormSheet', () => {
  let posted: Record<string, unknown> | null

  beforeEach(() => {
    posted = null
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: Request) => {
        const url = new URL(input.url)
        if (url.pathname === '/api/v1/gyms/g1/equipment') return jsonResponse(equipmentPage)
        if (url.pathname === '/api/v1/gyms/g1/exercises/similar') {
          return jsonResponse([
            { id: 'x1', name: 'Bieg na bieżni', primaryMuscle: 'CARDIO', secondaryMuscles: [], bodyweight: false, scope: 'GLOBAL', mine: false, equipmentTypes: [] },
          ])
        }
        if (url.pathname === '/api/v1/gyms/g1/exercises' && input.method === 'POST') {
          posted = await input.json()
          return jsonResponse({ ...posted, id: 'new', scope: 'CUSTOM', mine: true, secondaryMuscles: [], equipmentTypes: [] }, 201)
        }
        return jsonResponse({ status: 401, code: 'refresh_token_missing' }, 401)
      }),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('prefills name and equipment, defaults to private and submits', async () => {
    const onSaved = vi.fn()
    renderWithProviders(
      <ExerciseFormSheet
        open
        onClose={() => {}}
        gymId="g1"
        initialName="Chodzenie na bieżni"
        initialEquipmentIds={['eq-treadmill']}
        onSaved={onSaved}
      />,
    )

    expect(screen.getByRole('dialog', { name: 'Nowe ćwiczenie' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nazwa ćwiczenia')).toHaveValue('Chodzenie na bieżni')
    expect(await screen.findByLabelText('Bieżnia')).toBeChecked()
    expect(screen.getByLabelText('Rower')).not.toBeChecked()
    expect(screen.getByRole('tab', { name: 'Prywatne' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('Tylko Ty – w swoich planach i treningach.')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Główna partia mięśniowa'), 'CARDIO')
    await userEvent.click(screen.getByRole('tab', { name: 'Publiczne' }))
    await userEvent.click(screen.getByRole('button', { name: 'Zapisz ćwiczenie' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    expect(posted).toMatchObject({
      name: 'Chodzenie na bieżni',
      primaryMuscle: 'CARDIO',
      equipmentIds: ['eq-treadmill'],
      bodyweight: false,
      visibility: 'GYM',
    })
  })

  it('suggests similar exercises and lets pick an existing one', async () => {
    const onPick = vi.fn()
    renderWithProviders(
      <ExerciseFormSheet open onClose={() => {}} gymId="g1" initialName="bieg na biezni" onSaved={() => {}} onPickExisting={onPick} />,
    )
    expect(await screen.findByText('Bieg na bieżni')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Użyj tego' }))
    expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ id: 'x1' }))
  })

  it('requires equipment or bodyweight', async () => {
    renderWithProviders(<ExerciseFormSheet open onClose={() => {}} gymId="g1" initialName="Pajacyki" onSaved={() => {}} />)
    await userEvent.selectOptions(screen.getByLabelText('Główna partia mięśniowa'), 'CARDIO')
    await userEvent.click(screen.getByRole('button', { name: 'Zapisz ćwiczenie' }))
    expect(await screen.findByText('Wybierz sprzęt albo zaznacz ćwiczenie z masą ciała.')).toBeInTheDocument()
    expect(posted).toBeNull()
  })
})
