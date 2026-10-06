import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useGymSearch } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { EmptyState } from '../../components/EmptyState'
import { Icon } from '../../components/Icon'
import { PageHeader } from '../../components/PageHeader'
import { Pagination } from '../../components/Pagination'
import { SearchField } from '../../components/SearchField'
import { SkeletonList } from '../../components/Skeleton'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { GymList } from './GymList'
import styles from './Gyms.module.css'

export function GymSearchPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const [city, setCity] = useState(params.get('city') ?? '')
  const page = Number(params.get('page') ?? 0)
  const debouncedQ = useDebouncedValue(q)
  const debouncedCity = useDebouncedValue(city)

  // Filtry w URL – powrót z profilu siłowni zachowuje wyniki.
  useEffect(() => {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        const changed = (next.get('q') ?? '') !== debouncedQ || (next.get('city') ?? '') !== debouncedCity
        if (debouncedQ) next.set('q', debouncedQ)
        else next.delete('q')
        if (debouncedCity) next.set('city', debouncedCity)
        else next.delete('city')
        if (changed) next.delete('page')
        return next
      },
      { replace: true },
    )
  }, [debouncedQ, debouncedCity, setParams])

  const search = useGymSearch(debouncedQ, debouncedCity, page)
  const addLink = (
    <Link to="/gyms/new" className={buttonClass({ variant: 'primary', small: true })}>
      <Icon name="plus" size={16} />
      {t('gyms.add')}
    </Link>
  )

  return (
    <>
      <PageHeader title={t('gyms.searchTitle')} action={addLink} />
      <div className={styles.filters} role="search">
        <SearchField label={t('gyms.name')} placeholder={t('gyms.searchPlaceholder')} value={q} onChange={setQ} />
        <SearchField label={t('gyms.city')} value={city} onChange={setCity} />
      </div>
      {search.isError && <Alert kind="error">{errorMessage(t, search.error)}</Alert>}
      {search.isPending && <SkeletonList />}
      {search.data?.content.length === 0 && (
        <EmptyState icon="gym" action={addLink}>
          {t('gyms.noResults')}
        </EmptyState>
      )}
      {search.data && search.data.content.length > 0 && (
        <>
          <GymList gyms={search.data.content} emptyText="" />
          <Pagination
            page={search.data.page}
            totalPages={search.data.totalPages}
            onChange={(p) =>
              setParams((prev) => {
                const next = new URLSearchParams(prev)
                next.set('page', String(p))
                return next
              })
            }
          />
        </>
      )}
    </>
  )
}
