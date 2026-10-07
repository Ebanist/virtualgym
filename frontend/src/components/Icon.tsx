import type { SVGProps } from 'react'

/** Ścieżki ikon (24×24, obrys 2px, currentColor). Własny mały zestaw zamiast biblioteki. */
const PATHS = {
  home: 'M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z',
  gym: 'M3 12h18M6 7v10M3 9v6M18 7v10M21 9v6',
  plan: 'M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01',
  history: 'M3 12a9 9 0 1 0 3-6.7M3 4v5h5M12 7v5l3 2',
  user: 'M20 21a8 8 0 0 0-16 0M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10z',
  plus: 'M12 5v14M5 12h14',
  check: 'M5 12.5 10 17.5 19 7',
  chevronLeft: 'M15 18 9 12l6-6',
  chevronRight: 'm9 18 6-6-6-6',
  arrowUp: 'M12 19V5M6 11l6-6 6 6',
  arrowDown: 'M12 5v14M6 13l6 6 6-6',
  timer: 'M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 10v4l2 2M9 2h6',
  warning: 'M12 3 2 20h20zM12 10v4M12 17h.01',
  camera:
    'M3 8a2 2 0 0 1 2-2h2l2-2h6l2 2h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM12 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
  search: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM21 21l-4.3-4.3',
  edit: 'M4 20h4L19 9l-4-4L4 16zM14 6l4 4',
  trash: 'M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3',
  copy: 'M9 9h11v11H9zM5 15H4V4h11v1',
  archive: 'M3 4h18v4H3zM5 8v12h14V8M10 12h4',
  play: 'M7 4v16l13-8z',
  flag: 'M5 21V4M5 4h11l-2 4 2 4H5',
  sun: 'M12 17a5 5 0 1 0 0-10 5 5 0 0 0 0 10zM12 1v2M12 21v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M1 12h2M21 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4',
  moon: 'M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z',
  monitor: 'M3 4h18v12H3zM8 20h8M12 16v4',
  logout: 'M15 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M10 17l5-5-5-5M15 12H3',
  link: 'M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1',
  users:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8',
  pin: 'M12 22s7-6.2 7-12a7 7 0 1 0-14 0c0 5.8 7 12 7 12zM12 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
  x: 'M18 6 6 18M6 6l12 12',
  bolt: 'M13 2 4 14h7l-1 8 9-12h-7z',
  book: 'M4 4h11a3 3 0 0 1 3 3v13H7a3 3 0 0 1-3-3zM4 17a3 3 0 0 1 3-3h11',
  lock: 'M5 11h14v10H5zM8 11V7a4 4 0 0 1 8 0v4',
} as const

export type IconName = keyof typeof PATHS

interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'name'> {
  name: IconName
  size?: number
  /** Gdy podany – ikona jest czytana przez czytniki ekranu; inaczej dekoracyjna. */
  title?: string
}

export function Icon({ name, size = 20, title, ...rest }: IconProps) {
  const filled = name === 'play' || name === 'bolt'
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden={title ? undefined : true}
      role={title ? 'img' : undefined}
      focusable="false"
      {...rest}
    >
      {title && <title>{title}</title>}
      <path d={PATHS[name]} />
    </svg>
  )
}
