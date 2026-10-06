import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  REPORT_TYPES,
  useCreateReport,
  useEquipmentList,
  useEquipmentReports,
  useResolveReport,
  type Equipment,
  type ReportType,
} from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { SelectField } from '../../components/SelectField'
import { TextArea } from '../../components/TextArea'
import { formatDate } from '../../i18n/format'
import styles from './Equipment.module.css'

export function ReportsSection({ equipment }: { equipment: Equipment }) {
  const { t } = useTranslation()
  const reports = useEquipmentReports(equipment.id)
  const resolve = useResolveReport()
  const [formOpen, setFormOpen] = useState(false)

  return (
    <Card>
      <div className={styles.sectionHeader}>
        <h2>{t('equipment.reports.title')}</h2>
        {equipment.member && !formOpen && (
          <Button small onClick={() => setFormOpen(true)}>
            {t('equipment.reports.add')}
          </Button>
        )}
      </div>
      {formOpen && <ReportForm equipment={equipment} onDone={() => setFormOpen(false)} />}
      {resolve.error && <Alert kind="error">{errorMessage(t, resolve.error)}</Alert>}
      {reports.data?.length === 0 && <p className={styles.meta}>{t('equipment.reports.empty')}</p>}
      {reports.data?.map((r) => (
        <div key={r.id} className={[styles.report, r.status === 'RESOLVED' && styles.resolved].filter(Boolean).join(' ')}>
          <div>
            <strong>{t(`equipment.reports.type.${r.type}`)}</strong>
            {r.status === 'RESOLVED' && ` · ${t('equipment.reports.resolved')}`}
          </div>
          {r.duplicateOf && (
            <div className={styles.meta}>
              {t('equipment.reports.duplicateOf')}: {r.duplicateOf.name}
            </div>
          )}
          {r.comment && <div>{r.comment}</div>}
          <div className={styles.meta}>
            {r.reporter.displayName} · {formatDate(r.createdAt)}
          </div>
          {equipment.member && r.status === 'OPEN' && (
            <Button small disabled={resolve.isPending} onClick={() => resolve.mutate(r.id)}>
              {t('equipment.reports.resolve')}
            </Button>
          )}
        </div>
      ))}
    </Card>
  )
}

function ReportForm({ equipment, onDone }: { equipment: Equipment; onDone: () => void }) {
  const { t } = useTranslation()
  const create = useCreateReport(equipment.id)
  const [type, setType] = useState<ReportType>('WRONG_DATA')
  const [comment, setComment] = useState('')
  const [duplicateOfId, setDuplicateOfId] = useState('')
  const others = useEquipmentList(equipment.gymId, { size: 100 })

  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault()
        await create.mutateAsync({
          type,
          comment: comment.trim() || undefined,
          duplicateOfId: type === 'DUPLICATE' && duplicateOfId ? duplicateOfId : undefined,
        })
        onDone()
      }}
    >
      {create.error && <Alert kind="error">{errorMessage(t, create.error)}</Alert>}
      <SelectField label={t('equipment.reports.typeLabel')} value={type} onChange={(e) => setType(e.target.value as ReportType)}>
        {REPORT_TYPES.map((rt) => (
          <option key={rt} value={rt}>
            {t(`equipment.reports.type.${rt}`)}
          </option>
        ))}
      </SelectField>
      {type === 'DUPLICATE' && (
        <SelectField
          label={t('equipment.reports.duplicateOf')}
          value={duplicateOfId}
          onChange={(e) => setDuplicateOfId(e.target.value)}
        >
          <option value="">{t('equipment.reports.chooseDuplicate')}</option>
          {others.data?.content
            .filter((o) => o.id !== equipment.id)
            .map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
        </SelectField>
      )}
      {type === 'REMOVED_FROM_GYM' && <Alert kind="info">{t('equipment.reports.removedHint')}</Alert>}
      <TextArea
        label={t('equipment.reports.comment')}
        value={comment}
        maxLength={1000}
        onChange={(e) => setComment(e.target.value)}
      />
      <div className={styles.actions}>
        <Button type="submit" variant="primary" disabled={create.isPending}>
          {t('equipment.reports.submit')}
        </Button>
        <Button onClick={onDone}>{t('common.cancel')}</Button>
      </div>
    </form>
  )
}
