import { useEffect, useId, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import fieldStyles from '../../components/Field.module.css'
import styles from './Equipment.module.css'
import { PHOTO_TYPES, validatePhoto } from './schemas'

interface Props {
  file: File | null
  onChange: (file: File | null) => void
  label?: string
}

/** Wybór zdjęcia z podglądem i walidacją po stronie klienta. Na telefonie pozwala zrobić zdjęcie aparatem. */
export function PhotoInput({ file, onChange, label }: Props) {
  const { t } = useTranslation()
  const id = useId()
  const [error, setError] = useState<string | null>(null)
  const preview = useMemo(() => (file ? URL.createObjectURL(file) : null), [file])

  useEffect(() => {
    return () => {
      if (preview) URL.revokeObjectURL(preview)
    }
  }, [preview])

  return (
    <div className={fieldStyles.field}>
      <label htmlFor={id} className={fieldStyles.label}>
        {label ?? t('equipment.photoOptional')}
      </label>
      <input
        id={id}
        type="file"
        accept={PHOTO_TYPES.join(',')}
        className={fieldStyles.input}
        onChange={(e) => {
          const selected = e.target.files?.[0] ?? null
          const problem = selected ? validatePhoto(selected) : null
          setError(problem)
          onChange(problem ? null : selected)
          if (problem) e.target.value = ''
        }}
      />
      <span className={fieldStyles.hint}>{t('equipment.photoHint')}</span>
      {error && (
        <span className={fieldStyles.error} role="alert">
          {t(error)}
        </span>
      )}
      {preview && <img src={preview} alt="" className={styles.photo} />}
    </div>
  )
}
