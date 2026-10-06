import type { ButtonHTMLAttributes } from 'react'
import { buttonClass, type ButtonStyle } from './buttonClass'
import { Icon, type IconName } from './Icon'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, ButtonStyle {
  icon?: IconName
}

export function Button({ variant, block, small, iconOnly, icon, className, type = 'button', children, ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={[buttonClass({ variant, block, small, iconOnly }), className].filter(Boolean).join(' ')}
      {...rest}
    >
      {icon && <Icon name={icon} size={small ? 16 : 18} />}
      {children}
    </button>
  )
}
