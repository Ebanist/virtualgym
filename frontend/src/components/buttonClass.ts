import styles from './Button.module.css'

export type ButtonVariant = 'default' | 'primary' | 'danger' | 'ghost'

export interface ButtonStyle {
  variant?: ButtonVariant
  block?: boolean
  small?: boolean
  iconOnly?: boolean
}

/** Klasy przycisku – do użycia także na linkach (<Link className={buttonClass(...)}>). */
export function buttonClass({ variant = 'default', block, small, iconOnly }: ButtonStyle = {}) {
  return [
    styles.button,
    variant !== 'default' && styles[variant],
    block && styles.block,
    small && styles.small,
    iconOnly && styles.iconOnly,
  ]
    .filter(Boolean)
    .join(' ')
}
