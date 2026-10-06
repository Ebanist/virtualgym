import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/errors'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // Nie ponawiamy błędów 4xx (walidacja, brak dostępu) – tylko sieć/5xx.
        retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
        refetchOnWindowFocus: false,
      },
    },
  })
}
