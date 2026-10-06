import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useGym, useMembership } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { GymEquipmentSection } from '../equipment/GymEquipmentSection'
import styles from './Gyms.module.css'

export function GymPage() {
  const { gymId = '' } = useParams()
  const { t } = useTranslation()
  const gym = useGym(gymId)
  const { join, leave } = useMembership(gymId)
  const mutationError = join.error ?? leave.error

  if (gym.isPending) return <p>{t('app.loading')}</p>
  if (gym.isError) return <Alert kind="error">{errorMessage(t, gym.error)}</Alert>

  const g = gym.data
  return (
    <>
      <h1>{g.name}</h1>
      <p className={styles.meta}>
        {g.city}, {g.address} · {t('gyms.members', { count: g.memberCount })}
      </p>
      {mutationError && <Alert kind="error">{errorMessage(t, mutationError)}</Alert>}
      <div className={styles.actions}>
        {g.member ? (
          <Button
            variant="danger"
            disabled={leave.isPending}
            onClick={() => {
              if (window.confirm(t('gyms.leaveConfirm'))) leave.mutate()
            }}
          >
            {t('gyms.leave')}
          </Button>
        ) : (
          <Button variant="primary" disabled={join.isPending} onClick={() => join.mutate()}>
            {t('gyms.join')}
          </Button>
        )}
      </div>
      {g.description && (
        <Card>
          <p className={styles.description}>{g.description}</p>
        </Card>
      )}
      <GymEquipmentSection gymId={g.id} member={g.member} />
    </>
  )
}
