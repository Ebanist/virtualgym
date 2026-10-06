import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { profileApi } from '../../api/auth'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import { useAuth } from '../../app/AuthProvider'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { PageHeader } from '../../components/PageHeader'
import { Segmented } from '../../components/Segmented'
import { useTheme } from '../../app/ThemeProvider'
import type { ThemePreference } from '../../app/theme'
import { TextField } from '../../components/TextField'
import { changePasswordSchema, profileSchema, type ChangePasswordForm, type ProfileForm } from './schemas'

export function ProfilePage() {
  const { t } = useTranslation()
  return (
    <>
      <PageHeader title={t('profile.title')} />
      <Appearance />
      <ProfileDetails />
      <ChangePassword />
      <Logout />
    </>
  )
}

function ProfileDetails() {
  const { t } = useTranslation()
  const { user, setUser } = useAuth()
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: user?.displayName ?? '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setMessage(null)
    try {
      setUser(await profileApi.update(values))
      setMessage({ kind: 'success', text: t('common.saved') })
    } catch (e) {
      if (!applyFieldErrors(e, setError)) setMessage({ kind: 'error', text: errorMessage(t, e) })
    }
  })

  return (
    <Card>
      <h2>{t('profile.details')}</h2>
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      <form onSubmit={onSubmit} noValidate>
        <TextField label={t('auth.email')} value={user?.email ?? ''} readOnly disabled />
        <TextField label={t('auth.displayName')} error={errors.displayName?.message} {...register('displayName')} />
        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {t('common.save')}
        </Button>
      </form>
    </Card>
  )
}

function ChangePassword() {
  const { t } = useTranslation()
  const { applySession } = useAuth()
  const [message, setMessage] = useState<{ kind: 'success' | 'error'; text: string } | null>(null)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordForm>({ resolver: zodResolver(changePasswordSchema) })

  const onSubmit = handleSubmit(async ({ currentPassword, newPassword }) => {
    setMessage(null)
    try {
      applySession(await profileApi.changePassword({ currentPassword, newPassword }))
      reset()
      setMessage({ kind: 'success', text: t('profile.passwordChanged') })
    } catch (e) {
      setMessage({ kind: 'error', text: errorMessage(t, e) })
    }
  })

  return (
    <Card>
      <h2>{t('profile.changePassword')}</h2>
      {message && <Alert kind={message.kind}>{message.text}</Alert>}
      <form onSubmit={onSubmit} noValidate>
        <TextField
          label={t('profile.currentPassword')}
          type="password"
          autoComplete="current-password"
          error={errors.currentPassword?.message}
          {...register('currentPassword')}
        />
        <TextField
          label={t('profile.newPassword')}
          type="password"
          autoComplete="new-password"
          hint={t('auth.register.passwordHint')}
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />
        <TextField
          label={t('profile.confirmPassword')}
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {t('profile.changePassword')}
        </Button>
      </form>
    </Card>
  )
}

function Appearance() {
  const { t } = useTranslation()
  const { preference, setPreference } = useTheme()
  return (
    <Card>
      <h2>{t('theme.title')}</h2>
      <Segmented<ThemePreference>
        label={t('theme.label')}
        value={preference}
        options={[
          { value: 'system', label: t('theme.system'), icon: 'monitor' },
          { value: 'light', label: t('theme.light'), icon: 'sun' },
          { value: 'dark', label: t('theme.dark'), icon: 'moon' },
        ]}
        onChange={setPreference}
      />
    </Card>
  )
}

function Logout() {
  const { t } = useTranslation()
  const { logout } = useAuth()
  return (
    <Button variant="danger" block icon="logout" onClick={() => void logout()}>
      {t('nav.logout')}
    </Button>
  )
}
