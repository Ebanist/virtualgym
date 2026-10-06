import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { EQUIPMENT_CATEGORIES, useEquipmentList, type EquipmentCategory } from '../../api/equipment'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { Pagination } from '../../components/Pagination'
import { SelectField } from '../../components/SelectField'
import { TextField } from '../../components/TextField'
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

  return (
    <section>
      <div className={styles.sectionHeader}>
        <h2>{t('equipment.title')}</h2>
        {member && (
          <Link to={`/gyms/${gymId}/equipment/new`} className={buttonClass({ variant: 'primary', small: true })}>
            {t('equipment.add')}
          </Link>
        )}
      </div>
      <div className={styles.filters} role="search">
        <TextField
          label={t('equipment.search')}
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setPage(0)
          }}
        />
        <SelectField
          label={t('equipment.categoryLabel')}
          value={category}
          onChange={(e) => {
            setCategory(e.target.value as EquipmentCategory | '')
            setPage(0)
          }}
        >
          <option value="">{t('equipment.allCategories')}</option>
          {EQUIPMENT_CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {t(`equipment.category.${c}`)}
            </option>
          ))}
        </SelectField>
      </div>
      {list.isError && <Alert kind="error">{errorMessage(t, list.error)}</Alert>}
      {list.isPending && <p>{t('app.loading')}</p>}
      {list.data && (
        <>
          <EquipmentList
            items={list.data.content}
            emptyText={member ? t('equipment.emptyMember') : t('equipment.empty')}
          />
          <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}
