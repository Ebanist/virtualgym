import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { Exercise } from '../../api/exercises'
import styles from './Exercises.module.css'

interface Props {
  exercise: Exercise
  /** Dodatkowa treść pod opisem (np. lista sprzętu). */
  children?: ReactNode
  action?: ReactNode
}

export function ExerciseCard({ exercise, children, action }: Props) {
  const { t } = useTranslation()
  return (
    <div className={styles.card}>
      <div className={styles.cardHeader}>
        <div>
          <div className={styles.name}>{exercise.name}</div>
          <div className={styles.meta}>
            {t(`muscles.${exercise.primaryMuscle}`)}
            {exercise.secondaryMuscles.length > 0 &&
              ` · ${exercise.secondaryMuscles.map((m) => t(`muscles.${m}`)).join(', ')}`}
          </div>
        </div>
        {action}
      </div>
      {exercise.description && <p className={styles.description}>{exercise.description}</p>}
      <div className={styles.chips}>
        {exercise.bodyweight && <span className={styles.chip}>{t('exercises.bodyweight')}</span>}
        {exercise.scope === 'CUSTOM' && <span className={styles.chip}>{t('exercises.custom')}</span>}
        {exercise.equipmentTypes.map((type) => (
          <span key={type.id} className={styles.chip}>
            {type.name}
          </span>
        ))}
      </div>
      {children}
    </div>
  )
}
