// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import AgentView from './AgentView.vue'
import { aiService, type EvaluationReport } from '@/services/ai'

const mocks = vi.hoisted(() => ({ user: { username: 'member', roleLevel: 1 }, notify: { error: vi.fn(), success: vi.fn(), info: vi.fn() } }))
vi.mock('@/store/useAuthStore', () => ({ useAuthStore: () => mocks }))
vi.mock('@/lib/notify', () => ({ notify: mocks.notify }))
vi.mock('@/services/ai', async importOriginal => ({
  ...await importOriginal<typeof import('@/services/ai')>(),
  aiService: { status: vi.fn(), documents: vi.fn(), run: vi.fn(), addDocument: vi.fn(), removeDocument: vi.fn(), evaluate: vi.fn() },
}))

const document = { id: 'doc-1', title: 'Java 学习路径', source: '测试资料', minRoleLevel: 1, chunkCount: 1 }
let wrapper: VueWrapper | undefined
beforeEach(() => {
  vi.clearAllMocks()
  mocks.user = { username: 'member', roleLevel: 1 }
  vi.mocked(aiService.status).mockResolvedValue({ mode: 'offline', model: 'extractive', embeddingMode: 'BM25' })
  vi.mocked(aiService.documents).mockResolvedValue([document])
})
afterEach(() => { wrapper?.unmount(); wrapper = undefined })

describe('Agent 实验室 Vue 迁移', () => {
  it('会员可问答，无法访问资料管理和评测控件', async () => {
    wrapper = mount(AgentView)
    await flushPromises()
    expect(wrapper.find('#tab-evaluation').exists()).toBe(false)
    await wrapper.get('#tab-knowledge').trigger('click')
    expect(wrapper.text()).toContain('Java 学习路径')
    expect(wrapper.find('#doc-title').exists()).toBe(false)
    expect(wrapper.find('[aria-label="删除 Java 学习路径"]').exists()).toBe(false)
  })

  it('通过同一个 API 提交问题并展示原文引用、工具轨迹和离线模式', async () => {
    vi.mocked(aiService.run).mockResolvedValue({
      id: 'run-1', question: '怎么学习 Java？', answer: '先学习基础。', mode: 'offline', model: 'extractive',
      citations: [{ documentId: 'doc-1', chunkId: 'chunk-1', title: 'Java 学习路径', source: '测试资料', text: '从 Java 基础开始。', score: 1.2 }],
      steps: [{ name: 'search_knowledge', status: 'success', durationMs: 3, detail: '命中 1 条' }],
      durationMs: 5, promptTokens: 0, completionTokens: 0,
    })
    wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.get('#agent-question').setValue('  怎么学习 Java？  ')
    await wrapper.get('#panel-ask form').trigger('submit')
    await flushPromises()
    expect(aiService.run).toHaveBeenCalledWith('怎么学习 Java？')
    expect(wrapper.text()).toContain('先学习基础。')
    expect(wrapper.get('#panel-ask details').text()).toContain('从 Java 基础开始。')
    expect(wrapper.text()).toContain('search_knowledge')
    expect(wrapper.text()).toContain('离线摘录')
  })

  it('请求失败展示错误，不留下上一次答案冒充本次结果', async () => {
    vi.mocked(aiService.run).mockRejectedValue(new Error('检索服务暂不可用'))
    wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.get('#agent-question').setValue('Java')
    await wrapper.get('#panel-ask form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('#panel-ask [role="alert"]').text()).toContain('检索服务暂不可用')
    expect(wrapper.find('#panel-ask article').exists()).toBe(false)
  })

  it('空知识库时不能生成无文档标注的演示评测', async () => {
    mocks.user.roleLevel = 3
    vi.mocked(aiService.documents).mockResolvedValue([])
    wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.get('#tab-evaluation').trigger('click')
    const example = wrapper.findAll('#panel-evaluation button').find(b => b.text() === '生成示例数据集')!
    expect(example.attributes('disabled')).toBeDefined()
    expect(wrapper.get('#panel-evaluation').text()).toContain('先到知识库添加演示资料')
  })

  it('部长可用真实文档 ID 生成数据集，未标注维度显示 N/A', async () => {
    mocks.user.roleLevel = 3
    const report: EvaluationReport = {
      id: 'report-1', mode: 'offline', evaluatorVersion: 'rules-v1', dataset: [], total: 1, passed: 1, passRate: 1,
      meanRecallAtK: 1, meanReciprocalRank: 1, meanToolRecall: 1, meanAnswerTermCoverage: null, averageLatencyMs: 4,
      results: [{ question: '请介绍Java 学习路径', runId: 'run-1', passed: true, recallAtK: 1, reciprocalRank: 1, toolRecall: 1, answerTermCoverage: null, latencyMs: 4, error: null }],
    }
    vi.mocked(aiService.evaluate).mockResolvedValue(report)
    wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.get('#tab-evaluation').trigger('click')
    await wrapper.findAll('#panel-evaluation button').find(b => b.text() === '生成示例数据集')!.trigger('click')
    const dataset = JSON.parse((wrapper.get('#evaluation-dataset').element as HTMLTextAreaElement).value)
    expect(dataset[0].expectedDocumentIds).toEqual(['doc-1'])
    await wrapper.get('#panel-evaluation form').trigger('submit')
    await flushPromises()
    expect(aiService.evaluate).toHaveBeenCalledWith(dataset)
    expect(wrapper.get('#panel-evaluation tbody').text()).toContain('N/A')
    expect(wrapper.get('#panel-evaluation').text()).toContain('导出 JSON 报告')
  })

  it('资料保存成功后加入列表，角色选项不能高于当前用户', async () => {
    mocks.user.roleLevel = 3
    vi.mocked(aiService.addDocument).mockResolvedValue({ ...document, id: 'doc-new', title: '新资料' })
    wrapper = mount(AgentView)
    await flushPromises()
    await wrapper.get('#tab-knowledge').trigger('click')
    expect(wrapper.findAll('#doc-role option').map(o => o.attributes('value'))).toEqual(['1', '2', '3'])
    await wrapper.get('#doc-title').setValue(' 新资料 ')
    await wrapper.get('#doc-source').setValue(' 来源 ')
    await wrapper.get('#doc-content').setValue(' 正文 ')
    await wrapper.get('#panel-knowledge form').trigger('submit')
    await flushPromises()
    expect(aiService.addDocument).toHaveBeenCalledWith({ title: '新资料', source: '来源', content: '正文', minRoleLevel: 1 })
    expect(wrapper.get('#panel-knowledge ul').text()).toContain('doc-new')
    expect((wrapper.get('#doc-title').element as HTMLInputElement).value).toBe('')
  })
})
