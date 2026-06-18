import request from '@/utils/request'
import type { ApiId, FileRecord, PageResult } from '@/types/api'

export function getMyFiles(params: Record<string, unknown>) {
  return request.get<PageResult<FileRecord>>('/api/files', { params })
}

export function uploadFile(file: File, businessType?: string, businessId?: ApiId) {
  const data = new FormData()
  data.append('file', file)
  if (businessType) {
    data.append('businessType', businessType)
  }
  if (businessId !== undefined) {
    data.append('businessId', String(businessId))
  }
  return request.post<FileRecord>('/api/files/upload', data)
}

export function getFilePreviewUrl(id: ApiId) {
  return request.get<string>(`/api/files/${id}/preview`)
}

export function downloadFile(id: ApiId) {
  return request.get<Blob>(`/api/files/${id}/download`, { responseType: 'blob' })
}
