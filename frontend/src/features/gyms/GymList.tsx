import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { GymSummary } from '../../api/gyms'
import { Badge } from '../../components/Badge'
import { Icon } from '../../components/Icon'
import styles from '../../components/List.module.css'

export function GymList({ gyms, emptyText }: { gyms: GymSummary[]; emptyText: string }) {
  const { t } = useTranslation()
  if (gyms.length === 0) return <p className={styles.empty}>{emptyText}</p>
  return (
    <ul className={styles.list}>
      {gyms.map((gym) => (
        <li key={gym.id}>
          <Link to={`/gyms/${gym.id}`} className={styles.item}>
            <span className={styles.title}>{gym.name}</span>
            {gym.member && (
              <span className={styles.badge}>
                <Badge tone="accent" icon="check">
                  {t('gyms.memberBadge')}
                </Badge>
              </span>
            )}
            <div className={styles.meta}>
              <span className={styles.metaItem}>
                <Icon name="pin" size={14} />
                {gym.city}, {gym.address}
              </span>
              <span className={styles.metaItem}>
                <Icon name="users" size={14} />
                {t('gyms.members', { count: gym.memberCount })}
              </span>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  )
}
