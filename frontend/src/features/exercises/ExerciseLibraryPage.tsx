import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../api/errors'
import { useExerciseLibrary, type MuscleGroup } from '../../api/exercises'
import { Alert } from '../../components/Alert'
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
      <h1>{t('exercises.libraryTitle')}</h1>
      <ExerciseFilters q={q} muscle={muscle} onQChange={setQ} onMuscleChange={setMuscle} />
      {library.isError && <Alert kind="error">{errorMessage(t, library.error)}</Alert>}
      {library.isPending && <p>{t('app.loading')}</p>}
      {library.data?.length === 0 && <p className={styles.meta}>{t('exercises.noResults')}</p>}
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
