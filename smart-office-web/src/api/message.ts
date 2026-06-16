import request from '@/utils/request'

export function getUnreadCount() {
  return request.get<number>('/api/messages/unread-count')
}

export function getTodoCount() {
  return request.get<number>('/api/messages/todos/count')
}
