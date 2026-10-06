import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import { useMyGyms, type GymSummary } from '../../api/gyms'
import { useCreatePlan } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { SelectField } from '../../components/SelectField'
import { TextArea } from '../../components/TextArea'
import { TextField } from '../../components/TextField'
import { planSchema, type PlanForm } from './schemas'

export function NewPlanPage() {
  const { t } = useTranslation()
  const myGyms = useMyGyms()
  if (myGyms.isPending) return <p>{t('app.loading')}</p>
  if (myGyms.isError) return <Alert kind="error">{errorMessage(t, myGyms.error)}</Alert>
  if (myGyms.data.length === 0) {
    return (
      <>
        <h1>{t('plans.newTitle')}</h1>
        <Alert kind="info">
          {t('plans.joinGymFirst')} <Link to="/gyms">{t('home.findGym')}</Link>
        </Alert>
      </>
    )
  }
  return <NewPlanForm gyms={myGyms.data} />
}

function NewPlanForm({ gyms }: { gyms: GymSummary[] }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const create = useCreatePlan()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PlanForm>({
    resolver: zodResolver(planSchema),
    defaultValues: { gymId: params.get('gymId') ?? gyms[0]?.id ?? '', name: '', description: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      const plan = await create.mutateAsync({ ...values, description: values.description || undefined })
      navigate(`/plans/${plan.id}`, { replace: true })
    } catch (e) {
      if (!applyFieldErrors(e, setError)) setServerError(errorMessage(t, e))
    }
  })

  return (
    <>
      <h1>{t('plans.newTitle')}</h1>
      <Card>
        {serverError && <Alert kind="error">{serverError}</Alert>}
        <form onSubmit={onSubmit} noValidate>
          <SelectField label={t('plans.gym')} error={errors.gymId?.message} {...register('gymId')}>
            {gyms.map((gym) => (
              <option key={gym.id} value={gym.id}>
                {gym.name} ({gym.city})
              </option>
            ))}
          </SelectField>
          <TextField label={t('plans.name')} placeholder={t('plans.namePlaceholder')} error={errors.name?.message} {...register('name')} />
          <TextArea label={t('plans.descriptionOptional')} error={errors.description?.message} {...register('description')} />
          <Button type="submit" variant="primary" block disabled={isSubmitting}>
            {t('plans.create')}
          </Button>
        </form>
      </Card>
    </>
  )
}
