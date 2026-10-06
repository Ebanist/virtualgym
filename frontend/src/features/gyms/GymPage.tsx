import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useGym, useMembership } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { buttonClass } from '../../components/buttonClass'
import { Icon } from '../../components/Icon'
import { SkeletonList } from '../../components/Skeleton'
import { GymEquipmentSection } from '../equipment/GymEquipmentSection'
import styles from './Gyms.module.css'

export function GymPage() {
  const { gymId = '' } = useParams()
  const { t } = useTranslation()
  const gym = useGym(gymId)
  const { join, leave } = useMembership(gymId)
  const mutationError = join.error ?? leave.error

  if (gym.isPending) return <SkeletonList count={4} />
  if (gym.isError) return <Alert kind="error">{errorMessage(t, gym.error)}</Alert>

  const g = gym.data
  return (
    <>
      <section className={styles.hero}>
        {g.member && (
          <Badge tone="accent" icon="check">
            {t('gyms.memberBadge')}
          </Badge>
        )}
        <h1>{g.name}</h1>
        <div className={styles.stats}>
          <span className={styles.stat}>
            <Icon name="pin" size={16} />
            {g.city}, {g.address}
          </span>
          <span className={styles.stat}>
            <Icon name="users" size={16} />
            {t('gyms.members', { count: g.memberCount })}
          </span>
        </div>
        {g.description && <p className={styles.description}>{g.description}</p>}
        {mutationError && <Alert kind="error">{errorMessage(t, mutationError)}</Alert>}
        <div className={styles.actions}>
          {g.member ? (
            <Link to={`/plans/new?gymId=${g.id}`} className={buttonClass({ variant: 'primary' })}>
              <Icon name="plus" size={18} />
              {t('plans.new')}
            </Link>
          ) : (
            <Button variant="primary" icon="plus" disabled={join.isPending} onClick={() => join.mutate()}>
              {t('gyms.join')}
            </Button>
          )}
          <Link to={`/gyms/${g.id}/exercises`} className={buttonClass()}>
            <Icon name="book" size={18} />
            {t('exercises.showAvailable')}
          </Link>
          {g.member && (
            <Button
              variant="ghost"
              icon="logout"
              disabled={leave.isPending}
              onClick={() => {
                if (window.confirm(t('gyms.leaveConfirm'))) leave.mutate()
              }}
            >
              {t('gyms.leave')}
            </Button>
          )}
        </div>
      </section>
      <GymEquipmentSection gymId={g.id} member={g.member} />
    </>
  )
}
