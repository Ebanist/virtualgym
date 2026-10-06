import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../../api/auth'
import { errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { TextField } from '../../components/TextField'
import styles from './AuthPage.module.css'
import { resetPasswordSchema, type ResetPasswordForm } from './schemas'

export function ResetPasswordPage() {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const token = params.get('token')
  const [done, setDone] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordForm>({ resolver: zodResolver(resetPasswordSchema) })

  const onSubmit = handleSubmit(async ({ newPassword }) => {
    if (!token) return
    setServerError(null)
    try {
      await authApi.confirmPasswordReset(token, newPassword)
      setDone(true)
    } catch (e) {
      setServerError(errorMessage(t, e))
    }
  })

  return (
    <Card>
      <h1 className={styles.title}>{t('auth.reset.title')}</h1>
      {!token && <Alert kind="error">{t('auth.reset.missingToken')}</Alert>}
      {done && <Alert kind="success">{t('auth.reset.done')}</Alert>}
      {token && !done && (
        <>
          {serverError && <Alert kind="error">{serverError}</Alert>}
          <form onSubmit={onSubmit} noValidate>
            <TextField
              label={t('auth.reset.newPassword')}
              type="password"
              autoComplete="new-password"
              hint={t('auth.register.passwordHint')}
              error={errors.newPassword?.message}
              {...register('newPassword')}
            />
            <TextField
              label={t('auth.reset.confirmPassword')}
              type="password"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            <Button type="submit" variant="primary" block disabled={isSubmitting}>
              {t('auth.reset.submit')}
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
