import type { FieldErrors, UseFormRegister } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { EQUIPMENT_CATEGORIES, useEquipmentTypes, type EquipmentCategory } from '../../api/equipment'
import { SelectField } from '../../components/SelectField'
import { TextArea } from '../../components/TextArea'
import { TextField } from '../../components/TextField'
import type { EquipmentFormInput } from './schemas'

interface Props {
  register: UseFormRegister<EquipmentFormInput>
  errors: FieldErrors<EquipmentFormInput>
  category: EquipmentCategory | undefined
  /** Wstawiane pod polem nazwy (np. podpowiedzi podobnego sprzętu). */
  afterName?: React.ReactNode
}

export function EquipmentFormFields({ register, errors, category, afterName }: Props) {
  const { t } = useTranslation()
  const types = useEquipmentTypes()
  const typeOptions = (types.data ?? []).filter((type) => !category || type.category === category)
  return (
    <>
      <TextField label={t('equipment.name')} error={errors.name?.message} {...register('name')} />
      {afterName}
      <SelectField label={t('equipment.categoryLabel')} error={errors.category?.message} {...register('category')}>
        <option value="">{t('equipment.chooseCategory')}</option>
        {EQUIPMENT_CATEGORIES.map((c) => (
          <option key={c} value={c}>
            {t(`equipment.category.${c}`)}
          </option>
        ))}
      </SelectField>
      <SelectField label={t('equipment.type')} {...register('equipmentTypeId')}>
        <option value="">{t('equipment.noType')}</option>
        {typeOptions.map((type) => (
          <option key={type.id} value={type.id}>
            {type.name}
          </option>
        ))}
      </SelectField>
      <TextField
        label={t('equipment.quantity')}
        type="number"
        inputMode="numeric"
        min={1}
        error={errors.quantity?.message}
        {...register('quantity')}
      />
      <TextArea label={t('equipment.descriptionOptional')} error={errors.description?.message} {...register('description')} />
    </>
  )
}
