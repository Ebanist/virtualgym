import { useCallback, useEffect, useState } from 'react'

const STORAGE_KEY = 'gp_rest_end'

function readStored(): number | null {
  try {
    const value = Number(localStorage.getItem(STORAGE_KEY))
    return value > Date.now() ? value : null
  } catch {
    return null
  }
}

function store(endAt: number | null) {
  try {
    if (endAt) localStorage.setItem(STORAGE_KEY, String(endAt))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // brak dostępu do storage (tryb prywatny) – timer działa tylko w pamięci
  }
}

/**
 * Timer przerwy liczony od znacznika czasu końca (a nie od tyknięć), więc jest odporny na uśpienie karty
 * i przeładowanie strony (koniec zapisany w localStorage).
 */
export function useRestTimer() {
  const [endAt, setEndAt] = useState<number | null>(readStored)
  const [now, setNow] = useState(() => Date.now())
  const [finished, setFinished] = useState(false)

  useEffect(() => {
    if (!endAt) return
    const id = setInterval(() => {
      const current = Date.now()
      if (current >= endAt) {
        setEndAt(null)
        store(null)
        setFinished(true)
        navigator.vibrate?.([200, 100, 200])
      } else {
        setNow(current)
      }
    }, 250)
    return () => clearInterval(id)
  }, [endAt])

  const start = useCallback((seconds: number) => {
    if (seconds <= 0) return
    const end = Date.now() + seconds * 1000
    setFinished(false)
    setNow(Date.now())
    setEndAt(end)
    store(end)
  }, [])

  const addSeconds = useCallback((seconds: number) => {
    setEndAt((prev) => {
      const end = (prev ?? Date.now()) + seconds * 1000
      store(end)
      return end
    })
  }, [])

  const stop = useCallback(() => {
    setEndAt(null)
    setFinished(false)
    store(null)
  }, [])

  const remaining = endAt ? Math.max(0, Math.ceil((endAt - now) / 1000)) : 0
  return { running: endAt !== null, remaining, finished, start, addSeconds, stop, dismiss: () => setFinished(false) }
}

export function formatSeconds(total: number) {
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
