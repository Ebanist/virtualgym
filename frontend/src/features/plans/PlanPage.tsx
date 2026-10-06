import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { planActions, useDeletePlan, usePlan, usePlanMutation, type Plan, type PlanDay } from '../../api/plans'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { buttonClass } from '../../components/buttonClass'
import { TextField } from '../../components/TextField'
import { PlanItemRow } from './PlanItemRow'
import styles from './Plans.module.css'

export function PlanPage() {
  const { planId = '' } = useParams()
  const { t } = useTranslation()
  const plan = usePlan(planId)
  if (plan.isPending) return <p>{t('app.loading')}</p>
  if (plan.isError) return <Alert kind="error">{errorMessage(t, plan.error)}</Alert>
  return <PlanView plan={plan.data} />
}

function PlanView({ plan }: { plan: Plan }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(plan.name)
  const update = usePlanMutation((n: string) => planActions.update(plan.id, n, plan.description))
  const copy = usePlanMutation(() => planActions.copy(plan.id))
  const archive = usePlanMutation(() => (plan.archived ? planActions.unarchive(plan.id) : planActions.archive(plan.id)))
  const addDay = usePlanMutation((dayName: string) => planActions.addDay(plan.id, dayName))
  const remove = useDeletePlan()
  const error = update.error ?? copy.error ?? archive.error ?? addDay.error ?? remove.error

  return (
    <>
      <p className={styles.meta}>
        <Link to="/plans">← {t('plans.title')}</Link> · <Link to={`/gyms/${plan.gymId}`}>{plan.gymName}</Link>
      </p>
      {editing ? (
        <form
          className={styles.inlineForm}
          onSubmit={async (e) => {
            e.preventDefault()
            if (name.trim().length < 2) return
            await update.mutateAsync(name.trim())
            setEditing(false)
          }}
        >
          <TextField label={t('plans.name')} value={name} onChange={(e) => setName(e.target.value)} maxLength={100} autoFocus />
          <Button type="submit" variant="primary" disabled={update.isPending}>
            {t('common.save')}
          </Button>
        </form>
      ) : (
        <h1>{plan.name}</h1>
      )}
      {plan.description && <p>{plan.description}</p>}
      {plan.archived && <Alert kind="info">{t('plans.isArchived')}</Alert>}
      {plan.warningCount > 0 && <Alert kind="warning">{t('plans.warningInfo', { count: plan.warningCount })}</Alert>}
      {error && <Alert kind="error">{errorMessage(t, error)}</Alert>}
      <div className={styles.actions}>
        {!editing && <Button small onClick={() => setEditing(true)}>{t('plans.rename')}</Button>}
        <Button
          small
          disabled={copy.isPending}
          onClick={async () => {
            const created = await copy.mutateAsync(undefined)
            navigate(`/plans/${created.id}`)
          }}
        >
          {t('plans.copy')}
        </Button>
        <Button small disabled={archive.isPending} onClick={() => archive.mutate(undefined)}>
          {plan.archived ? t('plans.unarchive') : t('plans.archive')}
        </Button>
        <Button
          small
          variant="danger"
          disabled={remove.isPending}
          onClick={async () => {
            if (!window.confirm(t('plans.deleteConfirm'))) return
            await remove.mutateAsync(plan.id)
            navigate('/plans', { replace: true })
          }}
        >
          {t('common.delete')}
        </Button>
      </div>

      {plan.days.length === 0 && <p className={styles.meta}>{t('plans.noDays')}</p>}
      {plan.days.map((day, index) => (
        <DaySection key={day.id} plan={plan} day={day} isFirst={index === 0} isLast={index === plan.days.length - 1} />
      ))}
      <AddDayForm defaultName={t('plans.defaultDayName', { letter: String.fromCharCode(65 + plan.days.length) })} onAdd={(n) => addDay.mutateAsync(n)} pending={addDay.isPending} />
    </>
  )
}

function DaySection({ plan, day, isFirst, isLast }: { plan: Plan; day: PlanDay; isFirst: boolean; isLast: boolean }) {
  const { t } = useTranslation()
  const [renaming, setRenaming] = useState(false)
  const [name, setName] = useState(day.name)
  const rename = usePlanMutation((n: string) => planActions.renameDay(plan.id, day.id, n))
  const move = usePlanMutation((direction: 'UP' | 'DOWN') => planActions.moveDay(plan.id, day.id, direction))
  const remove = usePlanMutation(() => planActions.deleteDay(plan.id, day.id))
  const moveItem = usePlanMutation(({ itemId, direction }: { itemId: string; direction: 'UP' | 'DOWN' }) =>
    planActions.moveItem(plan.id, itemId, direction),
  )
  const removeItem = usePlanMutation((itemId: string) => planActions.deleteItem(plan.id, itemId))
  const error = rename.error ?? move.error ?? remove.error ?? moveItem.error ?? removeItem.error

  return (
    <section className={styles.day} aria-label={day.name}>
      <div className={styles.dayHeader}>
        {renaming ? (
          <form
            className={styles.inlineForm}
            onSubmit={async (e) => {
              e.preventDefault()
              if (!name.trim()) return
              await rename.mutateAsync(name.trim())
              setRenaming(false)
            }}
          >
            <TextField label={t('plans.dayName')} value={name} onChange={(e) => setName(e.target.value)} maxLength={100} autoFocus />
            <Button type="submit" small variant="primary">
              {t('common.save')}
            </Button>
          </form>
        ) : (
          <h2>{day.name}</h2>
        )}
        <div className={styles.moveButtons} style={{ flexDirection: 'row' }}>
          <button type="button" className={styles.iconButton} aria-label={t('plans.moveDayUp')} disabled={isFirst || move.isPending} onClick={() => move.mutate('UP')}>
            ↑
          </button>
          <button type="button" className={styles.iconButton} aria-label={t('plans.moveDayDown')} disabled={isLast || move.isPending} onClick={() => move.mutate('DOWN')}>
            ↓
          </button>
        </div>
      </div>
      {error && <Alert kind="error">{errorMessage(t, error)}</Alert>}
      {day.items.length === 0 && <p className={styles.dayFooter}>{t('plans.noItems')}</p>}
      <ol className={styles.items}>
        {day.items.map((item, index) => (
          <PlanItemRow
            key={item.id}
            planId={plan.id}
            item={item}
            isFirst={index === 0}
            isLast={index === day.items.length - 1}
            busy={moveItem.isPending || removeItem.isPending}
            onMove={(direction) => moveItem.mutate({ itemId: item.id, direction })}
            onDelete={() => {
              if (window.confirm(t('plans.deleteItemConfirm'))) removeItem.mutate(item.id)
            }}
          />
        ))}
      </ol>
      <div className={`${styles.dayFooter} ${styles.actions}`}>
        <Link to={`/plans/${plan.id}/days/${day.id}/add`} className={buttonClass({ variant: 'primary', small: true })}>
          {t('plans.addExercise')}
        </Link>
        {!renaming && (
          <Button small onClick={() => setRenaming(true)}>
            {t('plans.renameDay')}
          </Button>
        )}
        <Button
          small
          variant="danger"
          onClick={() => {
            if (window.confirm(t('plans.deleteDayConfirm'))) remove.mutate(undefined)
          }}
        >
          {t('plans.deleteDay')}
        </Button>
      </div>
    </section>
  )
}

function AddDayForm({ defaultName, onAdd, pending }: { defaultName: string; onAdd: (name: string) => Promise<unknown>; pending: boolean }) {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  return (
    <form
      className={styles.inlineForm}
      onSubmit={async (e) => {
        e.preventDefault()
        await onAdd(name.trim() || defaultName)
        setName('')
      }}
    >
      <TextField label={t('plans.newDay')} placeholder={defaultName} value={name} onChange={(e) => setName(e.target.value)} maxLength={100} />
      <Button type="submit" disabled={pending}>
        {t('plans.addDay')}
      </Button>
    </form>
  )
}
