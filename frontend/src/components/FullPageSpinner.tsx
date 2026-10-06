import { useTranslation } from 'react-i18next'
import { Icon } from './Icon'

export function FullPageSpinner() {
  const { t } = useTranslation()
  return (
    <div role="status" style={{ display: 'grid', placeItems: 'center', minHeight: '60vh', color: 'var(--accent-text)' }}>
      <Icon name="gym" size={40} title={t('app.loading')} />
    </div>
  )
}
