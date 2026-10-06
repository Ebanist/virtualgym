import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { AvailableExercise } from '../../api/exercises'
import type { PlanItemRequest } from '../../api/plans'
import { Button } from '../../components/Button'
import fieldStyles from '../../components/Field.module.css'
import { TextArea } from '../../components/TextArea'
import { TextField } from '../../components/TextField'
import styles from './Plans.module.css'
import { DEFAULT_ITEM, planItemSchema, type PlanItemForm as FormValues, type PlanItemFormInput } from './schemas'

interface Props {
  option: AvailableExercise
  defaultValues?: Partial<PlanItemFormInput>
  submitLabel: string
  onSubmit: (request: PlanItemRequest) => Promise<void>
}

/** Parametry pozycji planu; sprzęt wybierany wyłącznie spośród pasującego sprzętu siłowni. */
export function PlanItemForm({ option, defaultValues, submitLabel, onSubmit }: Props) {
  const { t } = useTranslation()
  const firstEquipment = option.exercise.bodyweight ? '' : (option.equipment[0]?.id ?? '')
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PlanItemFormInput, unknown, FormValues>({
    resolver: zodResolver(planItemSchema),
    defaultValues: { ...DEFAULT_ITEM, equipmentId: firstEquipment, ...defaultValues },
  })

  const submit = handleSubmit((v) =>
    onSubmit({
      exerciseId: option.exercise.id,
      equipmentId: v.equipmentId || undefined,
      sets: v.sets,
      repsMin: v.repsMin,
      repsMax: v.repsMax,
      targetWeightKg: v.targetWeightKg === '' || v.targetWeightKg === undefined ? undefined : v.targetWeightKg,
      restSeconds: v.restSeconds,
      note: v.note || undefined,
    }),
  )

  return (
    <form onSubmit={submit} noValidate>
      <fieldset className={fieldStyles.field} style={{ border: 'none', padding: 0 }}>
        <legend className={fieldStyles.label}>{t('plans.equipment')}</legend>
        {option.exercise.bodyweight && (
          <label className={styles.radio}>
            <input type="radio" value="" {...register('equipmentId')} />
            {t('plans.bodyweight')}
          </label>
        )}
        {option.equipment.map((eq) => (
          <label key={eq.id} className={styles.radio}>
            <input type="radio" value={eq.id} {...register('equipmentId')} />
            {eq.name}
          </label>
        ))}
      </fieldset>
      <div className={styles.grid2}>
        <TextField label={t('plans.sets')} type="number" inputMode="numeric" error={errors.sets?.message} {...register('sets')} />
        <TextField label={t('plans.rest')} type="number" inputMode="numeric" step={15} error={errors.restSeconds?.message} {...register('restSeconds')} />
        <TextField label={t('plans.repsMin')} type="number" inputMode="numeric" error={errors.repsMin?.message} {...register('repsMin')} />
        <TextField label={t('plans.repsMax')} type="number" inputMode="numeric" error={errors.repsMax?.message} {...register('repsMax')} />
      </div>
      <TextField
        label={t('plans.targetWeight')}
        type="number"
        inputMode="decimal"
        step={0.5}
        error={errors.targetWeightKg?.message}
        {...register('targetWeightKg')}
      />
      <TextArea label={t('plans.note')} rows={2} error={errors.note?.message} {...register('note')} />
      <Button type="submit" variant="primary" block disabled={isSubmitting}>
        {submitLabel}
      </Button>
    </form>
  )
}
