import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { errorMessage } from '../../api/errors'
import type { GymSummary } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { SelectField } from '../../components/SelectField'
import { useStartWorkout } from '../workout/useStartWorkout'

/** Start treningu bez planu w wybranej siłowni. */
export function AdHocWorkout({ gyms }: { gyms: GymSummary[] }) {
  const { t } = useTranslation()
  const [gymId, setGymId] = useState(gyms[0]?.id ?? '')
  const start = useStartWorkout()
  if (gyms.length === 0) return null
  return (
    <Card>
      <h2>{t('workout.adHocTitle')}</h2>
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
      <Button variant="primary" block disabled={start.isPending || !gymId} onClick={() => void start.run({ gymId })}>
        {t('workout.startAdHoc')}
      </Button>
    </Card>
  )
}
