import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { useCreateEquipment, useSimilarEquipment, useUploadEquipmentPhoto, type EquipmentCategory } from '../../api/equipment'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { EquipmentFormFields } from './EquipmentFormFields'
import { EquipmentList } from './EquipmentList'
import { PhotoInput } from './PhotoInput'
import { equipmentSchema, toRequest, type EquipmentForm, type EquipmentFormInput } from './schemas'

export function AddEquipmentPage() {
  const { gymId = '' } = useParams()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const create = useCreateEquipment(gymId)
  const upload = useUploadEquipmentPhoto()
  const [photo, setPhoto] = useState<File | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EquipmentFormInput, unknown, EquipmentForm>({ resolver: zodResolver(equipmentSchema) })

  const name = useDebouncedValue(useWatch({ control, name: 'name' }) ?? '', 400)
  const category = useWatch({ control, name: 'category' }) as EquipmentCategory | undefined
  const similar = useSimilarEquipment(gymId, name)

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    let createdId: string | null = null
    try {
      createdId = (await create.mutateAsync(toRequest(values))).id
      if (photo) await upload.mutateAsync({ id: createdId, file: photo })
      navigate(`/equipment/${createdId}`, { replace: true })
    } catch (e) {
      if (createdId) {
        // Sprzęt zapisany, nie udało się tylko zdjęcie – przechodzimy do sprzętu, gdzie można spróbować ponownie.
        navigate(`/equipment/${createdId}`, { replace: true, state: { photoError: errorMessage(t, e) } })
      } else if (!applyFieldErrors(e, setError)) {
        setServerError(errorMessage(t, e))
      }
    }
  })

  return (
    <>
      <h1>{t('equipment.addTitle')}</h1>
      <Card>
        {serverError && <Alert kind="error">{serverError}</Alert>}
        <form onSubmit={onSubmit} noValidate>
          <EquipmentFormFields
            register={register}
            errors={errors}
            category={category || undefined}
            afterName={
              similar.data && similar.data.length > 0 ? (
                <Alert kind="warning">
                  <p>{t('equipment.similarHint')}</p>
                  <EquipmentList items={similar.data} emptyText="" />
                </Alert>
              ) : null
            }
          />
          <PhotoInput file={photo} onChange={setPhoto} />
          <Button type="submit" variant="primary" block disabled={isSubmitting}>
            {t('equipment.add')}
          </Button>
        </form>
      </Card>
    </>
  )
}
