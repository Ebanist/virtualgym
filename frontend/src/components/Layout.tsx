import { NavLink, Outlet, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../app/AuthProvider'
import { Button } from './Button'
import styles from './Layout.module.css'

const navClass = ({ isActive }: { isActive: boolean }) => [styles.link, isActive && styles.active].filter(Boolean).join(' ')

export function Layout() {
  const { t } = useTranslation()
  const { logout } = useAuth()
  return (
    <>
      <header className={styles.header}>
        <div className={styles.bar}>
          <Link to="/" className={styles.brand}>
            {t('app.name')}
          </Link>
          <nav className={styles.nav} aria-label={t('nav.menu')}>
            <NavLink to="/" end className={navClass}>
              {t('nav.home')}
            </NavLink>
            <NavLink to="/profile" className={navClass}>
              {t('nav.profile')}
            </NavLink>
            <Button variant="ghost" small onClick={() => void logout()}>
              {t('nav.logout')}
            </Button>
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
