import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useAvailableExercises, type AvailableExercise, type MuscleGroup } from '../../api/exercises'
import { planActions, usePlan, usePlanMutation, type PlanItemRequest } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { ExerciseCard } from '../exercises/ExerciseCard'
import { ExerciseFilters } from '../exercises/ExerciseFilters'
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
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<MuscleGroup | ''>('')
  const [selected, setSelected] = useState<AvailableExercise | null>(null)
  const available = useAvailableExercises(gymId, useDebouncedValue(q), muscle || undefined)
  const add = usePlanMutation((body: PlanItemRequest) => planActions.addItem(planId, dayId, body))

  return (
    <>
      <p className={styles.meta}>
        <Link to={`/plans/${planId}`}>← {t('plans.backToPlan')}</Link>
      </p>
      <h1>{t('plans.addExerciseTo', { day: dayName })}</h1>
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
          <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} />
          {available.isError && <Alert kind="error">{errorMessage(t, available.error)}</Alert>}
          {available.isPending && <p>{t('app.loading')}</p>}
          {available.data?.length === 0 && <p className={styles.meta}>{t('exercises.noResults')}</p>}
          <ul className={styles.pickList}>
            {available.data?.map((option) => (
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
        </>
      )}
    </>
  )
}
