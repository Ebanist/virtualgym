import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, renderWithProviders } from '../../test/utils'
import { LoginPage } from './LoginPage'

const user = { id: 'u1', email: 'anna@example.com', displayName: 'Anna', role: 'USER', createdAt: '2026-01-01T00:00:00Z' }

describe('LoginPage', () => {
  const fetchMock = vi.fn<(input: Request | string) => Promise<Response>>()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function route(handler: (url: string, request: Request | string) => Response) {
    fetchMock.mockImplementation(async (input) => {
      const url = typeof input === 'string' ? input : input.url
      return handler(url, input)
    })
  }

  it('shows validation errors without calling the API', async () => {
    route(() => jsonResponse({ status: 401, code: 'refresh_token_missing' }, 401))
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' })

    await userEvent.click(await screen.findByRole('button', { name: 'Zaloguj' }))

    expect(await screen.findAllByText('To pole jest wymagane.')).toHaveLength(2)
    expect(fetchMock.mock.calls.some(([req]) => String(typeof req === 'string' ? req : req.url).includes('/login'))).toBe(false)
  })

  it('shows translated server error on invalid credentials', async () => {
    route((url) =>
      url.includes('/auth/login')
        ? jsonResponse({ status: 401, code: 'invalid_credentials' }, 401)
        : jsonResponse({ status: 401, code: 'refresh_token_missing' }, 401),
    )
    renderWithProviders(<LoginPage />, { route: '/login', path: '/login' })

    await userEvent.type(await screen.findByLabelText('E-mail'), 'anna@example.com')
    await userEvent.type(screen.getByLabelText('Hasło'), 'WrongPass1')
    await userEvent.click(screen.getByRole('button', { name: 'Zaloguj' }))

    expect(await screen.findByText('Nieprawidłowy e-mail lub hasło.')).toBeInTheDocument()
  })

  it('logs in and navigates to the home page', async () => {
    route((url) =>
      url.includes('/auth/login')
        ? jsonResponse({ accessToken: 'token', expiresIn: 900, user })
        : jsonResponse({ status: 401, code: 'refresh_token_missing' }, 401),
    )
    renderWithProviders(<LoginPage />, {
      route: '/login',
      path: '/login',
      extraRoutes: [{ path: '/', element: <p>Strona główna</p> }],
    })

    await userEvent.type(await screen.findByLabelText('E-mail'), 'anna@example.com')
    await userEvent.type(screen.getByLabelText('Hasło'), 'Secret123')
    await userEvent.click(screen.getByRole('button', { name: 'Zaloguj' }))

    await waitFor(() => expect(screen.getByText('Strona główna')).toBeInTheDocument())
  })
})
