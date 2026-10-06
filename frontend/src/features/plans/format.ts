import type { TFunction } from 'i18next'
import type { PlanItem } from '../../api/plans'

/** „3 × 8–12 · 40 kg · przerwa 90 s” */
export function formatItemParams(t: TFunction, item: Pick<PlanItem, 'sets' | 'repsMin' | 'repsMax' | 'targetWeightKg' | 'restSeconds'>) {
  const reps = item.repsMin === item.repsMax ? `${item.repsMin}` : `${item.repsMin}–${item.repsMax}`
  const parts = [`${item.sets} × ${reps}`]
  if (item.targetWeightKg !== undefined && item.targetWeightKg !== null) parts.push(`${item.targetWeightKg} kg`)
  parts.push(t('plans.restShort', { seconds: item.restSeconds }))
  return parts.join(' · ')
}
