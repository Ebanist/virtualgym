import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { usePlans } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Badge } from '../../components/Badge'
import { buttonClass } from '../../components/buttonClass'
import { EmptyState } from '../../components/EmptyState'
import { Icon } from '../../components/Icon'
import { PageHeader } from '../../components/PageHeader'
import { Segmented } from '../../components/Segmented'
import { SkeletonList } from '../../components/Skeleton'
import listStyles from '../../components/List.module.css'
import styles from './Plans.module.css'

export function PlansPage() {
  const { t } = useTranslation()
  const [archived, setArchived] = useState(false)
  const plans = usePlans(archived)
  return (
    <>
      <PageHeader
        title={t('plans.title')}
        action={
          <Link to="/plans/new" className={buttonClass({ variant: 'primary', small: true })}>
            <Icon name="plus" size={16} />
            {t('plans.new')}
          </Link>
        }
      />
      <Segmented
        label={t('plans.title')}
        value={archived ? 'archived' : 'active'}
        options={[
          { value: 'active', label: t('plans.active') },
          { value: 'archived', label: t('plans.archived'), icon: 'archive' },
        ]}
        onChange={(v) => setArchived(v === 'archived')}
      />
      {plans.isError && <Alert kind="error">{errorMessage(t, plans.error)}</Alert>}
      {plans.isPending && <SkeletonList />}
      {plans.data?.length === 0 && (
        <EmptyState icon={archived ? 'archive' : 'plan'}>{archived ? t('plans.noArchived') : t('plans.empty')}</EmptyState>
      )}
      <ul className={listStyles.list}>
        {plans.data?.map((plan) => (
          <li key={plan.id}>
            <Link to={`/plans/${plan.id}`} className={listStyles.item}>
              <span className={listStyles.title}>{plan.name}</span>
              <div className={listStyles.meta}>
                <span className={listStyles.metaItem}>
                  <Icon name="pin" size={14} />
                  {plan.gymName}
                </span>
                <span>
                  {t('plans.daysCount', { count: plan.dayCount })} · {t('plans.exercisesCount', { count: plan.exerciseCount })}
                </span>
              </div>
              {plan.warningCount > 0 && (
                <span className={styles.warningCount}>
                  <Badge tone="warning" icon="warning">
                    {t('plans.warningCount', { count: plan.warningCount })}
                  </Badge>
                </span>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </>
  )
}
