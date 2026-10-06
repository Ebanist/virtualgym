import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { PlanItem } from '../../api/plans'
import { Button } from '../../components/Button'
import { buttonClass } from '../../components/buttonClass'
import { formatItemParams } from './format'
import styles from './Plans.module.css'

interface Props {
  planId: string
  item: PlanItem
  isFirst: boolean
  isLast: boolean
  busy?: boolean
  onMove: (direction: 'UP' | 'DOWN') => void
  onDelete: () => void
}

export function PlanItemRow({ planId, item, isFirst, isLast, busy, onMove, onDelete }: Props) {
  const { t } = useTranslation()
  return (
    <li className={[styles.item, item.equipmentUnavailable && styles.itemWarning].filter(Boolean).join(' ')}>
      <div className={styles.moveButtons}>
        <button
          type="button"
          className={styles.iconButton}
          aria-label={t('plans.moveUp', { name: item.exercise.name })}
          disabled={isFirst || busy}
          onClick={() => onMove('UP')}
        >
          ↑
        </button>
        <button
          type="button"
          className={styles.iconButton}
          aria-label={t('plans.moveDown', { name: item.exercise.name })}
          disabled={isLast || busy}
          onClick={() => onMove('DOWN')}
        >
          ↓
        </button>
      </div>
      <div className={styles.itemBody}>
        <div className={styles.itemTitle}>{item.exercise.name}</div>
        <div className={styles.meta}>{item.equipment ? item.equipment.name : t('plans.bodyweight')}</div>
        {item.equipmentUnavailable && (
          <div className={styles.warning} role="note">
            ⚠ {item.equipment?.deleted ? t('plans.equipmentDeleted') : t('plans.equipmentRemoved')}
          </div>
        )}
        <div>{formatItemParams(t, item)}</div>
        {item.note && <div className={styles.meta}>{item.note}</div>}
        <div className={styles.itemActions}>
          <Link to={`/plans/${planId}/items/${item.id}/edit`} className={buttonClass({ small: true })}>
            {t('common.edit')}
          </Link>
          <Button small variant="ghost" disabled={busy} onClick={onDelete}>
            {t('common.delete')}
          </Button>
        </div>
      </div>
    </li>
  )
}
