import { useTranslation } from 'react-i18next'
import type { Exercise } from '../../api/exercises'
import { Badge } from '../../components/Badge'
import { exerciseOrigin } from './origin'

/** Odznaka pochodzenia ćwiczenia (Biblioteka / Moje + kłódka dla prywatnych / Społeczność). */
export function OriginBadge({ exercise }: { exercise: Pick<Exercise, 'scope' | 'mine' | 'visibility'> }) {
  const { t } = useTranslation()
  const origin = exerciseOrigin(exercise)
  if (origin === 'library') return <Badge icon="book">{t('exercises.origin.library')}</Badge>
  if (origin === 'community') {
    return (
      <Badge tone="info" icon="users">
        {t('exercises.origin.community')}
      </Badge>
    )
  }
  const isPrivate = exercise.visibility === 'PRIVATE'
  return (
    <Badge tone="accent" icon={isPrivate ? 'lock' : 'users'}>
      {t('exercises.origin.mine')}
      {isPrivate ? ` · ${t('exercises.visibility.PRIVATE')}` : ` · ${t('exercises.visibility.GYM')}`}
    </Badge>
  )
}
