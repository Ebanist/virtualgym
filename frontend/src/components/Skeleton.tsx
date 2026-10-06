import { useTranslation } from 'react-i18next'
import styles from './Skeleton.module.css'

/** Szkielet listy podczas ładowania danych. */
export function SkeletonList({ count = 3 }: { count?: number }) {
  const { t } = useTranslation()
  return (
    <div className={styles.list} role="status" aria-label={t('app.loading')}>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className={styles.item} />
      ))}
    </div>
  )
}
