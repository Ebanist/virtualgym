import styles from './Chips.module.css'

interface Props<T extends string> {
  label: string
  value: T | ''
  allLabel: string
  options: { value: T; label: string }[]
  onChange: (value: T | '') => void
}

/** Filtr jako poziomo przewijane „pigułki” (wygodne kciukiem na telefonie). */
export function FilterChips<T extends string>({ label, value, allLabel, options, onChange }: Props<T>) {
  const all = [{ value: '' as const, label: allLabel }, ...options]
  return (
    <div className={styles.chips} role="group" aria-label={label}>
      {all.map((o) => (
        <button
          key={o.value || 'all'}
          type="button"
          aria-pressed={o.value === value}
          className={[styles.chip, o.value === value && styles.active].filter(Boolean).join(' ')}
          onClick={() => onChange(o.value)}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}
