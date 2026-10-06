import { useTranslation } from 'react-i18next'
import { Navigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useSession } from '../../api/workouts'
import { Alert } from '../../components/Alert'
import { Card } from '../../components/Card'
import { PageHeader } from '../../components/PageHeader'
import { SkeletonList } from '../../components/Skeleton'
import { formatDateTime } from '../../i18n/format'
import { formatResults } from '../workout/format'
import styles from './History.module.css'

export function SessionDetailPage() {
  const { sessionId = '' } = useParams()
  const { t } = useTranslation()
  const session = useSession(sessionId)
  if (session.isPending) return <SkeletonList count={4} />
  if (session.isError) return <Alert kind="error">{errorMessage(t, session.error)}</Alert>
  const s = session.data
  if (s.status === 'IN_PROGRESS') return <Navigate to={`/workout/${s.id}`} replace />
  const minutes = s.finishedAt ? Math.round((Date.parse(s.finishedAt) - Date.parse(s.startedAt)) / 60000) : 0
  const done = s.exercises.flatMap((e) => e.sets.filter((set) => set.completed))
  const volume = done.reduce((sum, set) => sum + (set.reps ?? 0) * (set.weightKg ?? 0), 0)
  return (
    <>
      <PageHeader
        title={s.title}
        subtitle={`${s.finishedAt ? formatDateTime(s.finishedAt) : ''} · ${s.gymName}`}
        back={{ to: '/history', label: t('history.title') }}
      />
      {s.status === 'ABANDONED' && <Alert kind="info">{t('history.abandoned')}</Alert>}
      <div className={styles.summary}>
        <div className={styles.summaryTile}>
          <div className={styles.summaryValue}>{minutes}</div>
          <div className={styles.summaryLabel}>{t('history.minutes')}</div>
        </div>
        <div className={styles.summaryTile}>
          <div className={styles.summaryValue}>{done.length}</div>
          <div className={styles.summaryLabel}>{t('history.setsLabel')}</div>
        </div>
        <div className={styles.summaryTile}>
          <div className={styles.summaryValue}>{Math.round(volume)}</div>
          <div className={styles.summaryLabel}>{t('history.volumeKg')}</div>
        </div>
      </div>
      {s.note && <Card>{s.note}</Card>}
      {s.exercises.map((e) => {
        const sets = e.sets.filter((set) => set.completed)
        return (
          <Card key={e.id}>
            <strong>{e.exercise.name}</strong>
            <div className={styles.meta}>{e.equipment?.name ?? t('plans.bodyweight')}</div>
            <div className={styles.results}>{sets.length > 0 ? formatResults(sets) : t('history.noSets')}</div>
          </Card>
        )
      })}
    </>
  )
}
