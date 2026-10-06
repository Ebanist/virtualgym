import { useTranslation } from 'react-i18next'
import { useAuth } from '../../app/AuthProvider'
import { Card } from '../../components/Card'

export function HomePage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  return (
    <>
      <h1>{t('home.greeting', { name: user?.displayName })}</h1>
      <Card>
        <p>{t('home.intro')}</p>
      </Card>
    </>
  )
}
