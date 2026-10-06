import { z } from 'zod'
import { EQUIPMENT_CATEGORIES } from '../../api/equipment'

export const equipmentSchema = z.object({
  name: z.string().trim().min(2, { error: 'validation.length' }).max(120, { error: 'validation.length' }),
  category: z.enum(EQUIPMENT_CATEGORIES, { error: 'validation.required' }),
  equipmentTypeId: z.string().optional(),
  description: z.string().trim().max(2000, { error: 'validation.length' }).optional(),
  // Pole liczbowe z inputa: '' => brak wartości.
  quantity: z
    .union([z.literal(''), z.coerce.number().int({ error: 'validation.invalid' }).min(1, { error: 'validation.invalid' }).max(999, { error: 'validation.invalid' })])
    .optional(),
})
export type EquipmentFormInput = z.input<typeof equipmentSchema>
export type EquipmentForm = z.output<typeof equipmentSchema>

export function toRequest(values: EquipmentForm) {
  return {
    name: values.name,
    category: values.category,
    equipmentTypeId: values.equipmentTypeId || undefined,
    description: values.description || undefined,
    quantity: values.quantity === '' || values.quantity === undefined ? undefined : values.quantity,
  }
}

export const MAX_PHOTO_BYTES = 5 * 1024 * 1024
export const PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp']

/** Walidacja zdjęcia po stronie klienta (serwer i tak sprawdza sygnaturę pliku). Zwraca klucz i18n błędu. */
export function validatePhoto(file: File): string | null {
  if (!PHOTO_TYPES.includes(file.type)) return 'errors.unsupported_file_type'
  if (file.size > MAX_PHOTO_BYTES) return 'errors.file_too_large'
  return null
}
