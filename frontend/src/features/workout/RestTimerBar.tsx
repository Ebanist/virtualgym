import { useTranslation } from 'react-i18next'
import styles from './Workout.module.css'
import { formatSeconds } from './useRestTimer'

interface Props {
  running: boolean
  finished: boolean
  remaining: number
  onAdd: () => void
  onStop: () => void
  onDismiss: () => void
}

export function RestTimerBar({ running, finished, remaining, onAdd, onStop, onDismiss }: Props) {
  const { t } = useTranslation()
  if (!running && !finished) return null
  if (finished) {
    return (
      <div className={styles.timerDone} role="status" aria-live="assertive">
        <span className={styles.timerValue}>{t('workout.restOver')}</span>
        <button type="button" className={styles.timerButton} onClick={onDismiss}>
          {t('common.close')}
        </button>
      </div>
    )
  }
  return (
    <div className={styles.timer} role="timer" aria-label={t('workout.rest')}>
      <span>
        {t('workout.rest')} <span className={styles.timerValue}>{formatSeconds(remaining)}</span>
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
