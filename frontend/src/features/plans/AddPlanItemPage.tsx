import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { findAvailableExercise, useAvailableExercises, type AvailableExercise, type MuscleGroup } from '../../api/exercises'
import { planActions, usePlan, usePlanMutation, type PlanItemRequest } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { EmptyState } from '../../components/EmptyState'
import { PageHeader } from '../../components/PageHeader'
import { SkeletonList } from '../../components/Skeleton'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { ExerciseCard } from '../exercises/ExerciseCard'
import { ExerciseFilters } from '../exercises/ExerciseFilters'
import { ExerciseFormSheet } from '../exercises/ExerciseFormSheet'
import { matchesOrigin, type ExerciseOrigin } from '../exercises/origin'
import { PlanItemForm } from './PlanItemForm'
import styles from './Plans.module.css'

export function AddPlanItemPage() {
  const { planId = '', dayId = '' } = useParams()
  const { t } = useTranslation()
  const plan = usePlan(planId)
  if (plan.isPending) return <p>{t('app.loading')}</p>
  if (plan.isError) return <Alert kind="error">{errorMessage(t, plan.error)}</Alert>
  const day = plan.data.days.find((d) => d.id === dayId)
  if (!day) return <Alert kind="error">{t('errors.not_found')}</Alert>
  return <AddItem planId={planId} dayId={dayId} dayName={day.name} gymId={plan.data.gymId} />
}

function AddItem({ planId, dayId, dayName, gymId }: { planId: string; dayId: string; dayName: string; gymId: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<MuscleGroup | ''>('')
  const [origin, setOrigin] = useState<ExerciseOrigin | ''>('')
  const [selected, setSelected] = useState<AvailableExercise | null>(null)
  const [creating, setCreating] = useState(false)
  const [pickError, setPickError] = useState<string | null>(null)
  const available = useAvailableExercises(gymId, useDebouncedValue(q), muscle || undefined)
  const options = available.data?.filter((o) => matchesOrigin(o.exercise, origin))
  const add = usePlanMutation((body: PlanItemRequest) => planActions.addItem(planId, dayId, body))

  /** Po utworzeniu (lub wskazaniu istniejącego) ćwiczenia – od razu przejście do parametrów. */
  const selectById = async (exerciseId: string) => {
    setCreating(false)
    const option = await findAvailableExercise(queryClient, gymId, exerciseId)
    if (option) {
      setPickError(null)
      setSelected(option)
    } else {
      setPickError(t('plans.exerciseNotAvailableHere'))
    }
  }

  const newExerciseButton = (
    <Button variant="primary" small icon="plus" onClick={() => setCreating(true)}>
      {t('exercises.newExercise')}
    </Button>
  )

  return (
    <>
      <PageHeader
        title={t('plans.addExerciseTo', { day: dayName })}
        back={{ to: `/plans/${planId}`, label: t('plans.backToPlan') }}
        action={!selected && newExerciseButton}
      />
      {selected ? (
        <Card>
          <ExerciseCard
            exercise={selected.exercise}
            action={
              <Button small onClick={() => setSelected(null)}>
                {t('plans.changeExercise')}
              </Button>
            }
          />
          {add.error && <Alert kind="error">{errorMessage(t, add.error)}</Alert>}
          <PlanItemForm
            option={selected}
            submitLabel={t('plans.addToDay')}
            onSubmit={async (body) => {
              await add.mutateAsync(body)
              navigate(`/plans/${planId}`)
            }}
          />
        </Card>
      ) : (
        <>
          <p className={styles.meta}>{t('plans.onlyAvailable')}</p>
          {pickError && <Alert kind="warning">{pickError}</Alert>}
          <ExerciseFilters
            q={q}
            muscle={muscle}
            onQChange={setQ}
            onMuscleChange={setMuscle}
            origin={origin}
            onOriginChange={setOrigin}
          />
          {available.isError && <Alert kind="error">{errorMessage(t, available.error)}</Alert>}
          {available.isPending && <SkeletonList />}
          {options?.length === 0 && (
            <EmptyState
              icon="search"
              action={
                <Button variant="primary" icon="plus" onClick={() => setCreating(true)}>
                  {q.trim() ? t('exercises.addNamed', { name: q.trim() }) : t('exercises.newExercise')}
                </Button>
              }
            >
              {t('plans.noExerciseFound')}
            </EmptyState>
          )}
          <ul className={styles.pickList}>
            {options?.map((option) => (
              <li key={option.exercise.id}>
                <ExerciseCard
                  exercise={option.exercise}
                  action={
                    <Button small variant="primary" onClick={() => setSelected(option)}>
                      {t('plans.choose')}
                    </Button>
                  }
                >
                  {option.equipment.length > 0 && (
                    <div className={styles.meta}>
                      {t('exercises.onEquipment')}: {option.equipment.map((e) => e.name).join(', ')}
                    </div>
                  )}
                </ExerciseCard>
              </li>
            ))}
          </ul>
          {options && options.length > 0 && (
            <div className={styles.newExerciseHint}>
              <span>{t('plans.missingExercise')}</span>
              {newExerciseButton}
            </div>
          )}
        </>
      )}
      <ExerciseFormSheet
        open={creating}
        onClose={() => setCreating(false)}
        gymId={gymId}
        initialName={q.trim()}
        onSaved={(exercise) => void selectById(exercise.id)}
        onPickExisting={(exercise) => void selectById(exercise.id)}
      />
    </>
  )
}
