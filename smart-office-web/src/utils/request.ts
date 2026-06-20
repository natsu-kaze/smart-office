import axios, { AxiosError } from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeStoredUser, removeToken } from '@/utils/token'
import type { Result } from '@/types/api'

interface RequestInstance extends AxiosInstance {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  ((response: AxiosResponse<Result>) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const body = response.data as Result
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 401) {
      removeToken()
      removeStoredUser()
      router.push('/login')
    }
    if (body.code === 403) {
      router.push('/403')
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  }) as never,
  (error: AxiosError<Result>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message
    if (status === 401) {
      removeToken()
      removeStoredUser()
      router.push('/login')
    }
    if (status === 403) {
      router.push('/403')
    }
    ElMessage.error(message || '网络异常')
    return Promise.reject(error)
  },
)

export default request as RequestInstance
