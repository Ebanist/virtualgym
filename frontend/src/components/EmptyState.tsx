import type { ReactNode } from 'react'
import styles from './EmptyState.module.css'
import { Icon, type IconName } from './Icon'

export function EmptyState({ icon = 'bolt', children, action }: { icon?: IconName; children: ReactNode; action?: ReactNode }) {
  return (
    <div className={styles.empty}>
      <span className={styles.icon}>
        <Icon name={icon} size={26} />
      </span>
      <p className={styles.text}>{children}</p>
      {action}
    </div>
  )
}
