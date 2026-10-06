import i18n from './index'

export function formatDateTime(iso: string) {
  return new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso))
}

export function formatDate(iso: string) {
  return new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' }).format(new Date(iso))
}
