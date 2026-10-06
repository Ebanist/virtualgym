import { forwardRef, useId, type TextareaHTMLAttributes } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Field.module.css'

export interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
}

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(function TextArea(
  { label, error, id, rows = 3, ...rest },
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
      <textarea
        ref={ref}
        id={inputId}
        rows={rows}
        className={[styles.input, error && styles.invalid].filter(Boolean).join(' ')}
        aria-invalid={error ? true : undefined}
        {...rest}
      />
      {error && (
        <span className={styles.error} role="alert">
          {t(error)}
        </span>
      )}
    </div>
  )
})
