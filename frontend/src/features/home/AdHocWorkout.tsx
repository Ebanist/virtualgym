import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../api/errors'
import type { GymSummary } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { SelectField } from '../../components/SelectField'
import { useStartWorkout } from '../workout/useStartWorkout'
import styles from './Home.module.css'

/** Start treningu bez planu w wybranej siłowni. */
export function AdHocWorkout({ gyms }: { gyms: GymSummary[] }) {
  const { t } = useTranslation()
  const [gymId, setGymId] = useState(gyms[0]?.id ?? '')
  const start = useStartWorkout()
  if (gyms.length === 0) return null
  return (
    <section className={styles.hero}>
      <span className={styles.heroLabel}>{t('workout.quickStart')}</span>
      <h2 className={styles.heroTitle}>{t('workout.adHocTitle')}</h2>
      <p className={styles.heroText}>{t('home.adHocHint')}</p>
      {start.error && <Alert kind="error">{errorMessage(t, start.error)}</Alert>}
      {gyms.length > 1 && (
        <SelectField label={t('plans.gym')} value={gymId} onChange={(e) => setGymId(e.target.value)}>
          {gyms.map((g) => (
            <option key={g.id} value={g.id}>
              {g.name}
            </option>
          ))}
        </SelectField>
      )}
      <Button variant="primary" block icon="play" disabled={start.isPending || !gymId} onClick={() => void start.run({ gymId })}>
        {t('workout.startAdHoc')}
      </Button>
    </section>
  )
}
