import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { ApiError, applyFieldErrors, errorMessage } from '../../api/errors'
import { useCreateGym, useSimilarGyms, type GymSummary } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { TextArea } from '../../components/TextArea'
import { TextField } from '../../components/TextField'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { GymList } from './GymList'
import { gymSchema, type GymForm } from './schemas'

export function AddGymPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const createGym = useCreateGym()
  const [serverError, setServerError] = useState<string | null>(null)
  const [duplicates, setDuplicates] = useState<GymSummary[] | null>(null)
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors },
  } = useForm<GymForm>({ resolver: zodResolver(gymSchema) })

  const name = useDebouncedValue(useWatch({ control, name: 'name' }) ?? '', 400)
  const city = useDebouncedValue(useWatch({ control, name: 'city' }) ?? '', 400)
  const similar = useSimilarGyms(name, city)

  const submit = (confirmDuplicate: boolean) =>
    handleSubmit(async (values) => {
      setServerError(null)
      try {
        const gym = await createGym.mutateAsync({ ...values, confirmDuplicate })
        navigate(`/gyms/${gym.id}`, { replace: true })
      } catch (e) {
        if (e instanceof ApiError && e.code === 'gym_possible_duplicate') {
          setDuplicates((e.problem.candidates as GymSummary[] | undefined) ?? [])
        } else if (!applyFieldErrors(e, setError)) {
          setServerError(errorMessage(t, e))
        }
      }
    })

  const hints = duplicates ?? similar.data ?? []

  return (
    <>
      <h1>{t('gyms.addTitle')}</h1>
      <Card>
        {serverError && <Alert kind="error">{serverError}</Alert>}
        <form onSubmit={submit(false)} noValidate>
          <TextField label={t('gyms.name')} error={errors.name?.message} {...register('name')} />
          <TextField label={t('gyms.city')} error={errors.city?.message} {...register('city')} />
          {hints.length > 0 && (
            <Alert kind="warning">
              <p>{duplicates ? t('gyms.duplicateWarning') : t('gyms.similarHint')}</p>
              <GymList gyms={hints} emptyText="" />
            </Alert>
          )}
          <TextField label={t('gyms.address')} error={errors.address?.message} {...register('address')} />
          <TextArea label={t('gyms.descriptionOptional')} error={errors.description?.message} {...register('description')} />
          {duplicates ? (
            <Button variant="primary" block disabled={createGym.isPending} onClick={submit(true)}>
              {t('gyms.addAnyway')}
            </Button>
          ) : (
            <Button type="submit" variant="primary" block disabled={createGym.isPending}>
              {t('gyms.add')}
            </Button>
          )}
        </form>
      </Card>
    </>
  )
}
