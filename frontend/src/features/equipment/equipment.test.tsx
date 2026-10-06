import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { EquipmentSummary } from '../../api/equipment'
import { EquipmentList } from './EquipmentList'
import { PhotoInput } from './PhotoInput'
import { equipmentSchema, toRequest, validatePhoto } from './schemas'

const item = (overrides: Partial<EquipmentSummary>): EquipmentSummary => ({
  id: 'e1',
  name: 'Wyciąg górny',
  category: 'CABLE',
  status: 'ACTIVE',
  openReportCount: 0,
  ...overrides,
})

describe('EquipmentList', () => {
  it('marks removed equipment and open reports', () => {
    render(
      <MemoryRouter>
        <EquipmentList
          items={[item({}), item({ id: 'e2', name: 'Suwnica Smitha', status: 'REMOVED_FROM_GYM', openReportCount: 2 })]}
          emptyText="pusto"
        />
      </MemoryRouter>,
    )
    expect(screen.getByText('Wyciąg górny')).toBeInTheDocument()
    expect(screen.getByText('Usunięty z siłowni')).toBeInTheDocument()
    expect(screen.getByText('zgłoszenia: 2')).toBeInTheDocument()
    expect(screen.getAllByRole('link')[1]).toHaveAttribute('href', '/equipment/e2')
  })

  it('shows empty text', () => {
    render(<EquipmentList items={[]} emptyText="Brak sprzętu" />)
    expect(screen.getByText('Brak sprzętu')).toBeInTheDocument()
  })
})

describe('equipment schemas', () => {
  it('treats empty quantity as missing and coerces numbers', () => {
    const empty = equipmentSchema.parse({ name: 'Hantle', category: 'FREE_WEIGHTS', quantity: '' })
    expect(toRequest(empty).quantity).toBeUndefined()
    const withQty = equipmentSchema.parse({ name: 'Hantle', category: 'FREE_WEIGHTS', quantity: '12' })
    expect(toRequest(withQty).quantity).toBe(12)
    expect(equipmentSchema.safeParse({ name: 'Hantle', category: 'FREE_WEIGHTS', quantity: '0' }).success).toBe(false)
  })

  it('validates photo type and size', () => {
    expect(validatePhoto(new File(['x'], 'a.gif', { type: 'image/gif' }))).toBe('errors.unsupported_file_type')
    const big = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'a.jpg', { type: 'image/jpeg' })
    expect(validatePhoto(big)).toBe('errors.file_too_large')
    expect(validatePhoto(new File(['x'], 'a.webp', { type: 'image/webp' }))).toBeNull()
  })
})

describe('PhotoInput', () => {
  it('rejects unsupported files', async () => {
    const onChange = vi.fn()
    URL.createObjectURL = vi.fn(() => 'blob:x')
    URL.revokeObjectURL = vi.fn()
    render(<PhotoInput file={null} onChange={onChange} />)
    const input = screen.getByLabelText('Zdjęcie (opcjonalnie)')
    await userEvent.upload(input, new File(['x'], 'doc.pdf', { type: 'application/pdf' }), { applyAccept: false })
    expect(screen.getByRole('alert')).toHaveTextContent('Dozwolone formaty: JPG, PNG, WebP.')
    expect(onChange).toHaveBeenCalledWith(null)
  })
})
