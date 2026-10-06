import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { EquipmentSummary } from '../../api/equipment'
import { Badge } from '../../components/Badge'
import { Icon } from '../../components/Icon'
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
            <Link to={`/equipment/${item.id}`} className={[styles.item, removed && styles.itemRemoved].filter(Boolean).join(' ')}>
              {item.thumbnailUrl ? (
                <img src={item.thumbnailUrl} alt="" className={styles.thumb} loading="lazy" />
              ) : (
                <span className={styles.thumbPlaceholder} aria-hidden>
                  <Icon name="gym" size={26} />
                </span>
              )}
              <span className={styles.itemBody}>
                <span className={[styles.itemTitle, removed && styles.removed].filter(Boolean).join(' ')}>{item.name}</span>
                <span className={styles.meta}>
                  {t(`equipment.category.${item.category}`)}
                  {item.quantity ? ` · ${t('equipment.quantityShort', { count: item.quantity })}` : ''}
                </span>
                {(removed || item.openReportCount > 0) && (
                  <span className={styles.badges}>
                    {removed && (
                      <Badge tone="warning" icon="warning">
                        {t('equipment.status.REMOVED_FROM_GYM')}
                      </Badge>
                    )}
                    {item.openReportCount > 0 && (
                      <Badge tone="danger" icon="flag">
                        {t('equipment.openReports', { count: item.openReportCount })}
                      </Badge>
                    )}
                  </span>
                )}
              </span>
              <Icon name="chevronRight" size={18} className={styles.chevron} />
            </Link>
          </li>
        )
      })}
    </ul>
  )
}
