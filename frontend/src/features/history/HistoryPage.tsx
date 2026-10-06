import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useHistory } from '../../api/workouts'
import { Alert } from '../../components/Alert'
import { EmptyState } from '../../components/EmptyState'
import { Icon } from '../../components/Icon'
import { PageHeader } from '../../components/PageHeader'
import { Pagination } from '../../components/Pagination'
import { SkeletonList } from '../../components/Skeleton'
import i18n from '../../i18n'
import styles from './History.module.css'

const dayFormat = (iso: string) => new Intl.DateTimeFormat(i18n.language, { day: 'numeric' }).format(new Date(iso))
const monthFormat = (iso: string) => new Intl.DateTimeFormat(i18n.language, { month: 'short' }).format(new Date(iso))
const timeFormat = (iso: string) => new Intl.DateTimeFormat(i18n.language, { weekday: 'long', hour: '2-digit', minute: '2-digit' }).format(new Date(iso))

export function HistoryPage() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const history = useHistory(page)
  return (
    <>
      <PageHeader
        title={t('history.title')}
        subtitle={history.data ? t('history.total', { count: history.data.totalElements }) : undefined}
      />
      {history.isError && <Alert kind="error">{errorMessage(t, history.error)}</Alert>}
      {history.isPending && <SkeletonList />}
      {history.data?.content.length === 0 && <EmptyState icon="history">{t('history.empty')}</EmptyState>}
      <ul className={styles.list}>
        {history.data?.content.map((s) => {
          const minutes = Math.round((Date.parse(s.finishedAt) - Date.parse(s.startedAt)) / 60000)
          return (
            <li key={s.id}>
              <Link to={`/history/${s.id}`} className={styles.item}>
                <span className={styles.date} aria-hidden>
                  <span className={styles.day}>{dayFormat(s.finishedAt)}</span>
                  <span className={styles.month}>{monthFormat(s.finishedAt)}</span>
                </span>
                <span className={styles.body}>
                  <span className={styles.title}>{s.title}</span>
                  <span className={styles.meta}>
                    {timeFormat(s.finishedAt)} · {s.gymName}
                  </span>
                  <span className={styles.stats}>
                    <span className={styles.stat}>
                      <strong>{minutes}</strong> min
                    </span>
                    <span className={styles.stat}>
                      {t('history.setsLabel')} <strong>{s.completedSets}</strong>
                    </span>
                    <span className={styles.stat}>
                      <strong>{Math.round(s.volumeKg)}</strong> kg
                    </span>
                  </span>
                </span>
                <Icon name="chevronRight" size={18} />
              </Link>
            </li>
          )
        })}
      </ul>
      {history.data && <Pagination page={history.data.page} totalPages={history.data.totalPages} onChange={setPage} />}
    </>
  )
}
