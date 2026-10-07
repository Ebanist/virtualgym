import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../api/errors'
import { useDeleteExercise, useExercise, type Exercise } from '../../api/exercises'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ExerciseFormSheet } from './ExerciseFormSheet'
import styles from './Exercises.module.css'

/** Edycja i usuwanie własnego ćwiczenia (tylko autor – {@code exercise.mine}). */
export function MyExerciseActions({ exercise }: { exercise: Exercise }) {
  const { t } = useTranslation()
  const [editing, setEditing] = useState(false)
  const details = useExercise(editing ? exercise.id : undefined)
  const remove = useDeleteExercise()
  if (!exercise.mine || !exercise.gymId) return null
  return (
    <>
      <div className={styles.cardActions}>
        <Button
          small
          iconOnly
          variant="ghost"
          icon="edit"
          aria-label={t('exercises.editNamed', { name: exercise.name })}
          onClick={() => setEditing(true)}
        />
        <Button
          small
          iconOnly
          variant="ghost"
          icon="trash"
          aria-label={t('exercises.deleteNamed', { name: exercise.name })}
          disabled={remove.isPending}
          onClick={() => {
            if (window.confirm(t('exercises.deleteConfirm'))) remove.mutate(exercise.id)
          }}
        />
      </div>
      {remove.error && <Alert kind="error">{errorMessage(t, remove.error)}</Alert>}
      <ExerciseFormSheet
        open={editing && !!details.data}
        onClose={() => setEditing(false)}
        gymId={exercise.gymId}
        exercise={details.data}
        onSaved={() => setEditing(false)}
      />
    </>
  )
}
