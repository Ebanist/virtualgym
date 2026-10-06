import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Field.module.css'

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  /** Klucz i18n albo gotowy tekst błędu. */
  error?: string
  hint?: ReactNode
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, hint, id, className, ...rest },
  ref,
) {
  const { t } = useTranslation()
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`
  const hintId = `${inputId}-hint`
  return (
    <div className={styles.field}>
      <label htmlFor={inputId} className={styles.label}>
        {label}
      </label>
      <input
        ref={ref}
        id={inputId}
        className={[styles.input, error && styles.invalid, className].filter(Boolean).join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={[error && errorId, hint && hintId].filter(Boolean).join(' ') || undefined}
        {...rest}
      />
      {hint && !error && (
        <span id={hintId} className={styles.hint}>
          {hint}
        </span>
      )}
      {error && (
        <span id={errorId} className={styles.error} role="alert">
          {t(error)}
        </span>
      )}
    </div>
  )
})
