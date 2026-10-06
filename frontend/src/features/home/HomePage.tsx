import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useMyGyms } from '../../api/gyms'
import { useAuth } from '../../app/AuthProvider'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { GymList } from '../gyms/GymList'

export function HomePage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const myGyms = useMyGyms()
  return (
    <>
      <h1>{t('home.greeting', { name: user?.displayName })}</h1>
      <h2>{t('home.myGyms')}</h2>
      {myGyms.isPending && <p>{t('app.loading')}</p>}
      {myGyms.isError && <Alert kind="error">{errorMessage(t, myGyms.error)}</Alert>}
      {myGyms.data && <GymList gyms={myGyms.data} emptyText={t('home.noGyms')} />}
      <Link to="/gyms" className={buttonClass({ block: true })}>
        {t('home.findGym')}
      </Link>
      <p>
        <Link to="/exercises">{t('exercises.library')}</Link>
      </p>
    </>
  )
}
