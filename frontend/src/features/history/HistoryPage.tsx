import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useHistory } from '../../api/workouts'
import { Alert } from '../../components/Alert'
import listStyles from '../../components/List.module.css'
import { Pagination } from '../../components/Pagination'
import { formatDateTime } from '../../i18n/format'

export function HistoryPage() {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)
  const history = useHistory(page)
  return (
    <>
      <h1>{t('history.title')}</h1>
      {history.isError && <Alert kind="error">{errorMessage(t, history.error)}</Alert>}
      {history.isPending && <p>{t('app.loading')}</p>}
      {history.data?.content.length === 0 && <p className={listStyles.empty}>{t('history.empty')}</p>}
      <ul className={listStyles.list}>
        {history.data?.content.map((s) => (
          <li key={s.id}>
            <Link to={`/history/${s.id}`} className={listStyles.item}>
              <span className={listStyles.title}>{s.title}</span>
              <div className={listStyles.meta}>
                {formatDateTime(s.finishedAt)} · {s.gymName}
              </div>
              <div className={listStyles.meta}>
                {t('history.summary', { exercises: s.exerciseCount, sets: s.completedSets, volume: Math.round(s.volumeKg) })}
              </div>
            </Link>
          </li>
        ))}
      </ul>
      {history.data && <Pagination page={history.data.page} totalPages={history.data.totalPages} onChange={setPage} />}
    </>
  )
}
