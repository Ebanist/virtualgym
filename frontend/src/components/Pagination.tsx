import { useTranslation } from 'react-i18next'
import { Button } from './Button'
import styles from './Pagination.module.css'

interface Props {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

/** Prosta paginacja „poprzednia / następna” – wygodna na telefonie. */
export function Pagination({ page, totalPages, onChange }: Props) {
  const { t } = useTranslation()
  if (totalPages <= 1) return null
  return (
    <nav className={styles.pagination} aria-label={t('pagination.label')}>
      <Button small disabled={page <= 0} onClick={() => onChange(page - 1)}>
        {t('pagination.prev')}
      </Button>
      <span className={styles.info}>{t('pagination.info', { page: page + 1, total: totalPages })}</span>
      <Button small disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        {t('pagination.next')}
      </Button>
    </nav>
  )
}
