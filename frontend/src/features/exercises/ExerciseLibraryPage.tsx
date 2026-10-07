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
import { MyExerciseActions } from './MyExerciseActions'
import { matchesOrigin, type ExerciseOrigin } from './origin'

export function ExerciseLibraryPage() {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<MuscleGroup | ''>('')
  const [origin, setOrigin] = useState<ExerciseOrigin | ''>('')
  const debouncedQ = useDebouncedValue(q)
  const library = useExerciseLibrary(debouncedQ, muscle || undefined)
  const visible = library.data?.filter((e) => matchesOrigin(e, origin))
  return (
    <>
      <PageHeader title={t('exercises.libraryTitle')} subtitle={library.data ? t('exercises.count', { count: library.data.length }) : undefined} />
      <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} origin={origin} onOriginChange={setOrigin} />
      {library.isError && <Alert kind="error">{errorMessage(t, library.error)}</Alert>}
      {library.isPending && <SkeletonList />}
      {visible?.length === 0 && <EmptyState icon="search">{t('exercises.noResults')}</EmptyState>}
      <ul className={styles.list}>
        {visible?.map((exercise) => (
          <li key={exercise.id}>
            <ExerciseCard exercise={exercise} action={<MyExerciseActions exercise={exercise} />} />
          </li>
        ))}
      </ul>
    </>
  )
}
