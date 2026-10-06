import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useAvailableExercises, type MuscleGroup } from '../../api/exercises'
import { useGym } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { EmptyState } from '../../components/EmptyState'
import { Icon } from '../../components/Icon'
import { PageHeader } from '../../components/PageHeader'
import { SkeletonList } from '../../components/Skeleton'
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
      <PageHeader
        title={t('exercises.availableTitle')}
        subtitle={t('exercises.availableIntro')}
        back={{ to: `/gyms/${gymId}`, label: gym.data?.name ?? t('common.back') }}
        action={
          gym.data?.member && (
            <Link to={`/gyms/${gymId}/exercises/new`} className={buttonClass({ variant: 'primary', small: true })}>
              <Icon name="plus" size={16} />
              {t('exercises.addCustom')}
            </Link>
          )
        }
      />
      <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} />
      {exercises.isError && <Alert kind="error">{errorMessage(t, exercises.error)}</Alert>}
      {exercises.isPending && <SkeletonList />}
      {exercises.data?.length === 0 && <EmptyState icon="search">{t('exercises.noResults')}</EmptyState>}
      <ul className={styles.list}>
        {exercises.data?.map(({ exercise, equipment }) => (
          <li key={exercise.id}>
            <ExerciseCard exercise={exercise}>
              {equipment.length > 0 && (
                <div className={styles.meta}>
                  <Icon name="gym" size={14} /> {t('exercises.onEquipment')}:{' '}
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
