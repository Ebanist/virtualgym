import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { GymSummary } from '../../api/gyms'
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
            {gym.member && <span className={styles.badge}>{t('gyms.memberBadge')}</span>}
            <div className={styles.meta}>
              {gym.city}, {gym.address} · {t('gyms.members', { count: gym.memberCount })}
            </div>
          </Link>
        </li>
      ))}
    </ul>
  )
}
