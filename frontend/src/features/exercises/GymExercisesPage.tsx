import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useAvailableExercises, type MuscleGroup } from '../../api/exercises'
import { useGym } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { ExerciseCard } from './ExerciseCard'
import { ExerciseFilters } from './ExerciseFilters'
import styles from './Exercises.module.css'

export function GymExercisesPage() {
  const { gymId = '' } = useParams()
  const { t } = useTranslation()
  const gym = useGym(gymId)
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<MuscleGroup | ''>('')
  const debouncedQ = useDebouncedValue(q)
  const exercises = useAvailableExercises(gymId, debouncedQ, muscle || undefined)

  return (
    <>
      <p className={styles.meta}>
        <Link to={`/gyms/${gymId}`}>← {gym.data?.name ?? t('common.back')}</Link>
      </p>
      <div className={styles.header}>
        <h1>{t('exercises.availableTitle')}</h1>
        {gym.data?.member && (
          <Link to={`/gyms/${gymId}/exercises/new`} className={buttonClass({ variant: 'primary', small: true })}>
            {t('exercises.addCustom')}
          </Link>
        )}
      </div>
      <p className={styles.meta}>{t('exercises.availableIntro')}</p>
      <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} />
      {exercises.isError && <Alert kind="error">{errorMessage(t, exercises.error)}</Alert>}
      {exercises.isPending && <p>{t('app.loading')}</p>}
      {exercises.data?.length === 0 && <p className={styles.meta}>{t('exercises.noResults')}</p>}
      <ul className={styles.list}>
        {exercises.data?.map(({ exercise, equipment }) => (
          <li key={exercise.id}>
            <ExerciseCard exercise={exercise}>
              {equipment.length > 0 && (
                <div className={styles.meta}>
                  {t('exercises.onEquipment')}:{' '}
                  {equipment.map((eq, i) => (
                    <span key={eq.id}>
                      {i > 0 && ', '}
                      <Link to={`/equipment/${eq.id}`}>{eq.name}</Link>
                    </span>
                  ))}
                </div>
              )}
            </ExerciseCard>
          </li>
        ))}
      </ul>
    </>
  )
}
