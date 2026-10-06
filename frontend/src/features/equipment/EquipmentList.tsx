import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { EquipmentSummary } from '../../api/equipment'
import styles from './Equipment.module.css'

export function EquipmentList({ items, emptyText }: { items: EquipmentSummary[]; emptyText: string }) {
  const { t } = useTranslation()
  if (items.length === 0) return <p className={styles.meta}>{emptyText}</p>
  return (
    <ul className={styles.list}>
      {items.map((item) => {
        const removed = item.status === 'REMOVED_FROM_GYM'
        return (
          <li key={item.id}>
            <Link to={`/equipment/${item.id}`} className={styles.item}>
              {item.thumbnailUrl ? (
                <img src={item.thumbnailUrl} alt="" className={styles.thumb} loading="lazy" />
              ) : (
                <span className={styles.thumbPlaceholder} aria-hidden>
                  ⚙
                </span>
              )}
              <span className={styles.itemBody}>
                <span className={[styles.itemTitle, removed && styles.removed].filter(Boolean).join(' ')}>
                  {item.name}
                </span>
                <span className={styles.meta}>
                  {' · '}
                  {t(`equipment.category.${item.category}`)}
                  {item.quantity ? ` · ${t('equipment.quantityShort', { count: item.quantity })}` : ''}
                </span>
                {(removed || item.openReportCount > 0) && (
                  <span className={styles.badges}>
                    {removed && <span className={styles.badgeWarning}>{t('equipment.status.REMOVED_FROM_GYM')}</span>}
                    {item.openReportCount > 0 && (
                      <span className={styles.badgeWarning}>
                        {t('equipment.openReports', { count: item.openReportCount })}
                      </span>
                    )}
                  </span>
                )}
              </span>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
