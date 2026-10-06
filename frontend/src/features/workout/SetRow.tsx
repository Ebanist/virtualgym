import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { SessionSet, UpdateSetRequest } from '../../api/workouts'
import { parseNumber } from './format'
import styles from './Workout.module.css'

interface Props {
  set: SessionSet
  /** Podpowiedź powtórzeń (górna granica celu) – wpisywana przy odhaczeniu pustej serii. */
  repsHint?: number
  disabled?: boolean
  onSave: (request: UpdateSetRequest) => void
  /** Wywoływane po odhaczeniu serii (start przerwy). */
  onCompleted?: () => void
}

export function SetRow({ set, repsHint, disabled, onSave, onCompleted }: Props) {
  const { t } = useTranslation()
  const [weight, setWeight] = useState(set.weightKg?.toString() ?? '')
  const [reps, setReps] = useState(set.reps?.toString() ?? '')

  const save = (completed: boolean, repsValue = reps) =>
    onSave({ reps: parseNumber(repsValue), weightKg: parseNumber(weight), completed })

  const saveIfChanged = () => {
    if (parseNumber(weight) !== (set.weightKg ?? undefined) || parseNumber(reps) !== (set.reps ?? undefined)) {
      save(set.completed)
    }
  }

  const toggle = () => {
    if (set.completed) {
      save(false)
      return
    }
    const repsValue = reps.trim() === '' && repsHint ? String(repsHint) : reps
    setReps(repsValue)
    save(true, repsValue)
    onCompleted?.()
  }

  return (
    <tr className={set.completed ? styles.setDone : undefined}>
      <td className={styles.setNumber}>{set.setNumber}</td>
      <td>
        <input
          className={styles.numInput}
          inputMode="decimal"
          aria-label={t('workout.weightOfSet', { n: set.setNumber })}
          placeholder="kg"
          value={weight}
          disabled={disabled}
          onChange={(e) => setWeight(e.target.value)}
          onBlur={saveIfChanged}
        />
      </td>
      <td>
        <input
          className={styles.numInput}
          inputMode="numeric"
          aria-label={t('workout.repsOfSet', { n: set.setNumber })}
          placeholder={repsHint ? String(repsHint) : '–'}
          value={reps}
          disabled={disabled}
          onChange={(e) => setReps(e.target.value)}
          onBlur={saveIfChanged}
        />
      </td>
      <td>
        <button
          type="button"
          className={set.completed ? styles.checkDone : styles.check}
          aria-pressed={set.completed}
          aria-label={set.completed ? t('workout.uncheckSet', { n: set.setNumber }) : t('workout.checkSet', { n: set.setNumber })}
          disabled={disabled}
          onClick={toggle}
        >
          ✓
        </button>
      </td>
    </tr>
  )
}
