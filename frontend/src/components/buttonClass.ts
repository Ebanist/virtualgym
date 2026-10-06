import styles from './Button.module.css'

export type ButtonVariant = 'default' | 'primary' | 'danger' | 'ghost'

export interface ButtonStyle {
  variant?: ButtonVariant
  block?: boolean
  small?: boolean
}

/** Klasy przycisku – do użycia także na linkach (<Link className={buttonClass(...)}>). */
export function buttonClass({ variant = 'default', block, small }: ButtonStyle = {}) {
  return [styles.button, variant !== 'default' && styles[variant], block && styles.block, small && styles.small]
    .filter(Boolean)
    .join(' ')
}
