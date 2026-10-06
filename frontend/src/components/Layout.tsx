import { Link, NavLink, Outlet } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import styles from './Layout.module.css'

const navClass = ({ isActive }: { isActive: boolean }) => [styles.link, isActive && styles.active].filter(Boolean).join(' ')

export function Layout() {
  const { t } = useTranslation()
  return (
    <>
      <header className={styles.header}>
        <div className={styles.bar}>
          <Link to="/" className={styles.brand}>
            {t('app.name')}
          </Link>
          {/* Na telefonie nawigacja jest dolnym paskiem (fixed), od 768px – w nagłówku. */}
          <nav className={styles.nav} aria-label={t('nav.menu')}>
            <NavLink to="/" end className={navClass}>
              {t('nav.home')}
            </NavLink>
            <NavLink to="/gyms" className={navClass}>
              {t('nav.gyms')}
            </NavLink>
            <NavLink to="/plans" className={navClass}>
              {t('nav.plans')}
            </NavLink>
            <NavLink to="/history" className={navClass}>
              {t('nav.history')}
            </NavLink>
            <NavLink to="/profile" className={navClass}>
              {t('nav.profile')}
            </NavLink>
          </nav>
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
    <main className={styles.center}>
      <Outlet />
    </main>
  )
}
