import { useTranslation } from 'react-i18next'
import { Link, Navigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useSession } from '../../api/workouts'
import { Alert } from '../../components/Alert'
import { Card } from '../../components/Card'
import { formatDateTime } from '../../i18n/format'
import { formatResults } from '../workout/format'
import styles from '../workout/Workout.module.css'

export function SessionDetailPage() {
  const { sessionId = '' } = useParams()
  const { t } = useTranslation()
  const session = useSession(sessionId)
  if (session.isPending) return <p>{t('app.loading')}</p>
  if (session.isError) return <Alert kind="error">{errorMessage(t, session.error)}</Alert>
  const s = session.data
  if (s.status === 'IN_PROGRESS') return <Navigate to={`/workout/${s.id}`} replace />
  const minutes = s.finishedAt ? Math.round((Date.parse(s.finishedAt) - Date.parse(s.startedAt)) / 60000) : null
  return (
    <>
      <p className={styles.meta}>
        <Link to="/history">← {t('history.title')}</Link>
      </p>
      <h1>{s.title}</h1>
      <p className={styles.meta}>
        {s.finishedAt && formatDateTime(s.finishedAt)} · {s.gymName}
        {minutes !== null && ` · ${t('history.duration', { minutes })}`}
      </p>
      {s.status === 'ABANDONED' && <Alert kind="info">{t('history.abandoned')}</Alert>}
      {s.note && <Card>{s.note}</Card>}
      {s.exercises.map((e) => {
        const done = e.sets.filter((set) => set.completed)
        return (
          <Card key={e.id}>
            <strong>{e.exercise.name}</strong>
            <div className={styles.meta}>{e.equipment?.name ?? t('plans.bodyweight')}</div>
            <div>{done.length > 0 ? formatResults(done) : t('history.noSets')}</div>
          </Card>
        )
      })}
    </>
  )
}
