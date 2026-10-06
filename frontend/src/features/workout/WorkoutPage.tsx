import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import {
  sessionActions,
  useAbandonSession,
  useSession,
  useSessionMutation,
  type Session,
  type SessionExercise,
} from '../../api/workouts'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { formatDate } from '../../i18n/format'
import { AddExercisePanel } from './AddExercisePanel'
import { formatResults, formatTarget } from './format'
import { RestTimerBar } from './RestTimerBar'
import { SetRow } from './SetRow'
import { useRestTimer } from './useRestTimer'
import styles from './Workout.module.css'

export function WorkoutPage() {
  const { sessionId = '' } = useParams()
  const { t } = useTranslation()
  const session = useSession(sessionId)
  if (session.isPending) return <p>{t('app.loading')}</p>
  if (session.isError) return <Alert kind="error">{errorMessage(t, session.error)}</Alert>
  if (session.data.status !== 'IN_PROGRESS') return <Navigate to={`/history/${sessionId}`} replace />
  return <ActiveWorkout session={session.data} />
}

function ActiveWorkout({ session }: { session: Session }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const timer = useRestTimer()
  const [index, setIndex] = useState(0)
  const [adding, setAdding] = useState(session.exercises.length === 0)
  const updateSet = useSessionMutation((v: { setId: string; body: Parameters<typeof sessionActions.updateSet>[2] }) =>
    sessionActions.updateSet(session.id, v.setId, v.body),
  )
  const addSet = useSessionMutation((exerciseId: string) => sessionActions.addSet(session.id, exerciseId))
  const removeSet = useSessionMutation((setId: string) => sessionActions.removeSet(session.id, setId))
  const addExercise = useSessionMutation((body: Parameters<typeof sessionActions.addExercise>[1]) =>
    sessionActions.addExercise(session.id, body),
  )
  const removeExercise = useSessionMutation((id: string) => sessionActions.removeExercise(session.id, id))
  const finish = useSessionMutation(() => sessionActions.finish(session.id))
  const abandon = useAbandonSession()
  const error = updateSet.error ?? addSet.error ?? removeSet.error ?? addExercise.error ?? removeExercise.error ?? finish.error ?? abandon.error

  const exercises = session.exercises
  const current = exercises[Math.min(index, exercises.length - 1)]
  const currentIndex = current ? exercises.indexOf(current) : -1
  const doneCount = (e: SessionExercise) => e.sets.filter((s) => s.completed).length

  return (
    <>
      <div className={styles.header}>
        <h1>{session.title}</h1>
        <div className={styles.meta}>
          {session.gymName} · {t('workout.startedAt', { date: formatDate(session.startedAt) })}
        </div>
      </div>
      {error && <Alert kind="error">{errorMessage(t, error)}</Alert>}

      {exercises.length > 0 && (
        <nav className={styles.chips} aria-label={t('workout.exercises')}>
          {exercises.map((e, i) => {
            const done = e.sets.length > 0 && doneCount(e) === e.sets.length
            return (
              <button
                key={e.id}
                type="button"
                className={i === currentIndex ? styles.chipActive : done ? styles.chipDone : styles.chip}
                aria-current={i === currentIndex ? 'step' : undefined}
                onClick={() => setIndex(i)}
              >
                {i + 1}. {e.exercise.name}
              </button>
            )
          })}
        </nav>
      )}

      {current && (
        <ExerciseView
          key={current.id}
          exercise={current}
          position={currentIndex + 1}
          total={exercises.length}
          busy={updateSet.isPending}
          onSaveSet={(setId, body) => updateSet.mutate({ setId, body })}
          onSetCompleted={() => timer.start(current.restSeconds)}
          onAddSet={() => addSet.mutate(current.id)}
          onRemoveLastSet={() => {
            const last = current.sets[current.sets.length - 1]
            if (last) removeSet.mutate(last.id)
          }}
          onRemove={() => {
            if (window.confirm(t('workout.removeExerciseConfirm'))) {
              removeExercise.mutate(current.id)
              setIndex(Math.max(0, currentIndex - 1))
            }
          }}
        />
      )}

      {current && (
        <div className={styles.navRow}>
          <Button disabled={currentIndex <= 0} onClick={() => setIndex(currentIndex - 1)}>
            ← {t('workout.prev')}
          </Button>
          <Button disabled={currentIndex >= exercises.length - 1} onClick={() => setIndex(currentIndex + 1)}>
            {t('workout.next')} →
          </Button>
        </div>
      )}

      {adding ? (
        <AddExercisePanel
          gymId={session.gymId}
          pending={addExercise.isPending}
          onClose={() => setAdding(false)}
          onAdd={async (body) => {
            const updated = await addExercise.mutateAsync(body)
            setIndex(updated.exercises.length - 1)
            setAdding(false)
          }}
        />
      ) : (
        <div className={styles.row}>
          <Button onClick={() => setAdding(true)}>{t('workout.addExercise')}</Button>
        </div>
      )}

      <div className={styles.row}>
        <Button
          variant="primary"
          block
          disabled={finish.isPending}
          onClick={async () => {
            if (!window.confirm(t('workout.finishConfirm'))) return
            timer.stop()
            await finish.mutateAsync(undefined)
            navigate(`/history/${session.id}`, { replace: true })
          }}
        >
          {t('workout.finish')}
        </Button>
        <Button
          variant="danger"
          block
          disabled={abandon.isPending}
          onClick={async () => {
            if (!window.confirm(t('workout.abandonConfirm'))) return
            timer.stop()
            await abandon.mutateAsync(session.id)
            navigate('/', { replace: true })
          }}
        >
          {t('workout.abandon')}
        </Button>
      </div>
      <div className={styles.spacer} />
      <RestTimerBar
        running={timer.running}
        finished={timer.finished}
        remaining={timer.remaining}
        onAdd={() => timer.addSeconds(15)}
        onStop={timer.stop}
        onDismiss={timer.dismiss}
      />
    </>
  )
}

interface ExerciseViewProps {
  exercise: SessionExercise
  position: number
  total: number
  busy: boolean
  onSaveSet: (setId: string, body: Parameters<typeof sessionActions.updateSet>[2]) => void
  onSetCompleted: () => void
  onAddSet: () => void
  onRemoveLastSet: () => void
  onRemove: () => void
}

function ExerciseView({ exercise: e, position, total, onSaveSet, onSetCompleted, onAddSet, onRemoveLastSet, onRemove }: ExerciseViewProps) {
  const { t } = useTranslation()
  const target = formatTarget(t, e)
  const photo = e.equipment?.photoUrl
  return (
    <section className={styles.exercise} aria-label={e.exercise.name}>
      <div className={styles.meta}>{t('workout.exerciseOf', { n: position, total })}</div>
      <h2>{e.exercise.name}</h2>
      <div className={styles.meta}>{e.equipment ? e.equipment.name : t('plans.bodyweight')}</div>
      {e.equipment && (e.equipment.deleted || e.equipment.status === 'REMOVED_FROM_GYM') && (
        <Alert kind="warning">{t('plans.equipmentRemoved')}</Alert>
      )}
      {photo && <img src={photo} alt={e.equipment?.name ?? ''} className={styles.photo} />}
      {target && <div className={styles.target}>{target}</div>}
      {e.note && <div className={styles.meta}>{e.note}</div>}
      <div className={styles.previous}>
        {e.previous && e.previous.sets.length > 0 ? (
          <>
            <strong>{t('workout.previous', { date: formatDate(e.previous.date) })}</strong> {formatResults(e.previous.sets)}
          </>
        ) : (
          t('workout.noPrevious')
        )}
      </div>
      <table className={styles.sets}>
        <thead>
          <tr>
            <th>{t('workout.set')}</th>
            <th>{t('workout.weightKg')}</th>
            <th>{t('workout.reps')}</th>
            <th>
              <span className="visually-hidden">{t('workout.done')}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {e.sets.map((set) => (
            <SetRow
              key={set.id}
              set={set}
              repsHint={e.targetRepsMax}
              onSave={(body) => onSaveSet(set.id, body)}
              onCompleted={onSetCompleted}
            />
          ))}
        </tbody>
      </table>
      <div className={styles.row}>
        <Button small onClick={onAddSet}>
          + {t('workout.addSet')}
        </Button>
        {e.sets.length > 0 && (
          <Button small variant="ghost" onClick={onRemoveLastSet}>
            − {t('workout.removeSet')}
          </Button>
        )}
        <Button small variant="ghost" onClick={onRemove}>
          {t('workout.removeExercise')}
        </Button>
      </div>
    </section>
  )
}
