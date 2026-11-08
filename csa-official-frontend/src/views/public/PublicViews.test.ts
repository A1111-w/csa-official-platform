// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from './HomeView.vue'
import AboutView from './AboutView.vue'
import { publicService } from '@/services/public'

vi.mock('@/services/public', () => ({ publicService: { getAbout: vi.fn(), getContributors: vi.fn(), getCarousel: vi.fn(), getContributionRank: vi.fn() } }))
vi.mock('@/services/user', () => ({ userService: { getInfo: vi.fn() } }))
const wrappers: VueWrapper[] = []
beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(publicService.getAbout).mockResolvedValue('<p>欢迎加入 CSA</p>')
  vi.mocked(publicService.getContributors).mockResolvedValue([])
  vi.mocked(publicService.getCarousel).mockResolvedValue([])
  vi.mocked(publicService.getContributionRank).mockResolvedValue([])
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()) })
async function render(component: typeof HomeView | typeof AboutView) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push('/')
  const wrapper = mount(component, { global: { plugins: [createPinia(), router] } })
  wrappers.push(wrapper)
  await flushPromises()
  return wrapper
}
describe('Vue public pages', () => {
  it('keeps successful home content visible when another endpoint fails and offers retry', async () => {
    vi.mocked(publicService.getCarousel).mockRejectedValueOnce(new Error('offline'))
    const wrapper = await render(HomeView)
    expect(wrapper.text()).toContain('欢迎加入 CSA')
    expect(wrapper.get('[role="alert"]').text()).toContain('部分首页数据加载失败')
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(publicService.getCarousel).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('does not render executable carousel targets', async () => {
    vi.mocked(publicService.getCarousel).mockResolvedValue([{ id: 1, title: '协会活动', imgUrl: '/api/image', targetUrl: 'javascript:alert(1)' }])
    const wrapper = await render(HomeView)
    expect(wrapper.text()).toContain('协会活动')
    expect(wrapper.find('a[href^="javascript:"]').exists()).toBe(false)
    expect(wrapper.find('.carousel-caption a').exists()).toBe(false)
  })

  it('sanitizes rich HTML while preserving readable introduction text', async () => {
    vi.mocked(publicService.getAbout).mockResolvedValue('<p>协会介绍<script>alert(1)</script><a href="javascript:alert(1)">成员</a><img src=x onerror=alert(1)></p>')
    const wrapper = await render(AboutView)
    expect(wrapper.get('.prose').text()).toContain('协会介绍')
    expect(wrapper.find('.prose script').exists()).toBe(false)
    expect(wrapper.find('.prose img').exists()).toBe(false)
    expect(wrapper.get('.prose a').attributes('href')).toBeUndefined()
  })
})
