import { useTranslation } from 'react-i18next'
import { MUSCLE_GROUPS, type MuscleGroup } from '../../api/exercises'
import { SelectField } from '../../components/SelectField'
import { TextField } from '../../components/TextField'
import styles from './Exercises.module.css'

interface Props {
  q: string
  muscle: MuscleGroup | ''
  onQChange: (q: string) => void
  onMuscleChange: (m: MuscleGroup | '') => void
}

export function ExerciseFilters({ q, muscle, onQChange, onMuscleChange }: Props) {
  const { t } = useTranslation()
  return (
    <div className={styles.filters} role="search">
      <TextField label={t('exercises.search')} value={q} onChange={(e) => onQChange(e.target.value)} />
      <SelectField label={t('exercises.muscle')} value={muscle} onChange={(e) => onMuscleChange(e.target.value as MuscleGroup | '')}>
        <option value="">{t('exercises.allMuscles')}</option>
        {MUSCLE_GROUPS.map((m) => (
          <option key={m} value={m}>
            {t(`muscles.${m}`)}
          </option>
        ))}
      </SelectField>
    </div>
  )
}
