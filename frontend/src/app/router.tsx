import { createBrowserRouter } from 'react-router-dom'
import { CenteredLayout, Layout } from '../components/Layout'
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { ResetPasswordPage } from '../features/auth/ResetPasswordPage'
import { HomePage } from '../features/home/HomePage'
import { NotFoundPage } from '../features/home/NotFoundPage'
import { ProfilePage } from '../features/profile/ProfilePage'
import { PublicOnly, RequireAuth } from './RouteGuards'

export const router = createBrowserRouter([
  {
    element: <PublicOnly />,
    children: [
      {
        element: <CenteredLayout />,
        children: [
          { path: '/login', element: <LoginPage /> },
          { path: '/register', element: <RegisterPage /> },
          { path: '/forgot-password', element: <ForgotPasswordPage /> },
        ],
      },
    ],
  },
  {
    // Reset hasła dostępny niezależnie od stanu zalogowania (link z e-maila).
    element: <CenteredLayout />,
    children: [{ path: '/reset-password', element: <ResetPasswordPage /> }],
  },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <Layout />,
        children: [
          { path: '/', element: <HomePage /> },
          { path: '/profile', element: <ProfilePage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
