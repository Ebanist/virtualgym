import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Equipment } from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { useEquipmentExercises, useExerciseLibrary, useExerciseLinks } from '../../api/exercises'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { TextField } from '../../components/TextField'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import exerciseStyles from '../exercises/Exercises.module.css'
import styles from './Equipment.module.css'

/** Ćwiczenia wykonywane na sprzęcie + ręczne powiązania (członkowie). */
export function EquipmentExercisesSection({ equipment }: { equipment: Equipment }) {
  const { t } = useTranslation()
  const list = useEquipmentExercises(equipment.id)
  const { link, unlink } = useExerciseLinks(equipment.id)
  const [adding, setAdding] = useState(false)
  const error = link.error ?? unlink.error

  return (
    <Card>
      <div className={styles.sectionHeader}>
        <h2>{t('exercises.onThisEquipment')}</h2>
        {equipment.member && !adding && (
          <Button small onClick={() => setAdding(true)}>
            {t('exercises.link')}
          </Button>
        )}
      </div>
      {error && <Alert kind="error">{errorMessage(t, error)}</Alert>}
      {adding && <LinkPicker onPick={(id) => link.mutate(id)} onClose={() => setAdding(false)} linkedIds={list.data?.map((x) => x.exercise.id) ?? []} />}
      {list.data?.length === 0 && <p className={styles.meta}>{t('exercises.noneForEquipment')}</p>}
      <ul className={exerciseStyles.list}>
        {list.data?.map(({ exercise, linked, byType }) => (
          <li key={exercise.id} className={exerciseStyles.cardHeader}>
            <span>
              {exercise.name}
              <span className={styles.meta}> · {t(`muscles.${exercise.primaryMuscle}`)}</span>
              {!byType && <span className={styles.meta}> · {t('exercises.linkedManually')}</span>}
            </span>
            {equipment.member && linked && (
              <Button small variant="ghost" disabled={unlink.isPending} onClick={() => unlink.mutate(exercise.id)}>
                {t('exercises.unlink')}
              </Button>
            )}
          </li>
        ))}
      </ul>
    </Card>
  )
}

function LinkPicker({ onPick, onClose, linkedIds }: { onPick: (id: string) => void; onClose: () => void; linkedIds: string[] }) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const debounced = useDebouncedValue(q)
  const results = useExerciseLibrary(debounced, undefined, debounced.trim().length >= 2)
  return (
    <div>
      <TextField label={t('exercises.searchToLink')} value={q} onChange={(e) => setQ(e.target.value)} autoFocus />
      <ul className={exerciseStyles.list}>
        {results.data
          ?.filter((x) => !linkedIds.includes(x.id))
          .slice(0, 10)
          .map((exercise) => (
            <li key={exercise.id} className={exerciseStyles.cardHeader}>
              <span>{exercise.name}</span>
              <Button small onClick={() => onPick(exercise.id)}>
                {t('exercises.linkThis')}
              </Button>
            </li>
          ))}
      </ul>
      <Button small onClick={onClose}>
        {t('common.close')}
      </Button>
    </div>
  )
}
