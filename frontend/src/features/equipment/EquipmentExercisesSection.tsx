import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Equipment } from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { useEquipmentExercises, useExerciseLibrary, useExerciseLinks } from '../../api/exercises'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { SearchField } from '../../components/SearchField'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { ExerciseFormSheet } from '../exercises/ExerciseFormSheet'
import { OriginBadge } from '../exercises/OriginBadge'
import styles from './Equipment.module.css'

/** Ćwiczenia wykonywane na sprzęcie + ręczne powiązania i nowe ćwiczenia (członkowie). */
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
          <Button small icon="link" onClick={() => setAdding(true)}>
            {t('exercises.link')}
          </Button>
        )}
      </div>
      {error && <Alert kind="error">{errorMessage(t, error)}</Alert>}
      {adding && (
        <LinkPicker
          equipment={equipment}
          linkedIds={list.data?.map((x) => x.exercise.id) ?? []}
          onPick={(id) => link.mutate(id)}
          onClose={() => setAdding(false)}
        />
      )}
      {list.data?.length === 0 && <p className={styles.meta}>{t('exercises.noneForEquipment')}</p>}
      <ul className={styles.linkedList}>
        {list.data?.map(({ exercise, linked, byType }) => (
          <li key={exercise.id}>
            <span className={styles.linkedName}>
              <span>{exercise.name}</span>
              <span className={styles.meta}>
                {t(`muscles.${exercise.primaryMuscle}`)}
                {!byType && ` · ${t('exercises.linkedManually')}`}
              </span>
              <span>
                <OriginBadge exercise={exercise} />
              </span>
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

function LinkPicker({
  equipment,
  linkedIds,
  onPick,
  onClose,
}: {
  equipment: Equipment
  linkedIds: string[]
  onPick: (id: string) => void
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const [creating, setCreating] = useState(false)
  const debounced = useDebouncedValue(q)
  const searching = debounced.trim().length >= 2
  // Tylko ćwiczenia widoczne w tej siłowni (biblioteka + publiczne + moje prywatne).
  const results = useExerciseLibrary(debounced, undefined, searching, equipment.gymId)
  const candidates = results.data?.filter((x) => !linkedIds.includes(x.id)).slice(0, 10) ?? []

  return (
    <div className={styles.picker}>
      <SearchField label={t('exercises.searchToLink')} value={q} onChange={setQ} />
      {searching && results.data && candidates.length === 0 && (
        <p className={styles.meta}>{t('exercises.noResults')}</p>
      )}
      <ul className={styles.linkedList}>
        {candidates.map((exercise) => (
          <li key={exercise.id}>
            <span className={styles.linkedName}>
              <span>{exercise.name}</span>
              <span>
                <OriginBadge exercise={exercise} />
              </span>
            </span>
            <Button small onClick={() => onPick(exercise.id)}>
              {t('exercises.linkThis')}
            </Button>
          </li>
        ))}
      </ul>
      <div className={styles.pickerActions}>
        <Button small variant="primary" icon="plus" onClick={() => setCreating(true)}>
          {q.trim() ? t('exercises.addNamed', { name: q.trim() }) : t('exercises.newExercise')}
        </Button>
        <Button small variant="ghost" onClick={onClose}>
          {t('common.close')}
        </Button>
      </div>
      <ExerciseFormSheet
        open={creating}
        onClose={() => setCreating(false)}
        gymId={equipment.gymId}
        initialName={q.trim()}
        initialEquipmentIds={[equipment.id]}
        onSaved={() => {
          // Utworzone ćwiczenie jest już powiązane z tym sprzętem (equipmentIds).
          setCreating(false)
          onClose()
        }}
        onPickExisting={(exercise) => {
          onPick(exercise.id)
          setCreating(false)
          onClose()
        }}
      />
    </div>
  )
}
