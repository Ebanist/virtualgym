import { forwardRef, useId, type ReactNode, type SelectHTMLAttributes } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Field.module.css'

export interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
  children: ReactNode
}

export const SelectField = forwardRef<HTMLSelectElement, SelectFieldProps>(function SelectField(
  { label, error, id, children, ...rest },
  ref,
) {
  const { t } = useTranslation()
  const generatedId = useId()
  const inputId = id ?? generatedId
  return (
    <div className={styles.field}>
      <label htmlFor={inputId} className={styles.label}>
        {label}
      </label>
      <select
        ref={ref}
        id={inputId}
        className={[styles.input, error && styles.invalid].filter(Boolean).join(' ')}
        aria-invalid={error ? true : undefined}
        {...rest}
      >
        {children}
      </select>
      {error && (
        <span className={styles.error} role="alert">
          {t(error)}
        </span>
      )}
    </div>
  )
})
