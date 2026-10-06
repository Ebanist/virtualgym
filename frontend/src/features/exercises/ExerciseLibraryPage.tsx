import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../api/errors'
import { useExerciseLibrary, type MuscleGroup } from '../../api/exercises'
import { Alert } from '../../components/Alert'
import { EmptyState } from '../../components/EmptyState'
import { PageHeader } from '../../components/PageHeader'
import { SkeletonList } from '../../components/Skeleton'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { ExerciseCard } from './ExerciseCard'
import { ExerciseFilters } from './ExerciseFilters'
import styles from './Exercises.module.css'

export function ExerciseLibraryPage() {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<MuscleGroup | ''>('')
  const debouncedQ = useDebouncedValue(q)
  const library = useExerciseLibrary(debouncedQ, muscle || undefined)
  return (
    <>
      <PageHeader title={t('exercises.libraryTitle')} subtitle={library.data ? t('exercises.count', { count: library.data.length }) : undefined} />
      <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} />
      {library.isError && <Alert kind="error">{errorMessage(t, library.error)}</Alert>}
      {library.isPending && <SkeletonList />}
      {library.data?.length === 0 && <EmptyState icon="search">{t('exercises.noResults')}</EmptyState>}
      <ul className={styles.list}>
        {library.data?.map((exercise) => (
          <li key={exercise.id}>
            <ExerciseCard exercise={exercise} />
          </li>
        ))}
      </ul>
    </>
  )
}
