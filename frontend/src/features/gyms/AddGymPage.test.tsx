import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, renderWithProviders } from '../../test/utils'
import { AddGymPage } from './AddGymPage'

const candidate = {
  id: 'g1',
  name: 'Fitness Arena Centrum',
  city: 'Warszawa',
  address: 'ul. Marszałkowska 100',
  status: 'COMMUNITY',
  memberCount: 3,
  member: false,
}

describe('AddGymPage', () => {
  const posted: unknown[] = []

  beforeEach(() => {
    posted.length = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: Request) => {
        const url = new URL(input.url)
        if (url.pathname === '/api/v1/gyms/similar') return jsonResponse([])
        if (url.pathname === '/api/v1/gyms' && input.method === 'POST') {
          const body = await input.json()
          posted.push(body)
          if (!body.confirmDuplicate) {
            return jsonResponse({ status: 409, code: 'gym_possible_duplicate', candidates: [candidate] }, 409)
          }
          return jsonResponse({ ...candidate, id: 'new-gym', member: true, memberCount: 1, createdAt: '2026-01-01T00:00:00Z' }, 201)
        }
        return jsonResponse({ status: 401, code: 'refresh_token_missing' }, 401)
      }),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('warns about possible duplicate and allows adding anyway', async () => {
    renderWithProviders(<AddGymPage />, {
      route: '/gyms/new',
      path: '/gyms/new',
      extraRoutes: [{ path: '/gyms/:gymId', element: <p>Profil siłowni</p> }],
    })

    await userEvent.type(screen.getByLabelText('Nazwa'), 'Arena Fitness Centrum')
    await userEvent.type(screen.getByLabelText('Miasto'), 'Warszawa')
    await userEvent.type(screen.getByLabelText('Adres'), 'ul. Marszałkowska 102')
    await userEvent.click(screen.getByRole('button', { name: 'Dodaj siłownię' }))

    expect(await screen.findByText(/istnieje siłownia o podobnej nazwie/)).toBeInTheDocument()
    expect(screen.getByText('Fitness Arena Centrum')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'To inna siłownia – dodaj mimo to' }))

    await waitFor(() => expect(screen.getByText('Profil siłowni')).toBeInTheDocument())
    expect(posted).toHaveLength(2)
    expect(posted[1]).toMatchObject({ name: 'Arena Fitness Centrum', confirmDuplicate: true })
  })

  it('validates required fields', async () => {
    renderWithProviders(<AddGymPage />, { route: '/gyms/new', path: '/gyms/new' })
    await userEvent.click(screen.getByRole('button', { name: 'Dodaj siłownię' }))
    expect(await screen.findAllByText('Nieprawidłowa długość.')).toHaveLength(3)
    expect(posted).toHaveLength(0)
  })
})
