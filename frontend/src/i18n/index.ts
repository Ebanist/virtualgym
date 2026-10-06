import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import pl from './locales/pl/translation.json'

export const defaultLanguage = 'pl'

// Kolejne języki: dodaj plik locales/<lang>/translation.json i wpis w `resources`.
void i18n.use(initReactI18next).init({
  resources: { pl: { translation: pl } },
  lng: defaultLanguage,
  fallbackLng: defaultLanguage,
  interpolation: { escapeValue: false },
})

export default i18n
