import request from '@/utils/request'
import type {
  AnnouncementPayload,
  AnnouncementSendResult,
  ApiId,
  MessageNotice,
  MessageTodo,
  PageResult,
} from '@/types/api'

export function getUnreadCount() {
  return request.get<number>('/api/messages/unread-count')
}

export function getTodoCount() {
  return request.get<number>('/api/messages/todos/count')
}

export function getMessages(params: Record<string, unknown>) {
  return request.get<PageResult<MessageNotice>>('/api/messages', { params })
}

export function getMessageTodos(params: Record<string, unknown>) {
  return request.get<PageResult<MessageTodo>>('/api/messages/todos', { params })
}

export function markMessageRead(id: ApiId) {
  return request.patch(`/api/messages/${id}/read`)
}

export function batchMarkMessagesRead(ids: ApiId[]) {
  return request.patch('/api/messages/read', { ids })
}

export function completeMessageTodo(id: ApiId) {
  return request.patch(`/api/messages/todos/${id}/complete`)
}

export function deleteMessage(id: ApiId) {
  return request.delete(`/api/messages/${id}`)
}

export function batchDeleteMessages(ids: ApiId[]) {
  return request.delete('/api/messages/batch', { data: { ids } })
}

export function publishAnnouncement(data: AnnouncementPayload) {
  return request.post<AnnouncementSendResult>('/api/messages/announcements', data)
}
