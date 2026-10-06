import { Icon, type IconName } from './Icon'
import styles from './Segmented.module.css'

interface Option<T extends string> {
  value: T
  label: string
  icon?: IconName
}

interface Props<T extends string> {
  label: string
  value: T
  options: Option<T>[]
  onChange: (value: T) => void
}

/** Przełącznik kilku opcji (zakładki / wybór motywu). */
export function Segmented<T extends string>({ label, value, options, onChange }: Props<T>) {
  return (
    <div className={styles.segmented} role="tablist" aria-label={label}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="tab"
          aria-selected={o.value === value}
          className={[styles.option, o.value === value && styles.active].filter(Boolean).join(' ')}
          onClick={() => onChange(o.value)}
        >
          {o.icon && <Icon name={o.icon} size={16} />}
          {o.label}
        </button>
      ))}
    </div>
  )
}
