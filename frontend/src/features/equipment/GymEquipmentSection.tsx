import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { EQUIPMENT_CATEGORIES, useEquipmentList, type EquipmentCategory } from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { FilterChips } from '../../components/Chips'
import { EmptyState } from '../../components/EmptyState'
import { Icon } from '../../components/Icon'
import { Pagination } from '../../components/Pagination'
import { SearchField } from '../../components/SearchField'
import { SkeletonList } from '../../components/Skeleton'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { EquipmentList } from './EquipmentList'
import styles from './Equipment.module.css'

export function GymEquipmentSection({ gymId, member }: { gymId: string; member: boolean }) {
  const { t } = useTranslation()
  const [q, setQ] = useState('')
  const [category, setCategory] = useState<EquipmentCategory | ''>('')
  const [page, setPage] = useState(0)
  const debouncedQ = useDebouncedValue(q)
  const list = useEquipmentList(gymId, { q: debouncedQ || undefined, category: category || undefined, page })
  const addLink = member ? (
    <Link to={`/gyms/${gymId}/equipment/new`} className={buttonClass({ variant: 'primary', small: true })}>
      <Icon name="plus" size={16} />
      {t('equipment.add')}
    </Link>
  ) : null

  return (
    <section>
      <div className={styles.sectionHeader}>
        <h2>
          {t('equipment.title')}
          {list.data && <span className={styles.count}>{list.data.totalElements}</span>}
        </h2>
        {addLink}
      </div>
      <SearchField
        label={t('equipment.search')}
        value={q}
        onChange={(value) => {
          setQ(value)
          setPage(0)
        }}
      />
      <FilterChips
        label={t('equipment.categoryLabel')}
        value={category}
        allLabel={t('equipment.allCategories')}
        options={EQUIPMENT_CATEGORIES.map((c) => ({ value: c, label: t(`equipment.category.${c}`) }))}
        onChange={(value) => {
          setCategory(value)
          setPage(0)
        }}
      />
      {list.isError && <Alert kind="error">{errorMessage(t, list.error)}</Alert>}
      {list.isPending && <SkeletonList />}
      {list.data?.content.length === 0 && (
        <EmptyState icon="gym" action={addLink}>
          {member ? t('equipment.emptyMember') : t('equipment.empty')}
        </EmptyState>
      )}
      {list.data && list.data.content.length > 0 && (
        <>
          <EquipmentList items={list.data.content} emptyText="" />
          <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}
