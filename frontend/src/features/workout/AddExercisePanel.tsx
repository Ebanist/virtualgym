import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAvailableExercises } from '../../api/exercises'
import type { AddSessionExerciseRequest } from '../../api/workouts'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { SelectField } from '../../components/SelectField'
import { TextField } from '../../components/TextField'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import styles from './Workout.module.css'

/** Dodanie ćwiczenia do treningu – tylko ćwiczenia dostępne w siłowni treningu. */
export function AddExercisePanel({ gymId, onAdd, onClose, pending }: {
  gymId: string
  onAdd: (request: AddSessionExerciseRequest) => void
  onClose: () => void
  pending: boolean
}) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const available = useAvailableExercises(gymId, useDebouncedValue(q))
  const [equipmentChoice, setEquipmentChoice] = useState<Record<string, string>>({})

  return (
    <Card>
      <h2>{t('workout.addExercise')}</h2>
      <TextField label={t('exercises.search')} value={q} onChange={(e) => setQ(e.target.value)} autoFocus />
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {available.data?.slice(0, 30).map(({ exercise, equipment }) => {
          const choice = equipmentChoice[exercise.id] ?? (exercise.bodyweight ? '' : (equipment[0]?.id ?? ''))
          return (
            <li key={exercise.id} className={styles.exercise}>
              <strong>{exercise.name}</strong>
              {(equipment.length > 1 || (exercise.bodyweight && equipment.length > 0)) && (
                <SelectField
                  label={t('plans.equipment')}
                  value={choice}
                  onChange={(e) => setEquipmentChoice((prev) => ({ ...prev, [exercise.id]: e.target.value }))}
                >
                  {exercise.bodyweight && <option value="">{t('plans.bodyweight')}</option>}
                  {equipment.map((eq) => (
                    <option key={eq.id} value={eq.id}>
                      {eq.name}
                    </option>
                  ))}
                </SelectField>
              )}
              {equipment.length === 1 && !exercise.bodyweight && <div className={styles.meta}>{equipment[0].name}</div>}
              <div className={styles.row}>
                <Button
                  small
                  variant="primary"
                  disabled={pending}
                  onClick={() => onAdd({ exerciseId: exercise.id, equipmentId: choice || undefined })}
                >
                  {t('workout.add')}
                </Button>
              </div>
            </li>
          )
        })}
      </ul>
      <Button onClick={onClose}>{t('common.close')}</Button>
    </Card>
  )
}
