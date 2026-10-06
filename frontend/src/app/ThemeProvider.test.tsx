import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { THEME_STORAGE_KEY } from './theme'
import { ThemeProvider, useTheme } from './ThemeProvider'

function Probe() {
  const { preference, theme, setPreference } = useTheme()
  return (
    <>
      <span data-testid="state">
        {preference}/{theme}
      </span>
      <button onClick={() => setPreference('light')}>light</button>
      <button onClick={() => setPreference('system')}>system</button>
    </>
  )
}

function mockSystem(light: boolean) {
  const listeners: (() => void)[] = []
  const media = {
    matches: light,
    addEventListener: (_: string, l: () => void) => listeners.push(l),
    removeEventListener: vi.fn(),
  }
  window.matchMedia = vi.fn(() => media) as unknown as typeof window.matchMedia
  return {
    change(nextLight: boolean) {
      media.matches = nextLight
      listeners.forEach((l) => l())
    },
  }
}

describe('ThemeProvider', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
  })

  it('follows system theme by default and reacts to changes', () => {
    const system = mockSystem(false)
    render(
      <ThemeProvider>
        <Probe />
      </ThemeProvider>,
    )
    expect(screen.getByTestId('state')).toHaveTextContent('system/dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    act(() => system.change(true))
    expect(document.documentElement.dataset.theme).toBe('light')
  })

  it('stores explicit choice and clears it for system', async () => {
    mockSystem(false)
    render(
      <ThemeProvider>
        <Probe />
      </ThemeProvider>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'light' }))
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
    await userEvent.click(screen.getByRole('button', { name: 'system' }))
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull()
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('restores stored preference', () => {
    mockSystem(false)
    localStorage.setItem(THEME_STORAGE_KEY, 'light')
    render(
      <ThemeProvider>
        <Probe />
      </ThemeProvider>,
    )
    expect(screen.getByTestId('state')).toHaveTextContent('light/light')
  })
})
