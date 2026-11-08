import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { sessionGuard } from './guard'
import { useAuthStore } from '@/store/useAuthStore'
import { pinia } from '@/store/pinia'
import { userService } from '@/services/user'
import { ApiError } from '@/types/api'
import type { UserInfo } from '@/types/user'
vi.mock('@/services/user', () => ({ userService: { getInfo: vi.fn() } }))
function makeRouter() {
  const component = { template: '<div />' }
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/login', component }, { path: '/forbidden', component }, { path: '/session-error', component },
    { path: '/privacy', component },
    { path: '/dashboard/ai', component, meta: { requiresAuth: true, minLevel: 1 } },
    { path: '/dashboard/audit', component, meta: { requiresAuth: true, minLevel: 4 } },
  ] })
  router.beforeEach(sessionGuard)
  return router
}
beforeEach(() => { vi.resetAllMocks(); useAuthStore(pinia).logout() })
describe('Vue route authorization', () => {
  it('keeps public pages accessible when the backend is offline', async () => {
    const router = makeRouter()
    await router.push('/privacy')
    expect(router.currentRoute.value.path).toBe('/privacy')
    expect(userService.getInfo).not.toHaveBeenCalled()
  })
  it('preserves a local deep-link for an expired session', async () => {
    vi.mocked(userService.getInfo).mockRejectedValue(new ApiError({ message: 'expired', status: 401 }))
    const router = makeRouter()
    await router.push('/dashboard/ai?tab=knowledge')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/dashboard/ai?tab=knowledge')
  })
  it('ignores a previously displayed privileged role and enforces the server role', async () => {
    useAuthStore(pinia).setLogin({ username: 'president', roleLevel: 4 })
    vi.mocked(userService.getInfo).mockResolvedValue({ username: 'member', roleLevel: 1 } as UserInfo)
    const router = makeRouter()
    await router.push('/dashboard/audit')
    expect(router.currentRoute.value.path).toBe('/forbidden')
    await router.push('/dashboard/ai')
    expect(router.currentRoute.value.path).toBe('/dashboard/ai')
  })
  it('does not enter a protected view after a network error', async () => {
    vi.mocked(userService.getInfo).mockRejectedValue(new Error('offline'))
    const router = makeRouter()
    await router.push('/dashboard/ai')
    expect(router.currentRoute.value.path).toBe('/session-error')
  })
})
