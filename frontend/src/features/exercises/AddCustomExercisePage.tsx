import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { useEquipmentList } from '../../api/equipment'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import { MUSCLE_GROUPS, useCreateCustomExercise } from '../../api/exercises'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import fieldStyles from '../../components/Field.module.css'
import { SelectField } from '../../components/SelectField'
import { TextArea } from '../../components/TextArea'
import { TextField } from '../../components/TextField'
import styles from './Exercises.module.css'
import { customExerciseSchema, type CustomExerciseForm } from './schemas'

export function AddCustomExercisePage() {
  const { gymId = '' } = useParams()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const create = useCreateCustomExercise(gymId)
  const equipment = useEquipmentList(gymId, { status: 'ACTIVE', size: 100 })
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CustomExerciseForm>({
    resolver: zodResolver(customExerciseSchema),
    defaultValues: { secondaryMuscles: [], equipmentIds: [], bodyweight: false },
  })

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      await create.mutateAsync({ ...values, description: values.description || undefined })
      navigate(`/gyms/${gymId}/exercises`, { replace: true })
    } catch (e) {
      if (!applyFieldErrors(e, setError)) setServerError(errorMessage(t, e))
    }
  })

  return (
    <>
      <h1>{t('exercises.addCustomTitle')}</h1>
      <Card>
        <p className={styles.meta}>{t('exercises.addCustomIntro')}</p>
        {serverError && <Alert kind="error">{serverError}</Alert>}
        <form onSubmit={onSubmit} noValidate>
          <TextField label={t('exercises.name')} error={errors.name?.message} {...register('name')} />
          <SelectField label={t('exercises.primaryMuscle')} error={errors.primaryMuscle?.message} {...register('primaryMuscle')}>
            <option value="">{t('exercises.chooseMuscle')}</option>
            {MUSCLE_GROUPS.map((m) => (
              <option key={m} value={m}>
                {t(`muscles.${m}`)}
              </option>
            ))}
          </SelectField>
          <fieldset className={styles.group}>
            <legend>{t('exercises.secondaryMuscles')}</legend>
            <div className={styles.checkboxes}>
              {MUSCLE_GROUPS.map((m) => (
                <label key={m} className={styles.checkbox}>
                  <input type="checkbox" value={m} {...register('secondaryMuscles')} />
                  {t(`muscles.${m}`)}
                </label>
              ))}
            </div>
          </fieldset>
          <TextArea label={t('exercises.descriptionOptional')} error={errors.description?.message} {...register('description')} />
          <label className={styles.checkbox}>
            <input type="checkbox" {...register('bodyweight')} />
            {t('exercises.bodyweightLabel')}
          </label>
          <fieldset className={styles.group}>
            <legend>{t('exercises.equipment')}</legend>
            {equipment.data?.content.length === 0 && <p className={styles.meta}>{t('exercises.noEquipment')}</p>}
            <div className={styles.checkboxes}>
              {equipment.data?.content.map((eq) => (
                <label key={eq.id} className={styles.checkbox}>
                  <input type="checkbox" value={eq.id} {...register('equipmentIds')} />
                  {eq.name}
                </label>
              ))}
            </div>
            {errors.equipmentIds?.message && (
              <span className={fieldStyles.error} role="alert">
                {t(errors.equipmentIds.message)}
              </span>
            )}
          </fieldset>
          <Button type="submit" variant="primary" block disabled={isSubmitting}>
            {t('exercises.save')}
          </Button>
        </form>
      </Card>
    </>
  )
}
