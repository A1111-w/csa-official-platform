// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import ResourcesView from './ResourcesView.vue'
import CompetitionsView from './CompetitionsView.vue'
import ResumeView from './ResumeView.vue'
import ResumeReviewsView from './ResumeReviewsView.vue'
import VoteView from './VoteView.vue'
import DepartmentsView from './DepartmentsView.vue'

const state = vi.hoisted(() => ({ user: { username: 'demo', roleLevel: 4 }, path: '/dashboard/resources' }))
const mocks = vi.hoisted(() => ({
  resources: { list: vi.fn(), listCategories: vi.fn(), save: vi.fn(), remove: vi.fn(), trackDownload: vi.fn() },
  competitions: { list: vi.fn(), listPublic: vi.fn(), detail: vi.fn(), detailPublic: vi.fn(), save: vi.fn(), grantEditor: vi.fn() },
  resumes: { getMyResume: vi.fn(), getGitSyncStatus: vi.fn(), save: vi.fn(), submit: vi.fn(), syncGitRepository: vi.fn(), listReviews: vi.fn(), reviewDetail: vi.fn(), audit: vi.fn() },
  votes: { list: vi.fn(), create: vi.fn(), submit: vi.fn() },
  depts: { list: vi.fn(), appoint: vi.fn() }, users: { list: vi.fn() }, notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}))
vi.mock('@/store/useAuthStore', () => ({ useAuthStore: () => state }))
vi.mock('vue-router', () => ({ useRoute: () => ({ path: state.path }) }))
vi.mock('@/services/resource', () => ({ resourceService: mocks.resources }))
vi.mock('@/services/competition', () => ({ competitionService: mocks.competitions }))
vi.mock('@/services/resume', () => ({ resumeService: mocks.resumes, RESUME_STATUS: { DRAFT: 0, PENDING: 1, APPROVED: 2, REJECTED: 3 }, isPendingResumeReview: (v: number) => v === 1 }))
vi.mock('@/services/vote', () => ({ voteService: mocks.votes }))
vi.mock('@/services/dept', () => ({ deptService: mocks.depts }))
vi.mock('@/services/user', () => ({ userService: mocks.users }))
vi.mock('@/services/file', () => ({ fileService: { upload: vi.fn() } }))
vi.mock('@/lib/notify', () => ({ notify: mocks.notify }))
vi.mock('@/lib/sanitize-html', () => ({ sanitizeHtml: (v: string) => v }))

const wrappers: VueWrapper[] = []
function render(component: Parameters<typeof mount>[0]) { const wrapper = mount(component, { global: { stubs: { RouterLink: true } } }); wrappers.push(wrapper); return wrapper }
function button(wrapper: VueWrapper, label: string) { const found = wrapper.findAll('button').find(b => b.text() === label); if (!found) throw new Error(`Missing button ${label}`); return found }
beforeEach(() => {
  vi.resetAllMocks(); state.user = { username: 'demo', roleLevel: 4 }; state.path = '/dashboard/resources'
  mocks.resources.list.mockResolvedValue({ records: [], pages: 1, total: 0 }); mocks.resources.listCategories.mockResolvedValue(['教材'])
  mocks.competitions.list.mockResolvedValue({ records: [], pages: 1, total: 0 }); mocks.competitions.listPublic.mockResolvedValue({ records: [], pages: 1, total: 0 })
  mocks.users.list.mockResolvedValue([]); mocks.votes.list.mockResolvedValue([]); mocks.depts.list.mockResolvedValue([])
  mocks.resumes.getMyResume.mockResolvedValue({ content: '', gitRepoUrl: '', status: 0 }); mocks.resumes.getGitSyncStatus.mockResolvedValue({ configured: true, status: 'NOT_SYNCED' })
  mocks.resumes.listReviews.mockResolvedValue({ records: [], pages: 1, total: 0 })
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
})
afterEach(() => { wrappers.splice(0).forEach(w => w.unmount()); vi.restoreAllMocks() })

describe('Vue business migration contracts', () => {
  it('does not load protected resources for a guest', async () => {
    state.user.roleLevel = 0
    const wrapper = render(ResourcesView); await flushPromises()
    expect(mocks.resources.list).not.toHaveBeenCalled(); expect(wrapper.text()).toContain('资源库需要会员身份')
  })
  it('uses server categories and eight-item pages while hiding public management', async () => {
    state.path = '/resources'; state.user.roleLevel = 1
    const wrapper = render(ResourcesView); await flushPromises()
    expect(mocks.resources.list).toHaveBeenCalledWith({ page: 1, size: 8, category: undefined })
    await wrapper.get('select').setValue('教材'); await flushPromises()
    expect(mocks.resources.list).toHaveBeenLastCalledWith({ page: 1, size: 8, category: '教材' })
    expect(wrapper.find('form').exists()).toBe(false)
  })
  it('loads complete competition content before editing and preserves unpublished status', async () => {
    state.path = '/dashboard/competitions'
    mocks.competitions.list.mockResolvedValue({ records: [{ id: 7, title: '比赛', summary: '截断摘要', status: 0, canEdit: true, canGrant: false }], pages: 1, total: 1 })
    mocks.competitions.detail.mockResolvedValue({ id: 7, title: '比赛', content: '完整的正文，不应被列表摘要覆盖', canEdit: true, status: 0, startTime: null, endTime: null, coverImg: null })
    const wrapper = render(CompetitionsView); await flushPromises()
    await button(wrapper, '编辑').trigger('click'); await flushPromises()
    expect(mocks.competitions.detail).toHaveBeenCalledWith(7)
    await wrapper.get('form').trigger('submit'); await flushPromises()
    expect(mocks.competitions.save).toHaveBeenCalledWith(expect.objectContaining({ id: 7, content: '完整的正文，不应被列表摘要覆盖', status: 0 }))
    expect(wrapper.text()).not.toContain('授权协作')
  })
  it('locks pending resumes against edits and submission', async () => {
    mocks.resumes.getMyResume.mockResolvedValue({ content: '待审核简历', gitRepoUrl: '', status: 1 })
    const wrapper = render(ResumeView); await flushPromises()
    expect(wrapper.get('textarea').attributes('disabled')).toBeDefined()
    expect(button(wrapper, '提交审核').attributes('disabled')).toBeDefined()
    await wrapper.get('form').trigger('submit'); expect(mocks.resumes.save).not.toHaveBeenCalled()
  })
  it.each([99, null, undefined])('does not turn unknown resume status %s into an editable draft', async unknownStatus => {
    mocks.resumes.getMyResume.mockResolvedValue({ content: '内容', gitRepoUrl: '', status: unknownStatus })
    const wrapper = render(ResumeView); await flushPromises()
    expect(wrapper.text()).toContain('简历状态未知')
    expect(button(wrapper, '提交审核').attributes('disabled')).toBeDefined()
    await wrapper.get('form').trigger('submit'); expect(mocks.resumes.save).not.toHaveBeenCalled()
  })
  it('rejects stale competition edit permission returned by the detail endpoint', async () => {
    state.path = '/dashboard/competitions'
    mocks.competitions.list.mockResolvedValue({ records: [{ id: 8, title: '比赛', status: 1, canEdit: true }], pages: 1, total: 1 })
    mocks.competitions.detail.mockResolvedValue({ id: 8, title: '比赛', content: '全文', status: 1, canEdit: false })
    const wrapper = render(CompetitionsView); await flushPromises()
    await button(wrapper, '编辑').trigger('click'); await flushPromises()
    expect(mocks.notify.error).toHaveBeenCalledWith('你已无权编辑此比赛，请刷新列表')
    expect(wrapper.find('textarea').element.value).toBe('')
  })
  it('validates HTTPS for submit as well as draft save and saves before submit', async () => {
    const wrapper = render(ResumeView); await flushPromises()
    await wrapper.get('textarea').setValue('项目经历'); await wrapper.get('input').setValue('http://example.test/repo')
    await button(wrapper, '提交审核').trigger('click'); await flushPromises()
    expect(mocks.resumes.save).not.toHaveBeenCalled()
    await wrapper.get('input').setValue('https://example.test/repo')
    await button(wrapper, '提交审核').trigger('click'); await flushPromises()
    expect(mocks.resumes.save).toHaveBeenCalledWith({ content: '项目经历', gitRepoUrl: 'https://example.test/repo' })
    expect(mocks.resumes.submit).toHaveBeenCalledOnce()
    expect(mocks.resumes.save.mock.invocationCallOrder[0]).toBeLessThan(mocks.resumes.submit.mock.invocationCallOrder[0]!)
  })
  it('requires a rejection reason and sends the selected resume ID', async () => {
    mocks.resumes.listReviews.mockResolvedValue({ records: [{ id: 12, username: 'member', status: 1 }], pages: 1, total: 1 })
    mocks.resumes.reviewDetail.mockResolvedValue({ id: 12, username: 'member', status: 1, content: '项目内容' })
    const wrapper = render(ResumeReviewsView); await flushPromises()
    await button(wrapper, '查看详情').trigger('click'); await flushPromises()
    await button(wrapper, '驳回并反馈').trigger('click'); expect(mocks.resumes.audit).not.toHaveBeenCalled()
    await wrapper.get('textarea').setValue('请补充你负责的模块')
    await button(wrapper, '驳回并反馈').trigger('click'); await flushPromises()
    expect(mocks.resumes.audit).toHaveBeenCalledWith({ resumeId: 12, pass: false, reason: '请补充你负责的模块' })
  })
  it('submits a vote once and disables another vote in the mounted session', async () => {
    mocks.votes.list.mockResolvedValue([{ id: 2, title: '活动', reason: '分享会', type: 'ACTIVITY', status: 0, expireTime: null }])
    const wrapper = render(VoteView); await flushPromises()
    await button(wrapper, '赞成').trigger('click'); await flushPromises()
    expect(mocks.votes.submit).toHaveBeenCalledWith({ proposalId: 2, agree: true, comment: undefined })
    expect(wrapper.text()).toContain('你的投票已提交'); expect(wrapper.findAll('button').some(b => b.text() === '赞成')).toBe(false)
  })
  it('blocks president and cross-department appointments but permits an eligible member', async () => {
    mocks.depts.list.mockResolvedValue([{ id: 1, name: '技术部', leaderId: null }])
    mocks.users.list.mockResolvedValue([{ id: 2, username: 'president', roleLevel: 4 }, { id: 3, username: 'leader', roleLevel: 3, departmentId: 2 }, { id: 4, username: 'member', roleLevel: 1 }])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = render(DepartmentsView); await flushPromises()
    expect(wrapper.text()).toContain('会长或 Root 不参与部长任命'); expect(wrapper.text()).toContain('已担任其他部门部长')
    expect(wrapper.findAll('button').filter(b => b.text() === '任命为部长')).toHaveLength(1)
    await button(wrapper, '任命为部长').trigger('click'); await flushPromises()
    expect(mocks.depts.appoint).toHaveBeenCalledWith(1, 4)
  })
})
