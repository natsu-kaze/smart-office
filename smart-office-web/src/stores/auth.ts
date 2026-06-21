import { defineStore } from 'pinia'
import { loginApi, logoutApi, meApi } from '@/api/auth'
import type { AuthUser, LoginRequest } from '@/types/api'
import { getStoredUser, getToken, removeStoredUser, removeToken, setStoredUser, setToken } from '@/utils/token'

interface AuthState {
  token: string | null
  user: AuthUser | null
  currentUserLoaded: boolean
}

function normalizeRole(role: string) {
  const normalized = role.trim().toUpperCase()
  return normalized.startsWith('ROLE_') ? normalized.slice(5) : normalized
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken(),
    user: getStoredUser<AuthUser>(),
    currentUserLoaded: false,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    isSuperAdmin: (state) => {
      const username = state.user?.username?.trim().toLowerCase()
      if (username === 'admin') return true
      return Boolean(state.user?.roles?.some((role) => {
        const normalized = normalizeRole(role)
        return normalized === 'ADMIN' || normalized === 'ADMINISTRATOR' || normalized === '系统管理员'
      }))
    },
    hasRole: (state) => (roleCode: string) =>
      Boolean(state.user?.roles?.some((role) => normalizeRole(role) === normalizeRole(roleCode))),
    hasPermission: (state) => (permission: string) => {
      const username = state.user?.username?.trim().toLowerCase()
      const isAdmin = username === 'admin' || Boolean(state.user?.roles?.some((role) => {
        const normalized = normalizeRole(role)
        return normalized === 'ADMIN' || normalized === 'ADMINISTRATOR' || normalized === '系统管理员'
      }))
      return isAdmin || Boolean(state.user?.permissions?.includes(permission))
    },
    canManageContent(): boolean {
      return this.isSuperAdmin || this.hasRole('MANAGER')
    },
  },
  actions: {
    async login(payload: LoginRequest) {
      const response = await loginApi(payload)
      this.token = response.token
      this.user = response.user
      this.currentUserLoaded = true
      setToken(response.token)
      setStoredUser(response.user)
    },
    async loadCurrentUser() {
      if (!this.token) return
      this.user = await meApi()
      this.currentUserLoaded = true
      setStoredUser(this.user)
    },
    async ensureCurrentUser() {
      if (!this.token || this.currentUserLoaded) return
      try {
        await this.loadCurrentUser()
      } catch {
        this.token = null
        this.user = null
        this.currentUserLoaded = false
        removeToken()
        removeStoredUser()
      }
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.token = null
        this.user = null
        this.currentUserLoaded = false
        removeToken()
        removeStoredUser()
      }
    },
  },
})
