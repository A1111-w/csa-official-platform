// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia } from 'pinia'
import { useAuthStore } from '@/store/useAuthStore'
import StatusView from './StatusView.vue'

const mounted: VueWrapper[] = []
afterEach(() => mounted.splice(0).forEach(wrapper => wrapper.unmount()))

async function setup(path: string, kind: string) {
  const component = { template: '<div />' }
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/dashboard', component, meta: { requiresAuth: true, title: '概览' } },
    { path: '/dashboard/ai', component, meta: { requiresAuth: true, title: 'Agent 实验室' } },
    { path: '/:pathMatch(.*)*', component },
  ] })
  await router.push(path)
  const pinia = createPinia()
  const wrapper = mount(StatusView, { props: { kind }, global: { plugins: [pinia, router] } })
  mounted.push(wrapper)
  return { router, wrapper, auth: useAuthStore(pinia) }
}

describe('status page actions', () => {
  it('searches the public directory by purpose or path and clears empty results', async () => {
    const { wrapper } = await setup('/lost', 'missing')
    await wrapper.get('input[type="search"]').setValue('成员')
    expect(wrapper.findAll('.route-list a').map(link => link.attributes('href'))).toEqual(['/contributors'])
    await wrapper.get('input').setValue('RESOURCES')
    expect(wrapper.get('.route-list a').attributes('href')).toBe('/resources')
    await wrapper.get('input').setValue('not-a-route')
    await wrapper.get('.status-tools button').trigger('click')
    expect(wrapper.findAll('.route-list a')).toHaveLength(5)
  })

  it('shows workspace links only for the current signed-in role', async () => {
    const { wrapper, auth } = await setup('/forbidden', 'forbidden')
    expect(wrapper.get('.status-tools a').attributes('href')).toBe('/login')
    auth.setUser({ username: 'student', roleLevel: 1 })
    await flushPromises()
    expect(wrapper.get('.role-line').text()).toContain('会员')
    expect(wrapper.find('.route-list a[href="/dashboard/ai"]').exists()).toBe(true)
    expect(wrapper.find('[href="/dashboard/audit"]').exists()).toBe(false)
  })

  it('runs the session guard for retry, reports continued failure and falls back for external targets', async () => {
    const { wrapper, router } = await setup('/session-error?redirect=/dashboard/ai', 'session')
    const removeGuard = router.beforeEach(to => to.path === '/dashboard/ai' ? false : true)
    await wrapper.get('a[href="/dashboard/ai"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="status"]').text()).toContain('仍未能验证')
    removeGuard()
    await router.push('/session-error?redirect=https://invalid.example')
    expect(wrapper.get('.actions a').attributes('href')).toBe('/dashboard')
  })
})
