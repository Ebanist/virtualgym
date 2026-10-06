import { useTranslation } from 'react-i18next'
import { Icon } from '../../components/Icon'
import styles from './Workout.module.css'
import { formatSeconds } from './useRestTimer'

interface Props {
  running: boolean
  finished: boolean
  remaining: number
  total: number
  onAdd: () => void
  onStop: () => void
  onDismiss: () => void
}

const R = 20
const CIRCUMFERENCE = 2 * Math.PI * R

/** Pasek przerwy nad dolną nawigacją: okrągły wskaźnik postępu + czas + akcje. */
export function RestTimerBar({ running, finished, remaining, total, onAdd, onStop, onDismiss }: Props) {
  const { t } = useTranslation()
  if (!running && !finished) return null
  if (finished) {
    return (
      <div className={`${styles.timer} ${styles.timerDone}`} role="status" aria-live="assertive">
        <span className={styles.timerLabel}>
          <Icon name="bolt" size={22} />
          <span className={styles.timerValue}>{t('workout.restOver')}</span>
        </span>
        <button type="button" className={styles.timerButton} onClick={onDismiss}>
          {t('common.close')}
        </button>
      </div>
    )
  }
  const progress = total > 0 ? Math.min(1, remaining / total) : 0
  return (
    <div className={styles.timer} role="timer" aria-label={t('workout.rest')}>
      <span className={styles.timerLabel}>
        <svg className={styles.ring} viewBox="0 0 48 48" aria-hidden="true">
          <circle cx="24" cy="24" r={R} className={styles.ringTrack} />
          <circle
            cx="24"
            cy="24"
            r={R}
            className={styles.ringValue}
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={CIRCUMFERENCE * (1 - progress)}
          />
        </svg>
        <span>
          <span className={styles.timerCaption}>{t('workout.rest')}</span>
          <span className={styles.timerValue}>{formatSeconds(remaining)}</span>
        </span>
      </span>
      <span className={styles.timerButtons}>
        <button type="button" className={styles.timerButton} onClick={onAdd}>
          +15 s
        </button>
        <button type="button" className={styles.timerButton} onClick={onStop}>
          {t('workout.skipRest')}
        </button>
      </span>
    </div>
  )
}
