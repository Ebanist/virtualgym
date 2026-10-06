import { QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { AuthProvider } from './AuthProvider'
import { createQueryClient } from './queryClient'

export function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(createQueryClient)
  return (
    <QueryClientProvider client={client}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  )
}
