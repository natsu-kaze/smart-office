import { defineStore } from 'pinia'
import { loginApi, logoutApi, meApi } from '@/api/auth'
import type { AuthUser, LoginRequest } from '@/types/api'
import { getStoredUser, getToken, removeStoredUser, removeToken, setStoredUser, setToken } from '@/utils/token'

interface AuthState {
  token: string | null
  user: AuthUser | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken(),
    user: getStoredUser<AuthUser>(),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
  },
  actions: {
    async login(payload: LoginRequest) {
      const response = await loginApi(payload)
      this.token = response.token
      this.user = response.user
      setToken(response.token)
      setStoredUser(response.user)
    },
    async loadCurrentUser() {
      if (!this.token) return
      this.user = await meApi()
      setStoredUser(this.user)
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.token = null
        this.user = null
        removeToken()
        removeStoredUser()
      }
    },
  },
})
