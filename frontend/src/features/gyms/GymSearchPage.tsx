import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { errorMessage } from '../../api/errors'
import { useGymSearch } from '../../api/gyms'
import { Alert } from '../../components/Alert'
import { buttonClass } from '../../components/buttonClass'
import { Pagination } from '../../components/Pagination'
import { TextField } from '../../components/TextField'
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

  return (
    <>
      <div className={styles.header}>
        <h1>{t('gyms.searchTitle')}</h1>
        <Link to="/gyms/new" className={buttonClass({ variant: 'primary', small: true })}>
          {t('gyms.add')}
        </Link>
      </div>
      <div className={styles.filters} role="search">
        <TextField label={t('gyms.name')} value={q} onChange={(e) => setQ(e.target.value)} />
        <TextField label={t('gyms.city')} value={city} onChange={(e) => setCity(e.target.value)} />
      </div>
      {search.isError && <Alert kind="error">{errorMessage(t, search.error)}</Alert>}
      {search.isPending && <p>{t('app.loading')}</p>}
      {search.data && (
        <>
          <GymList gyms={search.data.content} emptyText={t('gyms.noResults')} />
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
