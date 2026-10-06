import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { authApi } from '../../api/auth'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { TextField } from '../../components/TextField'
import styles from './AuthPage.module.css'
import { forgotPasswordSchema, type ForgotPasswordForm } from './schemas'

export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const [sent, setSent] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordForm>({ resolver: zodResolver(forgotPasswordSchema) })

  const onSubmit = handleSubmit(async ({ email }) => {
    setServerError(null)
    try {
      await authApi.requestPasswordReset(email)
      setSent(true)
    } catch (e) {
      setServerError(errorMessage(t, e))
    }
  })

  return (
    <Card>
      <h1 className={styles.title}>{t('auth.forgot.title')}</h1>
      {sent ? (
        <Alert kind="success">{t('auth.forgot.sent')}</Alert>
      ) : (
        <>
          <p>{t('auth.forgot.intro')}</p>
          {serverError && <Alert kind="error">{serverError}</Alert>}
          <form onSubmit={onSubmit} noValidate>
            <TextField
              label={t('auth.email')}
              type="email"
              autoComplete="email"
              error={errors.email?.message}
              {...register('email')}
            />
            <Button type="submit" variant="primary" block disabled={isSubmitting}>
              {t('auth.forgot.submit')}
            </Button>
          </form>
        </>
      )}
      <div className={styles.links}>
        <Link to="/login">{t('auth.forgot.back')}</Link>
      </div>
    </Card>
  )
}
