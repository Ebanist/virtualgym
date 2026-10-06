import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { usePlans } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { buttonClass } from '../../components/buttonClass'
import listStyles from '../../components/List.module.css'
import styles from './Plans.module.css'

export function PlansPage() {
  const { t } = useTranslation()
  const [archived, setArchived] = useState(false)
  const plans = usePlans(archived)
  return (
    <>
      <div className={styles.header}>
        <h1>{t('plans.title')}</h1>
        <Link to="/plans/new" className={buttonClass({ variant: 'primary', small: true })}>
          {t('plans.new')}
        </Link>
      </div>
      <div className={styles.tabs} role="tablist">
        <Button small variant={archived ? 'default' : 'primary'} role="tab" aria-selected={!archived} onClick={() => setArchived(false)}>
          {t('plans.active')}
        </Button>
        <Button small variant={archived ? 'primary' : 'default'} role="tab" aria-selected={archived} onClick={() => setArchived(true)}>
          {t('plans.archived')}
        </Button>
      </div>
      {plans.isError && <Alert kind="error">{errorMessage(t, plans.error)}</Alert>}
      {plans.isPending && <p>{t('app.loading')}</p>}
      {plans.data?.length === 0 && (
        <p className={listStyles.empty}>{archived ? t('plans.noArchived') : t('plans.empty')}</p>
      )}
      <ul className={listStyles.list}>
        {plans.data?.map((plan) => (
          <li key={plan.id}>
            <Link to={`/plans/${plan.id}`} className={listStyles.item}>
              <span className={listStyles.title}>{plan.name}</span>
              <div className={listStyles.meta}>
                {plan.gymName} · {t('plans.daysCount', { count: plan.dayCount })} ·{' '}
                {t('plans.exercisesCount', { count: plan.exerciseCount })}
              </div>
              {plan.warningCount > 0 && <div className={styles.warning}>⚠ {t('plans.warningCount', { count: plan.warningCount })}</div>}
            </Link>
          </li>
        ))}
      </ul>
    </>
  )
}
