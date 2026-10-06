import { useTranslation } from 'react-i18next'
import { useEquipmentHistory, type EquipmentChange } from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { formatDateTime } from '../../i18n/format'
import styles from './Equipment.module.css'

export function HistorySection({ equipmentId }: { equipmentId: string }) {
  const { t } = useTranslation()
  const history = useEquipmentHistory(equipmentId)
  const entries = history.data?.pages.flatMap((p) => p.content) ?? []
  return (
    <Card>
      <h2>{t('equipment.history.title')}</h2>
      {history.isError && <Alert kind="error">{errorMessage(t, history.error)}</Alert>}
      <ul className={styles.history}>
        {entries.map((entry) => (
          <li key={entry.id}>
            <div>
              <strong>{entry.user.displayName}</strong> · {t(`equipment.history.type.${entry.changeType}`)}
            </div>
            <div className={styles.change}>{formatDateTime(entry.changedAt)}</div>
            <ChangeDetails change={entry} />
          </li>
        ))}
      </ul>
      {history.hasNextPage && (
        <Button small disabled={history.isFetchingNextPage} onClick={() => void history.fetchNextPage()}>
          {t('common.loadMore')}
        </Button>
      )}
    </Card>
  )
}

function ChangeDetails({ change }: { change: EquipmentChange }) {
  const { t } = useTranslation()
  if (change.changeType === 'CREATED' || change.changeType === 'PHOTO_CHANGED' || change.changeType === 'DELETED') {
    return null
  }
  const display = (field: string, value?: string) => {
    if (value === undefined || value === null || value === '') return '—'
    if (field === 'category') return t(`equipment.category.${value}`)
    if (field === 'status') return t(`equipment.status.${value}`)
    return value
  }
  return (
    <ul className={styles.change}>
      {Object.entries(change.changes).map(([field, diff]) => (
        <li key={field}>
          {t(`equipment.fields.${field}`, { defaultValue: field })}: {display(field, diff.oldValue)} →{' '}
          {display(field, diff.newValue)}
        </li>
      ))}
    </ul>
  )
}
