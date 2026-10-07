import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useEquipmentList } from '../../api/equipment'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import {
  MUSCLE_GROUPS,
  useCreateCustomExercise,
  useSimilarExercises,
  useUpdateExercise,
  type Exercise,
} from '../../api/exercises'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import fieldStyles from '../../components/Field.module.css'
import { Segmented } from '../../components/Segmented'
import { SelectField } from '../../components/SelectField'
import { Sheet } from '../../components/Sheet'
import { TextArea } from '../../components/TextArea'
import { TextField } from '../../components/TextField'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import styles from './Exercises.module.css'
import { OriginBadge } from './OriginBadge'
import { customExerciseSchema, type CustomExerciseForm } from './schemas'

interface Props {
  open: boolean
  onClose: () => void
  gymId: string
  /** Edycja istniejącego ćwiczenia (z equipmentIds) – inaczej tworzenie. */
  exercise?: Exercise
  initialName?: string
  /** Sprzęt zaznaczony na start (np. dodawanie ze strony sprzętu). */
  initialEquipmentIds?: string[]
  onSaved: (exercise: Exercise) => void
  /** Gdy podane – przy podpowiedziach podobnych ćwiczeń pojawia się „Użyj tego”. */
  onPickExisting?: (exercise: Exercise) => void
}

/** Panel dodawania / edycji własnego ćwiczenia – wspólny dla planu, sprzętu, treningu i listy ćwiczeń. */
export function ExerciseFormSheet(props: Props) {
  const { t } = useTranslation()
  return (
    <Sheet open={props.open} onClose={props.onClose} title={props.exercise ? t('exercises.editTitle') : t('exercises.newTitle')}>
      {/* Formularz montowany przy każdym otwarciu – świeże wartości początkowe. */}
      {props.open && <ExerciseForm {...props} />}
    </Sheet>
  )
}

function ExerciseForm({ gymId, exercise, initialName, initialEquipmentIds, onSaved, onPickExisting }: Props) {
  const { t } = useTranslation()
  const create = useCreateCustomExercise(gymId)
  const update = useUpdateExercise()
  const equipment = useEquipmentList(gymId, { status: 'ACTIVE', size: 100 })
  const [serverError, setServerError] = useState<string | null>(null)
  const defaultValues: Partial<CustomExerciseForm> = exercise
    ? {
        name: exercise.name,
        primaryMuscle: exercise.primaryMuscle,
        secondaryMuscles: exercise.secondaryMuscles,
        description: exercise.description ?? '',
        bodyweight: exercise.bodyweight,
        equipmentIds: (exercise.equipmentIds ?? []).filter((id): id is string => !!id),
        visibility: exercise.visibility ?? 'PRIVATE',
      }
    : {
        name: initialName ?? '',
        secondaryMuscles: [],
        description: '',
        bodyweight: false,
        equipmentIds: initialEquipmentIds ?? [],
        visibility: 'PRIVATE',
      }
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CustomExerciseForm>({
    resolver: zodResolver(customExerciseSchema),
    defaultValues,
  })

  const name = useDebouncedValue(useWatch({ control, name: 'name' }) ?? '', 400)
  const similar = useSimilarExercises(gymId, name, !exercise)
  const similarOthers = similar.data?.filter((s) => s.id !== exercise?.id) ?? []

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    const body = { ...values, description: values.description || undefined }
    try {
      const saved = exercise ? await update.mutateAsync({ id: exercise.id, body }) : await create.mutateAsync(body)
      onSaved(saved)
    } catch (e) {
      if (!applyFieldErrors(e, setError)) setServerError(errorMessage(t, e))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate>
      {serverError && <Alert kind="error">{serverError}</Alert>}
      <TextField label={t('exercises.name')} error={errors.name?.message} {...register('name')} />
      {similarOthers.length > 0 && (
        <Alert kind="warning">
          <p>{t('exercises.similarHint')}</p>
          <ul className={styles.similarList}>
            {similarOthers.map((s) => (
              <li key={s.id}>
                <span>
                  {s.name} <OriginBadge exercise={s} />
                </span>
                {onPickExisting && (
                  <Button small onClick={() => onPickExisting(s)}>
                    {t('exercises.useThis')}
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </Alert>
      )}
      <SelectField label={t('exercises.primaryMuscle')} error={errors.primaryMuscle?.message} {...register('primaryMuscle')}>
        <option value="">{t('exercises.chooseMuscle')}</option>
        {MUSCLE_GROUPS.map((m) => (
          <option key={m} value={m}>
            {t(`muscles.${m}`)}
          </option>
        ))}
      </SelectField>

      <fieldset className={styles.group}>
        <legend>{t('exercises.equipment')}</legend>
        <label className={styles.checkbox}>
          <input type="checkbox" {...register('bodyweight')} />
          {t('exercises.bodyweightLabel')}
        </label>
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

      <details className={styles.more}>
        <summary>{t('exercises.moreDetails')}</summary>
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
      </details>

      <div className={fieldStyles.field}>
        <span className={fieldStyles.label}>{t('exercises.visibilityLabel')}</span>
        <Controller
          control={control}
          name="visibility"
          render={({ field }) => (
            <>
              <Segmented
                label={t('exercises.visibilityLabel')}
                value={field.value}
                onChange={field.onChange}
                options={[
                  { value: 'PRIVATE', label: t('exercises.visibility.PRIVATE'), icon: 'lock' },
                  { value: 'GYM', label: t('exercises.visibility.GYM'), icon: 'users' },
                ]}
              />
              <span className={fieldStyles.hint}>{t(`exercises.visibilityHint.${field.value}`)}</span>
            </>
          )}
        />
      </div>

      <Button type="submit" variant="primary" block icon="check" disabled={isSubmitting}>
        {exercise ? t('common.save') : t('exercises.save')}
      </Button>
    </form>
  )
}
