const TOKEN_KEY = 'smart-office-token'
const USER_KEY = 'smart-office-user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoredUser<T>() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? (JSON.parse(raw) as T) : null
}

export function setStoredUser(value: unknown) {
  localStorage.setItem(USER_KEY, JSON.stringify(value))
}

export function removeStoredUser() {
  localStorage.removeItem(USER_KEY)
}
