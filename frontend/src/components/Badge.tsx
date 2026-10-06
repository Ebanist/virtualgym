import type { ReactNode } from 'react'
import styles from './Badge.module.css'
import { Icon, type IconName } from './Icon'

export type BadgeTone = 'neutral' | 'accent' | 'warning' | 'danger' | 'info'

export function Badge({ tone = 'neutral', icon, children }: { tone?: BadgeTone; icon?: IconName; children: ReactNode }) {
  return (
    <span className={[styles.badge, tone !== 'neutral' && styles[tone]].filter(Boolean).join(' ')}>
      {icon && <Icon name={icon} size={12} />}
      {children}
    </span>
  )
}
