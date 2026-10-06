import styles from './Layout.module.css'

/** Logo: sztanga w limonkowym kwadracie + nazwa. */
export function Logo({ large }: { large?: boolean }) {
  return (
    <span className={[styles.logo, large && styles.logoLarge].filter(Boolean).join(' ')}>
      <svg className={styles.logoMark} viewBox="0 0 32 32" aria-hidden="true">
        <rect width="32" height="32" rx="9" fill="var(--accent)" />
        <path fill="var(--on-accent)" d="M5 13h3v6H5zm19 0h3v6h-3zM9 10h3v12H9zm11 0h3v12h-3zm-8 5h8v2h-8z" />
      </svg>
      <span>
        Gym<span className={styles.logoAccent}>Planner</span>
      </span>
    </span>
  )
}
