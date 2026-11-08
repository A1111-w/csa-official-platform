import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuthStore } from './useAuthStore'
import { createPinia } from 'pinia'
import { userService } from '@/services/user'
import { ApiError } from '@/types/api'
import type { UserInfo } from '@/types/user'
vi.mock('@/services/user', () => ({ userService: { getInfo: vi.fn() } }))
const member = { id: 1, username: 'member', roleLevel: 1 } as UserInfo
beforeEach(() => vi.resetAllMocks())
describe('server-backed Pinia session', () => {
  it('deduplicates session requests and refreshes authoritative role changes', async () => {
    const store = useAuthStore(createPinia())
    vi.mocked(userService.getInfo).mockResolvedValue(member)
    await Promise.all([store.refreshSession(), store.refreshSession()])
    expect(userService.getInfo).toHaveBeenCalledTimes(1)
    expect(store.user?.roleLevel).toBe(1)
    vi.mocked(userService.getInfo).mockResolvedValue({ ...member, roleLevel: 0 })
    await store.refreshSession()
    expect(store.user?.roleLevel).toBe(0)
  })
  it('discards an in-flight result after logout', async () => {
    const store = useAuthStore(createPinia())
    let resolve!: (user: UserInfo) => void
    vi.mocked(userService.getInfo).mockReturnValue(new Promise(r => { resolve = r }))
    const request = store.refreshSession()
    store.logout()
    resolve(member)
    await request
    expect(store.user).toBeNull()
    expect(store.profile).toBeNull()
  })
  it('distinguishes expiration from a network outage', async () => {
    const store = useAuthStore(createPinia())
    store.setLogin(member)
    vi.mocked(userService.getInfo).mockRejectedValue(new ApiError({ message: 'expired', status: 401 }))
    await expect(store.refreshSession()).resolves.toBeNull()
    expect(store.user).toBeNull()
    vi.mocked(userService.getInfo).mockRejectedValue(new Error('network'))
    await expect(store.refreshSession()).rejects.toThrow('network')
  })
  it('starts a fresh authoritative check after login even if an old check is pending', async () => {
    const store = useAuthStore(createPinia())
    let finishOld!: (user: UserInfo) => void
    vi.mocked(userService.getInfo).mockReturnValueOnce(new Promise(r => { finishOld = r }))
    const oldRequest = store.refreshSession()
    store.setLogin({ username: 'president', roleLevel: 4 })
    vi.mocked(userService.getInfo).mockResolvedValueOnce(member)
    await store.refreshSession()
    finishOld({ ...member, roleLevel: 4 })
    await oldRequest
    expect(userService.getInfo).toHaveBeenCalledTimes(2)
    expect(store.user?.roleLevel).toBe(1)
  })
})
