import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { useActiveSession } from '../../api/workouts'
import { buttonClass } from '../../components/buttonClass'
import { Icon } from '../../components/Icon'
import styles from './Home.module.css'

export function ActiveWorkoutBanner() {
  const { t } = useTranslation()
  const active = useActiveSession()
  if (!active.data) return null
  const sets = active.data.exercises.flatMap((e) => e.sets)
  const done = sets.filter((s) => s.completed).length
  return (
    <section className={`${styles.hero} ${styles.heroActive}`} aria-label={t('workout.inProgress', { title: active.data.title })}>
      <span className={styles.heroLabel}>
        <span className={styles.pulse} /> {t('workout.live')}
      </span>
      <div className={styles.heroTitle}>{active.data.title}</div>
      <p className={styles.heroText}>{t('workout.setsProgress', { done, total: sets.length })}</p>
      <Link to={`/workout/${active.data.id}`} className={buttonClass({ variant: 'primary', block: true })}>
        <Icon name="play" size={16} />
        {t('workout.resume')}
      </Link>
    </section>
  )
}
