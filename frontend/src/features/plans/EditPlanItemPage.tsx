import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useAvailableExercises } from '../../api/exercises'
import { planActions, usePlan, usePlanMutation, type PlanItemRequest } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Card } from '../../components/Card'
import { PlanItemForm } from './PlanItemForm'
import styles from './Plans.module.css'

export function EditPlanItemPage() {
  const { planId = '', itemId = '' } = useParams()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const plan = usePlan(planId)
  const available = useAvailableExercises(plan.data?.gymId ?? '', '')
  const update = usePlanMutation((body: PlanItemRequest) => planActions.updateItem(planId, itemId, body))

  if (plan.isPending || (plan.data && available.isPending)) return <p>{t('app.loading')}</p>
  if (plan.isError) return <Alert kind="error">{errorMessage(t, plan.error)}</Alert>
  const item = plan.data.days.flatMap((d) => d.items).find((i) => i.id === itemId)
  if (!item) return <Alert kind="error">{t('errors.not_found')}</Alert>
  const option = available.data?.find((o) => o.exercise.id === item.exercise.id)
  const currentStillValid =
    option && (item.equipment ? option.equipment.some((e) => e.id === item.equipment?.id) : option.exercise.bodyweight)

  return (
    <>
      <p className={styles.meta}>
        <Link to={`/plans/${planId}`}>← {t('plans.backToPlan')}</Link>
      </p>
      <h1>{item.exercise.name}</h1>
      {item.equipmentUnavailable && <Alert kind="warning">{t('plans.chooseOtherEquipment')}</Alert>}
      {!option ? (
        <Alert kind="error">{t('plans.exerciseNoLongerAvailable')}</Alert>
      ) : (
        <Card>
          {update.error && <Alert kind="error">{errorMessage(t, update.error)}</Alert>}
          <PlanItemForm
            option={option}
            submitLabel={t('common.save')}
            defaultValues={{
              ...(currentStillValid ? { equipmentId: item.equipment?.id ?? '' } : {}),
              sets: item.sets,
              repsMin: item.repsMin,
              repsMax: item.repsMax,
              targetWeightKg: item.targetWeightKg ?? '',
              restSeconds: item.restSeconds,
              note: item.note ?? '',
            }}
            onSubmit={async (body) => {
              await update.mutateAsync(body)
              navigate(`/plans/${planId}`)
            }}
          />
        </Card>
      )}
    </>
  )
}
