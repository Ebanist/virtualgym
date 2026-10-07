import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useAvailableExercises, type MuscleGroup } from '../../api/exercises'
import { useGym } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { EmptyState } from '../../components/EmptyState'
import { Icon } from '../../components/Icon'
import { PageHeader } from '../../components/PageHeader'
import { SkeletonList } from '../../components/Skeleton'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { ExerciseCard } from './ExerciseCard'
import { ExerciseFilters } from './ExerciseFilters'
import { ExerciseFormSheet } from './ExerciseFormSheet'
import styles from './Exercises.module.css'
import { MyExerciseActions } from './MyExerciseActions'
import { matchesOrigin, type ExerciseOrigin } from './origin'

export function GymExercisesPage() {
  const { gymId = '' } = useParams()
  const { t } = useTranslation()
  const gym = useGym(gymId)
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<MuscleGroup | ''>('')
  const [origin, setOrigin] = useState<ExerciseOrigin | ''>('')
  const [creating, setCreating] = useState(false)
  const debouncedQ = useDebouncedValue(q)
  const exercises = useAvailableExercises(gymId, debouncedQ, muscle || undefined)
  const visible = exercises.data?.filter((x) => matchesOrigin(x.exercise, origin))
  const member = gym.data?.member

  return (
    <>
      <PageHeader
        title={t('exercises.availableTitle')}
        subtitle={t('exercises.availableIntro')}
        back={{ to: `/gyms/${gymId}`, label: gym.data?.name ?? t('common.back') }}
        action={
          member && (
            <Button variant="primary" small icon="plus" onClick={() => setCreating(true)}>
              {t('exercises.addCustom')}
            </Button>
          )
        }
      />
      <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} origin={origin} onOriginChange={setOrigin} />
      {exercises.isError && <Alert kind="error">{errorMessage(t, exercises.error)}</Alert>}
      {exercises.isPending && <SkeletonList />}
      {visible?.length === 0 && (
        <EmptyState
          icon="search"
          action={
            member && (
              <Button variant="primary" icon="plus" onClick={() => setCreating(true)}>
                {q.trim() ? t('exercises.addNamed', { name: q.trim() }) : t('exercises.addCustom')}
              </Button>
            )
          }
        >
          {t('exercises.noResults')}
        </EmptyState>
      )}
      <ul className={styles.list}>
        {visible?.map(({ exercise, equipment }) => (
          <li key={exercise.id}>
            <ExerciseCard exercise={exercise} action={<MyExerciseActions exercise={exercise} />}>
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
      <ExerciseFormSheet
        open={creating}
        onClose={() => setCreating(false)}
        gymId={gymId}
        initialName={q.trim()}
        onSaved={() => setCreating(false)}
      />
    </>
  )
}
