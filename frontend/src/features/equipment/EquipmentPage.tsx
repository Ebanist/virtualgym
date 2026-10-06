import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  useDeleteEquipment,
  useEquipment,
  useUpdateEquipment,
  useUploadEquipmentPhoto,
  type Equipment,
} from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { buttonClass } from '../../components/buttonClass'
import { Card } from '../../components/Card'
import styles from './Equipment.module.css'
import { EquipmentExercisesSection } from './EquipmentExercisesSection'
import { HistorySection } from './HistorySection'
import { PhotoInput } from './PhotoInput'
import { ReportsSection } from './ReportsSection'

export function EquipmentPage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const location = useLocation()
  const equipment = useEquipment(id)

  if (equipment.isPending) return <p>{t('app.loading')}</p>
  if (equipment.isError) return <Alert kind="error">{errorMessage(t, equipment.error)}</Alert>
  const e = equipment.data
  const photoError = (location.state as { photoError?: string } | null)?.photoError

  return (
    <>
      <p className={styles.meta}>
        <Link to={`/gyms/${e.gymId}`}>← {e.gymName}</Link>
      </p>
      <h1>{e.name}</h1>
      {photoError && <Alert kind="error">{photoError}</Alert>}
      {e.status === 'REMOVED_FROM_GYM' && <Alert kind="warning">{t('equipment.removedInfo')}</Alert>}
      {!e.member && <Alert kind="info">{t('equipment.joinToEdit')}</Alert>}
      {e.photoUrl && (
        <a href={e.photoUrl} target="_blank" rel="noreferrer">
          <img src={e.photoUrl} alt={e.name} className={styles.photo} />
        </a>
      )}
      <Card>
        <dl className={styles.details}>
          <dt>{t('equipment.categoryLabel')}</dt>
          <dd>{t(`equipment.category.${e.category}`)}</dd>
          <dt>{t('equipment.typeShort')}</dt>
          <dd>{e.equipmentType?.name ?? '—'}</dd>
          <dt>{t('equipment.quantity')}</dt>
          <dd>{e.quantity ?? '—'}</dd>
          <dt>{t('equipment.statusLabel')}</dt>
          <dd>{t(`equipment.status.${e.status}`)}</dd>
          {e.description && (
            <>
              <dt>{t('equipment.description')}</dt>
              <dd>{e.description}</dd>
            </>
          )}
          <dt>{t('equipment.addedBy')}</dt>
          <dd>{e.createdBy.displayName}</dd>
        </dl>
      </Card>
      {e.member && <MemberActions equipment={e} />}
      <EquipmentExercisesSection equipment={e} />
      <ReportsSection equipment={e} />
      <HistorySection equipmentId={e.id} />
    </>
  )
}

function MemberActions({ equipment: e }: { equipment: Equipment }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const update = useUpdateEquipment(e.id)
  const upload = useUploadEquipmentPhoto()
  const remove = useDeleteEquipment(e.id)
  const [photo, setPhoto] = useState<File | null>(null)
  const [showPhoto, setShowPhoto] = useState(false)
  const error = update.error ?? upload.error ?? remove.error
  const removed = e.status === 'REMOVED_FROM_GYM'

  const toggleStatus = () => {
    if (!removed && !window.confirm(t('equipment.markRemovedConfirm'))) return
    update.mutate({
      name: e.name,
      category: e.category,
      equipmentTypeId: e.equipmentType?.id,
      description: e.description,
      quantity: e.quantity,
      status: removed ? 'ACTIVE' : 'REMOVED_FROM_GYM',
      version: e.version,
    })
  }

  return (
    <>
      {error && <Alert kind="error">{errorMessage(t, error)}</Alert>}
      <div className={styles.actions}>
        <Link to={`/equipment/${e.id}/edit`} className={buttonClass()}>
          {t('common.edit')}
        </Link>
        <Button onClick={() => setShowPhoto((v) => !v)}>{e.photoUrl ? t('equipment.changePhoto') : t('equipment.addPhoto')}</Button>
        <Button onClick={toggleStatus} disabled={update.isPending}>
          {removed ? t('equipment.markActive') : t('equipment.markRemoved')}
        </Button>
        <Button
          variant="danger"
          disabled={remove.isPending}
          onClick={async () => {
            if (!window.confirm(t('equipment.deleteConfirm'))) return
            await remove.mutateAsync()
            navigate(`/gyms/${e.gymId}`, { replace: true })
          }}
        >
          {t('common.delete')}
        </Button>
      </div>
      {showPhoto && (
        <Card>
          <PhotoInput file={photo} onChange={setPhoto} label={t('equipment.photo')} />
          <Button
            variant="primary"
            disabled={!photo || upload.isPending}
            onClick={async () => {
              if (!photo) return
              await upload.mutateAsync({ id: e.id, file: photo })
              setPhoto(null)
              setShowPhoto(false)
            }}
          >
            {upload.isPending ? t('equipment.uploading') : t('equipment.uploadPhoto')}
          </Button>
        </Card>
      )}
    </>
  )
}
