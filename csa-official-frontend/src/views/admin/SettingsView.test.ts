// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import SettingsView from './SettingsView.vue'
import { accountService } from '@/services/account'
import { publicService } from '@/services/public'

const mocks = vi.hoisted(() => ({
  user: { username: 'chair', roleLevel: 4 }, logout: vi.fn(), replace: vi.fn(),
  notify: { error: vi.fn(), success: vi.fn(), info: vi.fn() },
}))
vi.mock('@/store/useAuthStore', () => ({ useAuthStore: () => mocks }))
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: mocks.replace }) }))
vi.mock('@/lib/notify', () => ({ notify: mocks.notify }))
vi.mock('@/services/account', () => ({ accountService: { changePassword: vi.fn(), revokeSessions: vi.fn(), deactivate: vi.fn(), requestDeletion: vi.fn(), exportData: vi.fn() } }))
vi.mock('@/services/public', () => ({ publicService: { getAbout: vi.fn(), updateAbout: vi.fn() } }))

let wrapper: VueWrapper | undefined
beforeEach(() => { vi.clearAllMocks(); vi.mocked(publicService.getAbout).mockResolvedValue('<p>协会介绍</p>') })
afterEach(() => { wrapper?.unmount(); wrapper = undefined; vi.restoreAllMocks() })

describe('账户与公开设置迁移边界', () => {
  it('公开介绍加载失败时禁用保存，避免空内容覆盖', async () => {
    vi.mocked(publicService.getAbout).mockRejectedValue(new Error('暂时无法加载'))
    wrapper = mount(SettingsView)
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('暂时无法加载')
    const save = wrapper.findAll('button').find(button => button.text() === '保存修改')!
    expect(save.attributes('disabled')).toBeDefined()
    await wrapper.findAll('form')[1]!.trigger('submit')
    expect(publicService.updateAbout).not.toHaveBeenCalled()
  })

  it('用户取消账号删除确认后不会发起删除或退出', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    wrapper = mount(SettingsView)
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '提交删除申请')!.trigger('click')
    expect(window.confirm).toHaveBeenCalled()
    expect(accountService.requestDeletion).not.toHaveBeenCalled()
    expect(mocks.logout).not.toHaveBeenCalled()
  })

  it('介绍预览清洗事件属性、脚本和危险链接', async () => {
    vi.mocked(publicService.getAbout).mockResolvedValue('<p onclick="alert(1)">正常内容</p><script>alert(1)</script><a href="javascript:alert(1)">链接</a>')
    wrapper = mount(SettingsView)
    await flushPromises()
    const html = wrapper.get('.rich-text').html()
    expect(html).toContain('正常内容')
    expect(html).not.toContain('onclick')
    expect(html).not.toContain('<script')
    expect(html).not.toContain('javascript:')
  })
})
