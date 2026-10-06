import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { useNavigate, useParams } from 'react-router-dom'
import { useEquipment, useUpdateEquipment, type Equipment, type EquipmentCategory } from '../../api/equipment'
import { applyFieldErrors, errorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { EquipmentFormFields } from './EquipmentFormFields'
import { equipmentSchema, toRequest, type EquipmentForm, type EquipmentFormInput } from './schemas'

export function EditEquipmentPage() {
  const { id = '' } = useParams()
  const { t } = useTranslation()
  const equipment = useEquipment(id)
  if (equipment.isPending) return <p>{t('app.loading')}</p>
  if (equipment.isError) return <Alert kind="error">{errorMessage(t, equipment.error)}</Alert>
  return <EditForm equipment={equipment.data} />
}

function EditForm({ equipment: e }: { equipment: Equipment }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const update = useUpdateEquipment(e.id)
  const [serverError, setServerError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    control,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<EquipmentFormInput, unknown, EquipmentForm>({
    resolver: zodResolver(equipmentSchema),
    defaultValues: {
      name: e.name,
      category: e.category,
      equipmentTypeId: e.equipmentType?.id ?? '',
      description: e.description ?? '',
      quantity: e.quantity ?? '',
    },
  })
  const category = useWatch({ control, name: 'category' }) as EquipmentCategory | undefined

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null)
    try {
      await update.mutateAsync({ ...toRequest(values), status: e.status, version: e.version })
      navigate(`/equipment/${e.id}`, { replace: true })
    } catch (err) {
      if (!applyFieldErrors(err, setError)) setServerError(errorMessage(t, err))
    }
  })

  return (
    <>
      <h1>{t('equipment.editTitle')}</h1>
      <Card>
        {serverError && <Alert kind="error">{serverError}</Alert>}
        <form onSubmit={onSubmit} noValidate>
          <EquipmentFormFields register={register} errors={errors} category={category || undefined} />
          <Button type="submit" variant="primary" block disabled={isSubmitting}>
            {t('common.save')}
          </Button>
        </form>
      </Card>
    </>
  )
}
