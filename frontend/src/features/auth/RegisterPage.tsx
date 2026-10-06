import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import { useAuth } from '../../app/AuthProvider'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { TextField } from '../../components/TextField'
import styles from './AuthPage.module.css'
import { registerSchema, type RegisterForm } from './schemas'

export function RegisterPage() {
  const { t } = useTranslation()
  const { register: registerAccount } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) })

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      await registerAccount(values)
      navigate('/', { replace: true })
    } catch (e) {
      if (!applyFieldErrors(e, setError)) setServerError(errorMessage(t, e))
    }
  })

  return (
    <>
      <p className={styles.brand}>{t('app.name')}</p>
      <Card>
        <h1 className={styles.title}>{t('auth.register.title')}</h1>
        {serverError && <Alert kind="error">{serverError}</Alert>}
        <form onSubmit={onSubmit} noValidate>
          <TextField
            label={t('auth.email')}
            type="email"
            autoComplete="email"
            inputMode="email"
            error={errors.email?.message}
            {...register('email')}
          />
          <TextField
            label={t('auth.displayName')}
            autoComplete="nickname"
            error={errors.displayName?.message}
            {...register('displayName')}
          />
          <TextField
            label={t('auth.password')}
            type="password"
            autoComplete="new-password"
            hint={t('auth.register.passwordHint')}
            error={errors.password?.message}
            {...register('password')}
          />
          <Button type="submit" variant="primary" block disabled={isSubmitting}>
            {t('auth.register.submit')}
          </Button>
        </form>
        <div className={styles.links}>
          <span>
            {t('auth.register.hasAccount')} <Link to="/login">{t('auth.register.login')}</Link>
          </span>
        </div>
      </Card>
    </>
  )
}
