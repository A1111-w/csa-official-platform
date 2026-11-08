// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AuthView from './AuthView.vue'
import { authService } from '@/services/auth'
import { publicService } from '@/services/public'
import { useAuthStore } from '@/store/useAuthStore'

vi.mock('@/services/auth', () => ({ authService: { login: vi.fn(), register: vi.fn(), sendCode: vi.fn(), forgotPassword: vi.fn(), resetPassword: vi.fn() } }))
vi.mock('@/services/public', () => ({ publicService: { getPrivacyNotice: vi.fn() } }))
vi.mock('@/services/user', () => ({ userService: { getInfo: vi.fn() } }))
vi.mock('@/lib/notify', () => ({ notify: { success: vi.fn(), error: vi.fn() } }))

const mounted: VueWrapper[] = []
async function setup(path: string) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push(path)
  await router.isReady()
  const pinia = createPinia()
  const wrapper = mount(AuthView, { global: { plugins: [pinia, router] } })
  mounted.push(wrapper)
  await flushPromises()
  return { wrapper, router, auth: useAuthStore(pinia) }
}
async function fillRegistration(wrapper: VueWrapper) {
  const values = { username: 'member_1', email: 'member@example.test', code: '123456', password: 'abc12345', confirmPassword: 'abc12345' }
  for (const [name, value] of Object.entries(values)) await wrapper.get(`[name="${name}"]`).setValue(value)
}
beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(publicService.getPrivacyNotice).mockResolvedValue({ policyVersion: 'policy-2026-11', collectedFields: [], purposes: {}, retention: {}, userRights: [], contactEmail: 'privacy@example.test' })
})
afterEach(() => { mounted.splice(0).forEach(wrapper => wrapper.unmount()); vi.useRealTimers() })

describe('Vue authentication workflows', () => {
  it('requires privacy consent and submits the server policy version with the selected registration mode', async () => {
    const { wrapper, router } = await setup('/register')
    await fillRegistration(wrapper)
    await wrapper.get('form').trigger('submit')
    expect(authService.register).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请先阅读并同意隐私政策')
    await wrapper.get('[name="inviteCode"]').setValue('invite-test')
    await wrapper.get('[name="privacyConsent"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(authService.register).toHaveBeenCalledWith(expect.objectContaining({ privacyConsentVersion: 'policy-2026-11', inviteCode: 'invite-test', merchantNo: undefined }))
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('blocks registration when the policy cannot be loaded and allows retry', async () => {
    vi.mocked(publicService.getPrivacyNotice).mockRejectedValueOnce(new Error('policy offline'))
    const { wrapper } = await setup('/register')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    await fillRegistration(wrapper)
    await wrapper.get('form').trigger('submit')
    expect(authService.register).not.toHaveBeenCalled()
    const retry = wrapper.findAll('button').find(button => button.text() === '重试加载')!
    await retry.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('policy-2026-11')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('does not send stale invitation codes from ordinary registration', async () => {
    const { wrapper } = await setup('/register')
    await fillRegistration(wrapper)
    await wrapper.get('[name="inviteCode"]').setValue('stale-invite')
    await wrapper.get('[name="registerType"]').setValue('guest')
    await wrapper.get('[name="merchantNo"]').setValue('manual-reference')
    await wrapper.get('[name="privacyConsent"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(authService.register).toHaveBeenCalledWith(expect.objectContaining({ inviteCode: undefined, merchantNo: 'manual-reference' }))
  })

  it('logs in with cookies via the service and rejects an external redirect', async () => {
    vi.mocked(authService.login).mockResolvedValue({ username: 'member', roleLevel: 1 })
    const { wrapper, router, auth } = await setup('/login?redirect=https%3A%2F%2Fevil.example')
    await wrapper.get('[name="username"]').setValue('member')
    await wrapper.get('[name="password"]').setValue('abc12345')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(auth.user).toEqual({ username: 'member', roleLevel: 1 })
    expect(router.currentRoute.value.path).toBe('/dashboard')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('clears a rejected login password and leaves the user unauthenticated', async () => {
    vi.mocked(authService.login).mockRejectedValue(new Error('账号或密码错误'))
    const { wrapper, auth, router } = await setup('/login')
    await wrapper.get('[name="username"]').setValue('member')
    await wrapper.get('[name="password"]').setValue('wrong')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect((wrapper.get('[name="password"]').element as HTMLInputElement).value).toBe('')
    expect(auth.user).toBeNull()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('prevents double submission while a login is pending', async () => {
    let resolve!: (value: { username: string; roleLevel: number }) => void
    vi.mocked(authService.login).mockReturnValue(new Promise(done => { resolve = done }))
    const { wrapper } = await setup('/login')
    await wrapper.get('[name="username"]').setValue('member')
    await wrapper.get('[name="password"]').setValue('abc12345')
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')
    expect(authService.login).toHaveBeenCalledTimes(1)
    resolve({ username: 'member', roleLevel: 1 })
    await flushPromises()
  })

  it('validates reset password length and confirmation before calling the API', async () => {
    const { wrapper, router } = await setup('/reset-password')
    await wrapper.get('[name="email"]').setValue('member@example.test')
    await wrapper.get('[name="code"]').setValue('123456')
    await wrapper.get('[name="password"]').setValue('abc123')
    await wrapper.get('[name="confirmPassword"]').setValue('abc123')
    await wrapper.get('form').trigger('submit')
    expect(authService.resetPassword).not.toHaveBeenCalled()
    await wrapper.get('[name="password"]').setValue('abcdefgh1234')
    await wrapper.get('[name="confirmPassword"]').setValue('abcdefgh1234')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(authService.resetPassword).toHaveBeenCalledWith({ email: 'member@example.test', code: '123456', newPassword: 'abcdefgh1234' })
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('keeps forgot-password feedback neutral about account existence', async () => {
    const { wrapper } = await setup('/forgot-password')
    await wrapper.get('[name="email"]').setValue('member@example.test')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(authService.forgotPassword).toHaveBeenCalledWith({ email: 'member@example.test' })
    expect(wrapper.text()).toContain('如果账号存在')
  })
})
