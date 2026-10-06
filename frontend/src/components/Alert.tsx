import type { ReactNode } from 'react'
import styles from './Alert.module.css'
import { Icon, type IconName } from './Icon'

type Kind = 'error' | 'success' | 'warning' | 'info'

const ICONS: Record<Kind, IconName> = { error: 'warning', success: 'check', warning: 'warning', info: 'bolt' }

export function Alert({ kind = 'info', children }: { kind?: Kind; children: ReactNode }) {
  return (
    <div className={`${styles.alert} ${styles[kind]}`} role={kind === 'error' ? 'alert' : 'status'}>
      <Icon name={ICONS[kind]} size={18} className={styles.icon} />
      <div className={styles.body}>{children}</div>
    </div>
  )
}
