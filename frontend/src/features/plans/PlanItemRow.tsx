import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import type { PlanItem } from '../../api/plans'
import { Button } from '../../components/Button'
import { buttonClass } from '../../components/buttonClass'
import { Icon } from '../../components/Icon'
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
  const reps = item.repsMin === item.repsMax ? `${item.repsMin}` : `${item.repsMin}–${item.repsMax}`
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
          <Icon name="arrowUp" size={18} />
        </button>
        <span className={styles.position}>{item.position + 1}</span>
        <button
          type="button"
          className={styles.iconButton}
          aria-label={t('plans.moveDown', { name: item.exercise.name })}
          disabled={isLast || busy}
          onClick={() => onMove('DOWN')}
        >
          <Icon name="arrowDown" size={18} />
        </button>
      </div>
      <div className={styles.itemBody}>
        <div className={styles.itemTitle}>{item.exercise.name}</div>
        <div className={styles.itemEquipment}>
          <Icon name="gym" size={14} />
          {item.equipment ? item.equipment.name : t('plans.bodyweight')}
        </div>
        {item.equipmentUnavailable && (
          <div className={styles.warning} role="note">
            <Icon name="warning" size={14} />
            {item.equipment?.deleted ? t('plans.equipmentDeleted') : t('plans.equipmentRemoved')}
          </div>
        )}
        <div className={styles.params} aria-label={formatParamsLabel(t, item)}>
          <span className={styles.param}>
            <strong>{item.sets}</strong> × <strong>{reps}</strong>
          </span>
          {item.targetWeightKg !== undefined && item.targetWeightKg !== null && (
            <span className={styles.param}>
              <strong>{item.targetWeightKg}</strong> kg
            </span>
          )}
          <span className={styles.param}>
            <Icon name="timer" size={13} /> {item.restSeconds} s
          </span>
        </div>
        {item.note && <div className={styles.note}>{item.note}</div>}
      </div>
      <div className={styles.itemActions}>
        <Link
          to={`/plans/${planId}/items/${item.id}/edit`}
          className={buttonClass({ small: true, iconOnly: true, variant: 'ghost' })}
          aria-label={t('plans.editItem', { name: item.exercise.name })}
        >
          <Icon name="edit" size={16} />
        </Link>
        <Button
          small
          iconOnly
          variant="ghost"
          icon="trash"
          aria-label={t('plans.deleteItem', { name: item.exercise.name })}
          disabled={busy}
          onClick={onDelete}
        />
      </div>
    </li>
  )
}

function formatParamsLabel(t: (key: string, o?: Record<string, unknown>) => string, item: PlanItem) {
  return t('plans.paramsLabel', {
    sets: item.sets,
    reps: item.repsMin === item.repsMax ? item.repsMin : `${item.repsMin}–${item.repsMax}`,
    rest: item.restSeconds,
  })
}
