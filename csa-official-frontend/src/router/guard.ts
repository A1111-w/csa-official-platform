import type { NavigationGuard } from 'vue-router'
import { useAuthStore } from '@/store/useAuthStore'
import { pinia } from '@/store/pinia'

export const sessionGuard: NavigationGuard = async to => {
  if (!to.meta.requiresAuth) return true
  const auth = useAuthStore(pinia)
  try {
    await auth.refreshSession()
  } catch {
    return { path: '/session-error', query: { redirect: to.fullPath } }
  }
  if (!auth.user) return { path: '/login', query: { redirect: to.fullPath } }
  if (auth.user.roleLevel < Number(to.meta.minLevel ?? 0)) return '/forbidden'
  return true
}
