import { useEffect, useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import { Icon } from './Icon'
import styles from './Sheet.module.css'

interface Props {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}

/** Panel wysuwany od dołu (na desktopie – okno dialogowe). Zamykanie: Esc, tło, przycisk ×. */
export function Sheet({ open, title, onClose, children }: Props) {
  const { t } = useTranslation()
  const titleId = useId()
  const panelRef = useRef<HTMLDivElement>(null)
  // Najnowszy onClose bez restartu efektu (inaczej każdy render rodzica przenosiłby fokus).
  const onCloseRef = useRef(onClose)
  useEffect(() => {
    onCloseRef.current = onClose
  }, [onClose])

  useEffect(() => {
    if (!open) return
    const previous = document.activeElement as HTMLElement | null
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCloseRef.current()
    }
    document.addEventListener('keydown', onKey)
    // Fokus na pierwsze pole formularza (albo panel).
    const first = panelRef.current?.querySelector<HTMLElement>('input, select, textarea, button:not([data-close])')
    ;(first ?? panelRef.current)?.focus()
    return () => {
      document.body.style.overflow = overflow
      document.removeEventListener('keydown', onKey)
      previous?.focus?.()
    }
  }, [open])

  if (!open) return null
  return createPortal(
    <div className={styles.overlay} onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div ref={panelRef} className={styles.panel} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}>
        <div className={styles.handle} aria-hidden />
        <div className={styles.header}>
          <h2 id={titleId}>{title}</h2>
          <button type="button" data-close className={styles.close} aria-label={t('common.close')} onClick={onClose}>
            <Icon name="x" size={20} />
          </button>
        </div>
        <div className={styles.body}>{children}</div>
      </div>
    </div>,
    document.body,
  )
}
