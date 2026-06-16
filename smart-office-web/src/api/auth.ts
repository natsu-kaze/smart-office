import request from '@/utils/request'
import type { AuthUser, LoginRequest, LoginResponse } from '@/types/api'

export function loginApi(data: LoginRequest) {
  return request.post<LoginResponse>('/api/auth/login', data)
}

export function meApi() {
  return request.get<AuthUser>('/api/auth/me')
}

export function logoutApi() {
  return request.post<void>('/api/auth/logout')
}
