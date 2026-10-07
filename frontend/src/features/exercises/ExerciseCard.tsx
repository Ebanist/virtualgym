import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { Exercise } from '../../api/exercises'
import { Badge } from '../../components/Badge'
import styles from './Exercises.module.css'
import { OriginBadge } from './OriginBadge'

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
        <div className={styles.cardTitle}>
          <div className={styles.name}>{exercise.name}</div>
          <div className={styles.muscles}>
            <span className={styles.primaryMuscle}>{t(`muscles.${exercise.primaryMuscle}`)}</span>
            {exercise.secondaryMuscles.length > 0 && (
              <span> · {exercise.secondaryMuscles.map((m) => t(`muscles.${m}`)).join(', ')}</span>
            )}
          </div>
        </div>
        {action}
      </div>
      {exercise.description && <p className={styles.description}>{exercise.description}</p>}
      <div className={styles.chips}>
        <OriginBadge exercise={exercise} />
        {exercise.bodyweight && <Badge>{t('exercises.bodyweight')}</Badge>}
        {exercise.equipmentTypes.map((type) => (
          <Badge key={type.id}>{type.name}</Badge>
        ))}
      </div>
      {children}
    </div>
  )
}
