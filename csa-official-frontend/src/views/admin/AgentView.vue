<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { aiService, parseEvaluationCases, type AgentRun, type AiStatus, type DocumentInput, type EvaluationReport, type KnowledgeDocument } from '@/services/ai'
import { useAuthStore } from '@/store/useAuthStore'
import { getRoleLabel } from '@/lib/access'
import { notify } from '@/lib/notify'
import { downloadJson, errorText } from './helpers'

const auth = useAuthStore()
const canUse = computed(() => (auth.user?.roleLevel ?? 0) >= 1)
const canManage = computed(() => (auth.user?.roleLevel ?? 0) >= 3)
const levels = computed(() => [1, 2, 3, 4, 99].filter(level => level <= (auth.user?.roleLevel ?? 0)))
const tab = ref<'ask' | 'knowledge' | 'evaluation'>('ask')
const status = ref<AiStatus | null>(null), documents = ref<KnowledgeDocument[]>([])
const loading = ref(false), loadError = ref('')
const question = ref(''), run = ref<AgentRun | null>(null), asking = ref(false), askError = ref('')
const draft = reactive<DocumentInput>({ title: '', source: '', content: '', minRoleLevel: 1 })
const saving = ref(false), deleting = ref<string | null>(null), readingFile = ref(false)
const dataset = ref('[]'), evaluating = ref(false), evaluationError = ref('')
const report = ref<EvaluationReport | null>(null)
const evaluatedDataset = ref('[]')
const sampleQuestions = ['Java 学习路径有哪些步骤？', '有什么适合新人的比赛？', '查找 Spring Boot 学习资源']
const percent = (value: number | null | undefined) => value == null ? 'N/A' : `${(value * 100).toFixed(0)}%`
const modeLabels: Record<string, string> = { offline: '离线摘录', live: '在线模型', unknown: '未知', mixed: '混合模式' }
const modeLabel = (mode: string) => modeLabels[mode] || mode
const reportMetrics = computed(() => report.value ? [
  ['严格通过率', percent(report.value.passRate)], ['文档召回率', percent(report.value.meanRecallAtK)],
  ['MRR', report.value.meanReciprocalRank == null ? 'N/A' : report.value.meanReciprocalRank.toFixed(3)],
  ['工具召回率', percent(report.value.meanToolRecall)], ['关键词覆盖率', percent(report.value.meanAnswerTermCoverage)],
  ['平均耗时', `${report.value.averageLatencyMs.toFixed(0)} ms`],
] : [])
async function load() {
  if (!canUse.value || loading.value) return
  loading.value = true; loadError.value = ''
  try {
    const [nextStatus, nextDocuments] = await Promise.all([aiService.status(), aiService.documents()])
    status.value = nextStatus; documents.value = nextDocuments
  } catch (e) { loadError.value = errorText(e) }
  finally { loading.value = false }
}
async function ask() {
  if (!canUse.value || asking.value || !question.value.trim()) return
  asking.value = true; askError.value = ''; run.value = null
  try { run.value = await aiService.run(question.value.trim()) }
  catch (e) { askError.value = errorText(e) }
  finally { asking.value = false }
}
function fillSample() {
  Object.assign(draft, {
    title: 'Java 学习路径（演示资料）', source: '人工编写的演示资料', minRoleLevel: 1,
    content: 'Java 学习路径：先学习 Java 基础、集合与异常处理，再完成 Spring Boot REST API。数据库阶段学习 MySQL 索引与事务，缓存阶段学习 Redis。最后练习 JUnit 测试、权限控制与 Docker 部署。建议用协会资源管理项目串联这些知识。此文档为演示样例，不代表协会正式教学安排。',
  })
}
async function saveDocument() {
  if (!canManage.value || saving.value || readingFile.value) return
  const payload = { ...draft, title: draft.title.trim(), source: draft.source.trim(), content: draft.content.trim() }
  if (!payload.title || !payload.source || !payload.content) { notify.error('请填写标题、来源和正文'); return }
  saving.value = true
  try {
    const added = await aiService.addDocument(payload)
    documents.value = [added, ...documents.value.filter(doc => doc.id !== added.id)]
    Object.assign(draft, { title: '', source: '', content: '', minRoleLevel: 1 })
    notify.success('资料已加入知识库')
  } catch (e) { notify.error(errorText(e)) }
  finally { saving.value = false }
}
async function removeDocument(doc: KnowledgeDocument) {
  if (!canManage.value || deleting.value || !window.confirm(`删除“${doc.title}”及其检索分片？`)) return
  deleting.value = doc.id
  try {
    await aiService.removeDocument(doc.id)
    documents.value = documents.value.filter(item => item.id !== doc.id)
    notify.success('资料已删除')
  } catch (e) { notify.error(errorText(e)) }
  finally { deleting.value = null }
}
async function readFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]; input.value = ''
  if (!file || saving.value || readingFile.value) return
  if (!/\.(md|txt)$/i.test(file.name) || file.size > 150000) { notify.error('请选择 150 KB 以内的 Markdown 或 TXT 文件。'); return }
  readingFile.value = true
  try {
    const content = await file.text()
    if (content.length > 30000) throw new Error('文档不能超过 30,000 个字符。')
    draft.title ||= file.name; draft.source ||= file.name; draft.content = content
  } catch (e) { notify.error(errorText(e)) }
  finally { readingFile.value = false }
}
function useExample() {
  const doc = documents.value[0]
  if (!doc) return
  dataset.value = JSON.stringify([{ question: `请介绍${doc.title}`, expectedDocumentIds: [doc.id], expectedTools: ['search_knowledge'], expectedAnswerTerms: [] }], null, 2)
  evaluationError.value = ''
}
async function evaluate() {
  if (!canManage.value || evaluating.value) return
  evaluationError.value = ''
  try {
    const cases = parseEvaluationCases(dataset.value)
    evaluating.value = true; report.value = null
    report.value = await aiService.evaluate(cases)
    evaluatedDataset.value = JSON.stringify(cases)
  } catch (e) { evaluationError.value = errorText(e) }
  finally { evaluating.value = false }
}
function downloadReport() {
  if (report.value) downloadJson({ dataset: JSON.parse(evaluatedDataset.value), report: report.value }, `agent-evaluation-${report.value.id}.json`)
}
function chooseTab(value: 'ask' | 'knowledge' | 'evaluation') { tab.value = value }
function tabKey(event: KeyboardEvent) {
  const tabs: ('ask' | 'knowledge' | 'evaluation')[] = canManage.value ? ['ask', 'knowledge', 'evaluation'] : ['ask', 'knowledge']
  const current = tabs.indexOf(tab.value)
  const next = event.key === 'ArrowRight' ? (current + 1) % tabs.length : event.key === 'ArrowLeft' ? (current - 1 + tabs.length) % tabs.length : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : -1
  if (next < 0) return
  event.preventDefault()
  tab.value = tabs[next]!
  document.getElementById(`tab-${tab.value}`)?.focus()
}
onMounted(load)
</script>

<template>
  <section class="page stack agent-lab">
    <header class="panel lab-header">
      <div class="toolbar"><div><p class="eyebrow">CSA · AGENT LAB</p><h1>让知识成为行动的起点</h1><p class="muted">提问、检索、调用工具，再用评测验证效果。观察每一次回答的来源与执行过程。</p></div><button class="secondary" :disabled="loading || !canUse" @click="load">刷新</button></div>
      <div class="metrics"><div><small>运行模式</small><strong>{{ status ? modeLabel(status.mode) : '待连接' }}</strong></div><div><small>可访问资料</small><strong>{{ loading ? '…' : documents.length }}</strong></div><div><small>检索方式</small><strong>{{ status?.embeddingMode || '待连接' }}</strong></div></div>
      <p v-if="status?.mode === 'offline'" class="muted">当前使用真实知识库检索与原文摘录，不产生模型调用费用。在线模式由后端配置启用。</p>
    </header>
    <p v-if="!canUse" class="panel">会员及以上可使用知识库问答。</p>
    <template v-else>
      <p v-if="loadError" class="error" role="alert">连接失败：{{ loadError }}，请使用上方刷新按钮重试。</p>
      <nav class="tabs" role="tablist" aria-label="Agent 实验室功能" @keydown="tabKey">
        <button id="tab-ask" role="tab" :aria-selected="tab === 'ask'" aria-controls="panel-ask" :tabindex="tab === 'ask' ? 0 : -1" @click="chooseTab('ask')">问答</button>
        <button id="tab-knowledge" role="tab" :aria-selected="tab === 'knowledge'" aria-controls="panel-knowledge" :tabindex="tab === 'knowledge' ? 0 : -1" @click="chooseTab('knowledge')">知识库</button>
        <button v-if="canManage" id="tab-evaluation" role="tab" :aria-selected="tab === 'evaluation'" aria-controls="panel-evaluation" :tabindex="tab === 'evaluation' ? 0 : -1" @click="chooseTab('evaluation')">评测</button>
      </nav>
      <section v-show="tab === 'ask'" id="panel-ask" role="tabpanel" aria-labelledby="tab-ask" class="stack">
        <form class="panel stack" @submit.prevent="ask"><label for="agent-question"><strong>今天想了解什么？</strong></label><textarea id="agent-question" v-model="question" maxlength="2000" rows="4" required :disabled="asking" placeholder="例如：Java 后端应该怎么学？有哪些相关资料和比赛？" /><div class="toolbar"><small class="muted">独立单轮问答 · {{ question.length }}/2000</small><button :disabled="asking || !question.trim()">{{ asking ? '正在检索与执行…' : '发送问题' }}</button></div><div v-if="!run && !asking" class="actions"><button v-for="item in sampleQuestions" :key="item" type="button" class="secondary" @click="question = item">{{ item }} ↗</button></div></form>
        <p v-if="askError" class="error" role="alert">{{ askError }}</p><p v-if="asking" class="panel" role="status">正在执行，完成后展示回答、引用与轨迹。</p>
        <div v-if="run" class="stack" aria-live="polite"><article class="panel"><div class="toolbar"><h2>回答</h2><small>{{ run.mode === 'offline' ? '离线摘录' : run.model }} · {{ (run.durationMs / 1000).toFixed(2) }} 秒</small></div><p v-if="run.question" class="muted">问题：{{ run.question }}</p><p class="answer">{{ run.answer }}</p><small class="break-all">Run {{ run.id }}</small></article>
          <div class="grid"><section class="panel stack"><h2>参考来源 <span class="muted">{{ run.citations.length }}</span></h2><p v-if="!run.citations.length" class="muted">本次没有知识库引用。可先添加相关资料再提问。</p><details v-for="(citation, index) in run.citations" :key="`${citation.chunkId}-${index}`"><summary>[{{ index + 1 }}] {{ citation.title }}</summary><p class="answer">{{ citation.text }}</p><p class="muted break-all">{{ citation.source || '未标注来源' }} · 分数 {{ citation.score.toFixed(4) }}</p><small class="break-all">{{ citation.chunkId }}</small></details></section>
            <section class="panel"><h2>执行轨迹</h2><ol class="trace"><li v-for="(step, index) in run.steps" :key="index"><div class="toolbar"><strong>{{ step.name }}</strong><small>{{ step.status }} · {{ step.durationMs }} ms</small></div><p class="muted">{{ step.detail }}</p></li></ol><p class="muted">Token 用量：输入 {{ run.promptTokens }} / 输出 {{ run.completionTokens }}（离线模式不调用模型）</p></section></div>
        </div>
        <div v-else-if="!asking && !askError" class="empty panel"><h2>从一个问题开始</h2><p>回答完成后，可以展开来源并检查每一步工具调用。</p></div>
      </section>
      <section v-show="tab === 'knowledge'" id="panel-knowledge" role="tabpanel" aria-labelledby="tab-knowledge" class="grid">
        <section class="panel stack"><h2>知识资料</h2><p class="muted">仅显示当前身份可以访问的内容。</p><p v-if="loading" role="status">正在加载资料…</p><p v-else-if="!documents.length" class="empty">知识库暂无资料。部长及以上可添加 Markdown 或纯文本。</p><ul v-else class="document-list stack"><li v-for="doc in documents" :key="doc.id"><div class="toolbar"><h3>{{ doc.title }}</h3><button v-if="canManage" class="danger" :disabled="deleting !== null || saving" :aria-label="`删除 ${doc.title}`" @click="removeDocument(doc)">{{ deleting === doc.id ? '删除中…' : '删除' }}</button></div><p class="muted">{{ doc.source || '未标注来源' }} · {{ doc.chunkCount }} 个分片 · Level {{ doc.minRoleLevel }} 起</p><small class="break-all">文档 ID：{{ doc.id }}</small></li></ul></section>
        <form v-if="canManage" class="panel stack" @submit.prevent="saveDocument"><div class="toolbar"><h2>添加资料</h2><button type="button" class="secondary" :disabled="saving || readingFile" @click="fillSample">填入演示资料</button></div><p class="muted">保存后自动切片并建立检索索引。演示资料明确标注来源。</p><fieldset class="stack" :disabled="saving || readingFile">
          <label class="field">标题<input id="doc-title" v-model="draft.title" required maxlength="200" /></label><label class="field">来源标注<input id="doc-source" v-model="draft.source" required maxlength="500" placeholder="例如：协会手册 / 文档地址" /></label><label class="field">最低可访问等级<select id="doc-role" v-model.number="draft.minRoleLevel"><option v-for="level in levels" :key="level" :value="level">{{ getRoleLabel(level) }}及以上</option></select></label><label class="field">从本地文本文件填入<input id="doc-file" type="file" accept=".md,.txt" @change="readFile" /></label><label class="field">正文<textarea id="doc-content" v-model="draft.content" required maxlength="30000" rows="8" /></label><small class="muted">{{ draft.content.length }}/30000</small><button :disabled="!draft.content.trim() || !draft.title.trim() || !draft.source.trim()">{{ saving ? '正在建立索引…' : '加入知识库' }}</button>
        </fieldset></form><div v-else class="panel"><h2>资料维护</h2><p class="muted">部长及以上可添加和删除资料。你可以在问答页检索现有知识。</p></div>
      </section>
      <section v-if="canManage" v-show="tab === 'evaluation'" id="panel-evaluation" role="tabpanel" aria-labelledby="tab-evaluation" class="stack">
        <form class="panel stack" @submit.prevent="evaluate"><div class="toolbar"><h2>让回答接受检验</h2><button type="button" class="secondary" :disabled="evaluating || !documents.length" @click="useExample">生成示例数据集</button></div><p>输入 1–5 条带标注的用例。每条至少填写一项期望文档、工具或答案关键词。</p><p v-if="!documents.length" class="muted">先到知识库添加演示资料，再生成带真实文档 ID 的示例。</p><label class="field">评测数据集 JSON<textarea id="evaluation-dataset" v-model="dataset" class="code-editor" rows="12" spellcheck="false" :disabled="evaluating" /></label><p class="muted">文档召回、MRR、工具召回和关键词覆盖均为规则指标，不代表语义正确性或真实性。未标注的维度显示 N/A，不计入均值。</p><button :disabled="evaluating">{{ evaluating ? '正在执行评测…' : '运行评测' }}</button></form>
        <p v-if="evaluationError" class="error" role="alert">{{ evaluationError }}</p><p v-if="evaluating" class="panel" role="status">正在逐条执行 Agent 并计算规则指标，请等待完成。</p>
        <section v-if="report" class="panel stack" aria-live="polite"><div class="toolbar"><div><h2>评测报告</h2><p class="muted">{{ report.passed }}/{{ report.total }} 条通过 · {{ modeLabel(report.mode) }} · {{ report.evaluatorVersion }}</p></div><button class="secondary" @click="downloadReport">导出 JSON 报告</button></div><div class="metrics"><div v-for="[label, value] in reportMetrics" :key="label"><small>{{ label }}</small><strong>{{ value }}</strong></div></div><div class="table-wrap"><table><thead><tr><th>问题 / 运行</th><th>结果</th><th>文档召回</th><th>MRR</th><th>工具召回</th><th>关键词覆盖</th><th>耗时</th></tr></thead><tbody><tr v-for="(result, index) in report.results" :key="index"><td>{{ result.question }}<small class="break-all">{{ result.runId || '未产生运行记录' }}</small><p v-if="result.error" class="error">{{ result.error }}</p></td><td>{{ result.passed ? '通过' : '未通过' }}</td><td>{{ percent(result.recallAtK) }}</td><td>{{ result.reciprocalRank == null ? 'N/A' : result.reciprocalRank.toFixed(3) }}</td><td>{{ percent(result.toolRecall) }}</td><td>{{ percent(result.answerTermCoverage) }}</td><td>{{ result.latencyMs }} ms</td></tr></tbody></table></div><small class="break-all">Report {{ report.id }}</small></section>
      </section>
    </template>
  </section>
</template>

<style scoped>
.lab-header { background: linear-gradient(120deg, var(--soft), var(--surface) 65%, var(--bg)); }
.eyebrow { color: var(--accent); letter-spacing: .16em; font-size: .75rem; font-weight: 700; }
.metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin: 20px 0; }
.metrics > div { border: 1px solid var(--border); background: var(--surface); padding: 14px; border-radius: 12px; }
.metrics small, .metrics strong { display: block; }.metrics strong { font-size: 1.25rem; margin-top: 6px; overflow-wrap: anywhere; }
.tabs { display: flex; flex-wrap: wrap; gap: 6px; }.tabs button { background: transparent; color: var(--text); border: 1px solid var(--border); }.tabs button[aria-selected="true"] { background: var(--accent); color: var(--surface); }
.answer { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.9; }.break-all { overflow-wrap: anywhere; }
details, .document-list li { border: 1px solid var(--border); border-radius: 12px; padding: 14px; } summary { cursor: pointer; font-weight: 600; }
.trace { padding-left: 22px; }.trace li { padding: 10px 0; }.document-list { list-style: none; padding: 0; }.code-editor { font-family: ui-monospace, monospace; } td small { display: block; margin-top: 8px; }
</style>
