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
import { Icon } from '../../components/Icon'
import { PageHeader } from '../../components/PageHeader'
import { SkeletonList } from '../../components/Skeleton'
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

  if (equipment.isPending) return <SkeletonList count={4} />
  if (equipment.isError) return <Alert kind="error">{errorMessage(t, equipment.error)}</Alert>
  const e = equipment.data
  const photoError = (location.state as { photoError?: string } | null)?.photoError

  return (
    <>
      <PageHeader
        title={e.name}
        subtitle={t(`equipment.category.${e.category}`)}
        back={{ to: `/gyms/${e.gymId}`, label: e.gymName }}
      />
      {photoError && <Alert kind="error">{photoError}</Alert>}
      {e.status === 'REMOVED_FROM_GYM' && <Alert kind="warning">{t('equipment.removedInfo')}</Alert>}
      {!e.member && <Alert kind="info">{t('equipment.joinToEdit')}</Alert>}
      {e.photoUrl ? (
        <a href={e.photoUrl} target="_blank" rel="noreferrer">
          <img src={e.photoUrl} alt={e.name} className={styles.photo} />
        </a>
      ) : (
        <div className={styles.photoPlaceholder}>
          <Icon name="camera" size={28} />
          <span>{t('equipment.noPhoto')}</span>
        </div>
      )}
      <dl className={styles.tiles}>
        <div className={styles.tile}>
          <dt>{t('equipment.typeShort')}</dt>
          <dd>{e.equipmentType?.name ?? '—'}</dd>
        </div>
        <div className={styles.tile}>
          <dt>{t('equipment.quantityLabel')}</dt>
          <dd className="num">{e.quantity ?? '—'}</dd>
        </div>
        <div className={styles.tile}>
          <dt>{t('equipment.statusLabel')}</dt>
          <dd className={e.status === 'REMOVED_FROM_GYM' ? styles.statusWarn : styles.statusOk}>
            {t(`equipment.status.${e.status}`)}
          </dd>
        </div>
        <div className={styles.tile}>
          <dt>{t('equipment.addedBy')}</dt>
          <dd>{e.createdBy.displayName}</dd>
        </div>
      </dl>
      {e.description && (
        <Card>
          <p className={styles.descriptionText}>{e.description}</p>
        </Card>
      )}
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
        <Link to={`/equipment/${e.id}/edit`} className={buttonClass({ small: true })}>
          <Icon name="edit" size={16} />
          {t('common.edit')}
        </Link>
        <Button small icon="camera" onClick={() => setShowPhoto((v) => !v)}>
          {e.photoUrl ? t('equipment.changePhoto') : t('equipment.addPhoto')}
        </Button>
        <Button small icon={removed ? 'check' : 'warning'} onClick={toggleStatus} disabled={update.isPending}>
          {removed ? t('equipment.markActive') : t('equipment.markRemoved')}
        </Button>
        <Button
          small
          icon="trash"
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
            icon="camera"
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
