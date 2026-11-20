// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from './HomeView.vue'
import AboutView from './AboutView.vue'
import ContributorsView from './ContributorsView.vue'
import PrivacyView from './PrivacyView.vue'
import ResourcesView from '../business/ResourcesView.vue'
import { resourceService } from '@/services/resource'
import { useAuthStore } from '@/store/useAuthStore'
import { publicService } from '@/services/public'

vi.mock('@/services/public', () => ({ publicService: { getAbout: vi.fn(), getContributors: vi.fn(), getCarousel: vi.fn(), getContributionRank: vi.fn(), getPrivacyNotice: vi.fn() } }))
vi.mock('@/services/user', () => ({ userService: { getInfo: vi.fn() } }))
vi.mock('@/services/resource', () => ({ resourceService: { list: vi.fn(), listCategories: vi.fn() } }))
const wrappers: VueWrapper[] = []
beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(publicService.getAbout).mockResolvedValue('<p>欢迎加入 CSA</p>')
  vi.mocked(publicService.getContributors).mockResolvedValue([])
  vi.mocked(publicService.getCarousel).mockResolvedValue([])
  vi.mocked(publicService.getContributionRank).mockResolvedValue([])
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()) })
async function render(component: Parameters<typeof mount>[0]) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push('/')
  const wrapper = mount(component, { global: { plugins: [createPinia(), router] } })
  wrappers.push(wrapper)
  await flushPromises()
  return wrapper
}
describe('Vue public pages', () => {
  it('offers all four public exercises without requesting member resources, then keeps the guide for members', async () => {
    const wrapper = await render(ResourcesView)
    expect(wrapper.get('details.public-guide').attributes('open')).toBeDefined()
    const hosts = ['developer.mozilla.org', 'atcoder.jp', 'cn.vuejs.org', 'git-scm.com']
    const buttons = wrapper.findAll('.shelf-picker button')
    for (const [i, button] of buttons.entries()) {
      await button.trigger('click')
      expect(wrapper.findAll('.shelf-guide li')).toHaveLength(2)
      expect(new URL(wrapper.get('.shelf-guide a').attributes('href')!).host).toBe(hosts[i])
    }
    expect(resourceService.list).not.toHaveBeenCalled()
    vi.mocked(resourceService.list).mockResolvedValue({ records: [], total: 0, current: 1, pages: 1, size: 8 })
    vi.mocked(resourceService.listCategories).mockResolvedValue([])
    useAuthStore().setUser({ username: 'member', roleLevel: 1 })
    await flushPromises()
    expect(wrapper.get('details.public-guide').attributes('open')).toBeUndefined()
    expect(wrapper.findAll('.shelf-picker button')).toHaveLength(4)
    expect(resourceService.list).toHaveBeenCalledTimes(1)
  })

  it('only attaches a ranking to the matching member ID even for identical names', async () => {
    const member = { id: 1, realName: '同名成员', avatar: '', deptName: '开发组', title: '前端', roleLevel: 2 }
    vi.mocked(publicService.getContributors).mockResolvedValue([member, { ...member, id: 2 }])
    vi.mocked(publicService.getContributionRank).mockResolvedValue([
      { userId: 2, realName: member.realName, username: null, avatar: null, deptName: null, contributionCount: 4, score: '18.5' },
    ])
    const wrapper = await render(ContributorsView)
    const cards = wrapper.findAll('.member-card')
    expect(cards[0]!.find('details').exists()).toBe(false)
    expect(cards[1]!.get('details p').text()).toBe('公开榜第 1 名，累计 18.5 分，已记录 4 次贡献。')
  })
  it('combines department and role search independently of the contribution board', async () => {
    vi.mocked(publicService.getContributors).mockResolvedValue([
      { id: 1, realName: '小夏', avatar: '', deptName: '开发组', title: '前端', roleLevel: 2 },
      { id: 2, realName: '林同学', avatar: '', deptName: '活动组', title: '策划', roleLevel: 3 },
    ])
    vi.mocked(publicService.getContributionRank).mockResolvedValue([
      { userId: 9, realName: '榜单成员', username: null, avatar: null, deptName: '活动组', contributionCount: 3, score: '4.50' },
    ])
    const wrapper = await render(ContributorsView)
    await wrapper.get('input[type=search]').setValue('前端')
    expect(wrapper.get('.member-grid').text()).toContain('小夏')
    expect(wrapper.get('.member-grid').text()).not.toContain('林同学')
    await wrapper.findAll('.department-filter button').find(b => b.text().includes('活动组'))!.trigger('click')
    expect(wrapper.text()).toContain('找到 0 位成员')
    expect(wrapper.get('.leaderboard').text()).toContain('榜单成员')
    await wrapper.get('.result-count button').trigger('click')
    expect(wrapper.findAll('.member-card')).toHaveLength(2)
  })

  it('retries just the contribution board when the public directory succeeds', async () => {
    vi.mocked(publicService.getContributionRank).mockRejectedValueOnce(new Error('排名暂不可用'))
    const wrapper = await render(ContributorsView)
    await wrapper.get('.leaderboard button').trigger('click')
    await flushPromises()
    expect(publicService.getContributionRank).toHaveBeenCalledTimes(2)
    expect(publicService.getContributors).toHaveBeenCalledTimes(1)
    expect(wrapper.get('.leaderboard').text()).toContain('暂时还没有')
  })

  it('matches actions to the rights actually returned by the policy', async () => {
    vi.mocked(publicService.getPrivacyNotice).mockResolvedValue({ policyVersion: 'v1', collectedFields: [], purposes: {}, retention: {}, userRights: ['access', 'export', 'custom'], contactEmail: '' })
    const wrapper = await render(PrivacyView)
    expect(wrapper.get('.right-action a').attributes('href')).toBe('/dashboard/profile')
    await wrapper.findAll('.rights-picker button')[1]!.trigger('click')
    expect(wrapper.get('.right-action a').attributes('href')).toBe('/dashboard/settings')
    await wrapper.findAll('.rights-picker button')[2]!.trigger('click')
    expect(wrapper.get('.right-action a').attributes('href')).toBe('#policy-contact')
  })

  it('gives prospective competitors an activity route and project builders a member route', async () => {
    const wrapper = await render(AboutView)
    expect(wrapper.get('.path-detail a').attributes('href')).toBe('/competitions')
    await wrapper.findAll('.choices button')[1]!.trigger('click')
    expect(wrapper.get('.path-detail a').attributes('href')).toBe('/contributors')
  })
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
