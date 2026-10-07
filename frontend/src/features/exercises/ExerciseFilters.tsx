import { useTranslation } from 'react-i18next'
import { MUSCLE_GROUPS, type MuscleGroup } from '../../api/exercises'
import { FilterChips } from '../../components/Chips'
import { SearchField } from '../../components/SearchField'
import { EXERCISE_ORIGINS, type ExerciseOrigin } from './origin'

interface Props {
  q: string
  muscle: MuscleGroup | ''
  onQChange: (q: string) => void
  onMuscleChange: (m: MuscleGroup | '') => void
  /** Filtr pochodzenia (Biblioteka / Moje / Społeczność) – pokazywany, gdy podano. */
  origin?: ExerciseOrigin | ''
  onOriginChange?: (o: ExerciseOrigin | '') => void
}

export function ExerciseFilters({ q, muscle, onQChange, onMuscleChange, origin, onOriginChange }: Props) {
  const { t } = useTranslation()
  return (
    <div role="search">
      <SearchField label={t('exercises.search')} value={q} onChange={onQChange} />
      {onOriginChange && (
        <FilterChips
          label={t('exercises.originLabel')}
          value={origin ?? ''}
          allLabel={t('exercises.allOrigins')}
          options={EXERCISE_ORIGINS.map((o) => ({ value: o, label: t(`exercises.origin.${o}`) }))}
          onChange={onOriginChange}
        />
      )}
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
