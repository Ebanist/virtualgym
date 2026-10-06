import { keepPreviousData, useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import { unwrap } from './errors'
import type { components } from './schema'

export type EquipmentType = components['schemas']['EquipmentTypeDto']
export type EquipmentSummary = components['schemas']['EquipmentSummaryDto']
export type Equipment = components['schemas']['EquipmentDto']
export type EquipmentCategory = Equipment['category']
export type EquipmentStatus = Equipment['status']
export type CreateEquipmentRequest = components['schemas']['CreateEquipmentRequest']
export type UpdateEquipmentRequest = components['schemas']['UpdateEquipmentRequest']
export type EquipmentChange = components['schemas']['EquipmentChangeDto']
export type EquipmentReport = components['schemas']['EquipmentReportDto']
export type CreateReportRequest = components['schemas']['CreateReportRequest']
export type ReportType = EquipmentReport['type']

export const EQUIPMENT_CATEGORIES: EquipmentCategory[] = [
  'STRENGTH_MACHINE',
  'CABLE',
  'FREE_WEIGHTS',
  'BENCH',
  'CARDIO',
  'FUNCTIONAL',
  'OTHER',
]
export const REPORT_TYPES: ReportType[] = ['DUPLICATE', 'WRONG_DATA', 'REMOVED_FROM_GYM']
export const EQUIPMENT_PAGE_SIZE = 20

export const equipmentKeys = {
  all: ['equipment'] as const,
  types: ['equipment-types'] as const,
  list: (gymId: string, filters: object) => ['equipment', 'list', gymId, filters] as const,
  similar: (gymId: string, name: string) => ['equipment', 'similar', gymId, name] as const,
  detail: (id: string) => ['equipment', 'detail', id] as const,
  history: (id: string) => ['equipment', 'history', id] as const,
  reports: (id: string) => ['equipment', 'reports', id] as const,
}

export function useEquipmentTypes() {
  return useQuery({
    queryKey: equipmentKeys.types,
    queryFn: () => unwrap(api.GET('/api/v1/equipment-types')),
    staleTime: Infinity,
  })
}

export interface EquipmentFilters {
  q?: string
  category?: EquipmentCategory
  status?: EquipmentStatus
  page?: number
  size?: number
}

export function useEquipmentList(gymId: string, filters: EquipmentFilters) {
  return useQuery({
    queryKey: equipmentKeys.list(gymId, filters),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/gyms/{gymId}/equipment', {
          params: { path: { gymId }, query: { size: EQUIPMENT_PAGE_SIZE, ...filters } },
        }),
      ),
    placeholderData: keepPreviousData,
  })
}

export function useSimilarEquipment(gymId: string, name: string, enabled = true) {
  return useQuery({
    queryKey: equipmentKeys.similar(gymId, name),
    queryFn: () =>
      unwrap(api.GET('/api/v1/gyms/{gymId}/equipment/similar', { params: { path: { gymId }, query: { name } } })),
    enabled: enabled && name.trim().length >= 3,
  })
}

export function useEquipment(id: string) {
  return useQuery({
    queryKey: equipmentKeys.detail(id),
    queryFn: () => unwrap(api.GET('/api/v1/equipment/{id}', { params: { path: { id } } })),
  })
}

export function useCreateEquipment(gymId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateEquipmentRequest) =>
      unwrap(api.POST('/api/v1/gyms/{gymId}/equipment', { params: { path: { gymId } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: equipmentKeys.all }),
  })
}

export function useUpdateEquipment(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: UpdateEquipmentRequest) =>
      unwrap(api.PUT('/api/v1/equipment/{id}', { params: { path: { id } }, body })),
    onSuccess: (data) => {
      queryClient.setQueryData(equipmentKeys.detail(id), data)
      return queryClient.invalidateQueries({ queryKey: equipmentKeys.all })
    },
  })
}

export function useUploadEquipmentPhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, file }: { id: string; file: File }) =>
      unwrap(
        api.POST('/api/v1/equipment/{id}/photo', {
          params: { path: { id } },
          body: { file: file as unknown as string },
          bodySerializer: () => {
            const form = new FormData()
            form.append('file', file)
            return form
          },
        }),
      ),
    onSuccess: (data) => {
      queryClient.setQueryData(equipmentKeys.detail(data.id), data)
      return queryClient.invalidateQueries({ queryKey: equipmentKeys.all })
    },
  })
}

export function useDeleteEquipment(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => unwrap(api.DELETE('/api/v1/equipment/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: equipmentKeys.all }),
  })
}

export function useEquipmentHistory(id: string) {
  return useInfiniteQuery({
    queryKey: equipmentKeys.history(id),
    queryFn: ({ pageParam }) =>
      unwrap(api.GET('/api/v1/equipment/{id}/history', { params: { path: { id }, query: { page: pageParam, size: 10 } } })),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  })
}

export function useEquipmentReports(id: string) {
  return useQuery({
    queryKey: equipmentKeys.reports(id),
    queryFn: () => unwrap(api.GET('/api/v1/equipment/{id}/reports', { params: { path: { id } } })),
  })
}

export function useCreateReport(equipmentId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateReportRequest) =>
      unwrap(api.POST('/api/v1/equipment/{id}/reports', { params: { path: { id: equipmentId } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: equipmentKeys.all }),
  })
}

export function useResolveReport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (reportId: string) =>
      unwrap(api.POST('/api/v1/reports/{reportId}/resolve', { params: { path: { reportId } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: equipmentKeys.all }),
  })
}
