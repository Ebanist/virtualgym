import { useId } from 'react'
import fieldStyles from './Field.module.css'
import { Icon } from './Icon'
import styles from './SearchField.module.css'

interface Props {
  label: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

/** Pole wyszukiwania z ikoną lupy (etykieta dostępna dla czytników, wizualnie jako placeholder). */
export function SearchField({ label, value, onChange, placeholder }: Props) {
  const id = useId()
  return (
    <div className={`${fieldStyles.field} ${styles.wrap}`}>
      <label htmlFor={id} className="visually-hidden">
        {label}
      </label>
      <Icon name="search" size={18} className={styles.icon} />
      <input
        id={id}
        type="search"
        className={`${fieldStyles.input} ${styles.input}`}
        placeholder={placeholder ?? label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  )
}
