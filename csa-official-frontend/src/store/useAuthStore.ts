import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { AuthUser, UserInfo } from '@/types/user'
import { userService } from '@/services/user'
import { ApiError } from '@/types/api'

// Roles and tokens are never persisted. Authorization starts with the server session.
export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const profile = ref<UserInfo | null>(null)
  const initialized = ref(false)
  let pending: Promise<UserInfo | null> | null = null
  let generation = 0

  function setUser(value: AuthUser | null) { user.value = value }
  function setLogin(value: AuthUser) {
    generation++
    pending = null
    user.value = value
    profile.value = null
    initialized.value = false
  }
  function logout() {
    generation++
    pending = null
    user.value = null
    profile.value = null
    initialized.value = true
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('token')
      localStorage.removeItem('csa-auth-storage')
    }
  }
  function refreshSession() {
    if (pending) return pending
    const requestGeneration = generation
    const request = userService.getInfo({ skipAuthRedirect: true })
      .then(value => {
        if (generation !== requestGeneration) return null
        profile.value = value
        user.value = { username: value.username, roleLevel: value.roleLevel }
        initialized.value = true
        return value
      })
      .catch(error => {
        if (generation !== requestGeneration) return null
        if (error instanceof ApiError && (error.status === 401 || error.code === 401)) {
          logout()
          return null
        }
        throw error
      })
      .finally(() => { if (pending === request) pending = null })
    pending = request
    return request
  }
  return { user, profile, initialized, setLogin, setUser, logout, refreshSession }
})
