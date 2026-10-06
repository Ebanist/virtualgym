import type { ButtonHTMLAttributes } from 'react'
import { buttonClass, type ButtonStyle } from './buttonClass'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, ButtonStyle {}

export function Button({ variant, block, small, className, type = 'button', ...rest }: ButtonProps) {
  return <button type={type} className={[buttonClass({ variant, block, small }), className].filter(Boolean).join(' ')} {...rest} />
}
