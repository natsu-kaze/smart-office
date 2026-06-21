import request from '@/utils/request'
import type { AttendanceRecord, AttendanceSummary, AttendanceToday, PageResult } from '@/types/api'

export function getTodayAttendance() {
  return request.get<AttendanceToday>('/api/attendance/today')
}

export function checkIn(remark?: string) {
  return request.post<AttendanceToday>('/api/attendance/check-in', remark ? { remark } : undefined)
}

export function checkOut(remark?: string) {
  return request.post<AttendanceToday>('/api/attendance/check-out', remark ? { remark } : undefined)
}

export function getMyAttendanceRecords(params: Record<string, unknown>) {
  return request.get<PageResult<AttendanceRecord>>('/api/attendance/my-records', { params })
}

export function getDepartmentAttendanceRecords(params: Record<string, unknown>) {
  return request.get<PageResult<AttendanceRecord>>('/api/attendance/department-records', { params })
}

export function getMonthlyAttendanceSummary(month: string) {
  return request.get<AttendanceSummary>('/api/attendance/summary/monthly', { params: { month } })
}
