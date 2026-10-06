import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../app/AuthProvider'
import { createQueryClient } from '../app/queryClient'

/** Renderuje komponent z providerami aplikacji pod podaną ścieżką. */
export function renderWithProviders(
  ui: ReactElement,
  { route = '/', path = '/', extraRoutes = [] as { path: string; element: ReactElement }[] } = {},
) {
  const client = createQueryClient()
  client.setDefaultOptions({ queries: { retry: false } })
  return render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <MemoryRouter initialEntries={[route]}>
          <Routes>
            <Route path={path} element={ui} />
            {extraRoutes.map((r) => (
              <Route key={r.path} path={r.path} element={r.element} />
            ))}
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  )
}

export function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
  })
}
