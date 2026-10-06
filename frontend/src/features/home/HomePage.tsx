import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useMyGyms } from '../../api/gyms'
import { useActiveSession } from '../../api/workouts'
import { useAuth } from '../../app/AuthProvider'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { EmptyState } from '../../components/EmptyState'
import { Icon, type IconName } from '../../components/Icon'
import { SkeletonList } from '../../components/Skeleton'
import { GymList } from '../gyms/GymList'
import { ActiveWorkoutBanner } from './ActiveWorkoutBanner'
import { AdHocWorkout } from './AdHocWorkout'
import styles from './Home.module.css'

export function HomePage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const myGyms = useMyGyms()
  const active = useActiveSession()
  return (
    <>
      <div className={styles.hello}>
        <h1>{t('home.greeting', { name: user?.displayName })}</h1>
        <p>{t('home.subtitle')}</p>
      </div>
      <ActiveWorkoutBanner />
      {!active.data && myGyms.data && myGyms.data.length > 0 && <AdHocWorkout gyms={myGyms.data} />}

      <h2 className={styles.sectionTitle}>{t('home.quickActions')}</h2>
      <div className={styles.grid}>
        <Tile to="/plans/new" icon="plus" title={t('home.newPlan')} hint={t('home.newPlanHint')} />
        <Tile to="/plans" icon="plan" title={t('home.myPlans')} hint={t('home.plansHint')} />
        <Tile to="/gyms" icon="gym" title={t('nav.gyms')} hint={t('home.findGymHint')} />
        <Tile to="/exercises" icon="book" title={t('home.library')} hint={t('home.libraryHint')} />
      </div>

      <h2 className={styles.sectionTitle}>
        {t('home.myGyms')}
        <Link to="/gyms">{t('home.findGym')}</Link>
      </h2>
      {myGyms.isPending && <SkeletonList count={2} />}
      {myGyms.isError && <Alert kind="error">{errorMessage(t, myGyms.error)}</Alert>}
      {myGyms.data?.length === 0 && (
        <EmptyState
          icon="pin"
          action={
            <Link to="/gyms" className={buttonClass({ variant: 'primary' })}>
              {t('home.findGym')}
            </Link>
          }
        >
          {t('home.noGyms')}
        </EmptyState>
      )}
      {myGyms.data && myGyms.data.length > 0 && <GymList gyms={myGyms.data} emptyText="" />}
    </>
  )
}

function Tile({ to, icon, title, hint }: { to: string; icon: IconName; title: string; hint: string }) {
  return (
    <Link to={to} className={styles.tile}>
      <span className={styles.tileIcon}>
        <Icon name={icon} />
      </span>
      <span className={styles.tileTitle}>{title}</span>
      <span className={styles.tileHint}>{hint}</span>
    </Link>
  )
}
