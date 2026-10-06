import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { useActiveSession } from '../../api/workouts'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'

export function ActiveWorkoutBanner() {
  const { t } = useTranslation()
  const active = useActiveSession()
  if (!active.data) return null
  return (
    <Alert kind="success">
      <p style={{ margin: '0 0 8px' }}>{t('workout.inProgress', { title: active.data.title })}</p>
      <Link to={`/workout/${active.data.id}`} className={buttonClass({ variant: 'primary', block: true })}>
        {t('workout.resume')}
      </Link>
    </Alert>
  )
}
