import { useTranslation } from 'react-i18next'

export function FullPageSpinner() {
  const { t } = useTranslation()
  return (
    <p role="status" style={{ padding: 'var(--space-6)', textAlign: 'center' }}>
      {t('app.loading')}
    </p>
  )
}
