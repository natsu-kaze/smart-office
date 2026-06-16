import request from '@/utils/request'
import type { AttendanceToday } from '@/types/api'

export function getTodayAttendance() {
  return request.get<AttendanceToday>('/api/attendance/today')
}

export function checkIn() {
  return request.post<AttendanceToday>('/api/attendance/check-in')
}

export function checkOut() {
  return request.post<AttendanceToday>('/api/attendance/check-out')
}
