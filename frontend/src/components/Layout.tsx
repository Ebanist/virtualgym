import { useTranslation } from 'react-i18next'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { useTheme } from '../app/ThemeProvider'
import { Icon, type IconName } from './Icon'
import styles from './Layout.module.css'
import { Logo } from './Logo'

const NAV: { to: string; label: string; icon: IconName; end?: boolean }[] = [
  { to: '/', label: 'nav.home', icon: 'home', end: true },
  { to: '/gyms', label: 'nav.gyms', icon: 'gym' },
  { to: '/plans', label: 'nav.plans', icon: 'plan' },
  { to: '/history', label: 'nav.history', icon: 'history' },
  { to: '/profile', label: 'nav.profile', icon: 'user' },
]

const navClass = ({ isActive }: { isActive: boolean }) => [styles.link, isActive && styles.active].filter(Boolean).join(' ')

export function Layout() {
  const { t } = useTranslation()
  const { theme, setPreference } = useTheme()
  const next = theme === 'dark' ? 'light' : 'dark'
  return (
    <>
      <header className={styles.header}>
        <div className={styles.bar}>
          <Link to="/" className={styles.brand} aria-label={t('app.name')}>
            <Logo />
          </Link>
          {/* Na telefonie nawigacja jest dolnym paskiem (fixed), od 768px – w nagłówku. */}
          <nav className={styles.nav} aria-label={t('nav.menu')}>
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={navClass}>
                <Icon name={item.icon} size={22} className={styles.navIcon} />
                <span>{t(item.label)}</span>
              </NavLink>
            ))}
          </nav>
          <button
            type="button"
            className={styles.themeToggle}
            aria-label={t(next === 'light' ? 'theme.switchToLight' : 'theme.switchToDark')}
            onClick={() => setPreference(next)}
          >
            <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={20} />
          </button>
        </div>
      </header>
      <main className={styles.main}>
        <Outlet />
      </main>
    </>
  )
}

/** Wąski, wyśrodkowany układ dla ekranów logowania/rejestracji. */
export function CenteredLayout() {
  return (
    <div className={styles.auth}>
      <main className={styles.center}>
        <div className={styles.authBrand}>
          <Logo large />
        </div>
        <Outlet />
      </main>
    </div>
  )
}
