import type { ReactNode } from 'react'
import styles from './Alert.module.css'

export function Alert({ kind = 'info', children }: { kind?: 'error' | 'success' | 'warning' | 'info'; children: ReactNode }) {
  return (
    <div className={`${styles.alert} ${styles[kind]}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
