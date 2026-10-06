import { useTranslation } from 'react-i18next'
import { MUSCLE_GROUPS, type MuscleGroup } from '../../api/exercises'
import { FilterChips } from '../../components/Chips'
import { SearchField } from '../../components/SearchField'

interface Props {
  q: string
  muscle: MuscleGroup | ''
  onQChange: (q: string) => void
  onMuscleChange: (m: MuscleGroup | '') => void
}

export function ExerciseFilters({ q, muscle, onQChange, onMuscleChange }: Props) {
  const { t } = useTranslation()
  return (
    <div role="search">
      <SearchField label={t('exercises.search')} value={q} onChange={onQChange} />
      <FilterChips
        label={t('exercises.muscle')}
        value={muscle}
        allLabel={t('exercises.allMuscles')}
        options={MUSCLE_GROUPS.map((m) => ({ value: m, label: t(`muscles.${m}`) }))}
        onChange={onMuscleChange}
      />
    </div>
  )
}
