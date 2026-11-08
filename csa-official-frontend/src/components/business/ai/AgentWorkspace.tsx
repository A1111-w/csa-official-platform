"use client"

import { useCallback, useEffect, useState, type ChangeEvent, type FormEvent, type ReactNode } from "react"
import { Activity, ArrowUpRight, BookOpen, Bot, CheckCircle2, Download, FlaskConical, Loader2, Plus, RefreshCw, Search, Send, Trash2 } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useIsClient } from "@/hooks/use-is-client"
import { hasRoleLevel } from "@/lib/access"
import { aiService, parseEvaluationCases, type AgentRun, type AiStatus, type DocumentInput, type EvaluationReport, type KnowledgeDocument } from "@/services/ai"
import { useAuthStore } from "@/store/useAuthStore"

const panel = "rounded-2xl border bg-card p-5 shadow-sm md:p-6"
const emptyDraft: DocumentInput = { title: "", source: "", content: "", minRoleLevel: 1 }
const sampleDraft: DocumentInput = {
  title: "Java 学习路径（演示资料）",
  source: "人工编写的演示资料",
  minRoleLevel: 1,
  content: "Java 学习路径：先学习 Java 基础、集合与异常处理，再完成 Spring Boot REST API。数据库阶段学习 MySQL 索引与事务，缓存阶段学习 Redis。最后练习 JUnit 测试、权限控制与 Docker 部署。建议用协会资源管理项目串联这些知识。此文档为演示样例，不代表协会正式教学安排。",
}

function errorText(error: unknown) {
  return error instanceof Error ? error.message : "请求失败，请稍后重试。"
}

function percent(value: number | null) {
  return value == null ? "—" : `${(value * 100).toFixed(0)}%`
}

function Metric({ label, value }: { label: string; value: ReactNode }) {
  return <div className="rounded-xl border bg-background/70 px-4 py-3"><p className="text-xs text-muted-foreground">{label}</p><p className="mt-1 text-xl font-semibold tabular-nums">{value}</p></div>
}

function RunResult({ run }: { run: AgentRun }) {
  return <div className="space-y-5" aria-live="polite">
    <article className={panel}>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 font-semibold"><Bot className="size-4 text-primary" />回答</h3>
        <span className="text-xs text-muted-foreground">{run.mode === "offline" ? "离线摘录" : run.model} · {(run.durationMs / 1000).toFixed(2)} 秒</span>
      </div>
      {run.question && <p className="mb-4 rounded-lg bg-muted/40 p-3 text-sm text-muted-foreground">问题：{run.question}</p>}
      <p className="whitespace-pre-wrap break-words text-sm leading-7">{run.answer}</p>
      <p className="mt-5 break-all border-t pt-3 font-mono text-xs text-muted-foreground">Run {run.id}</p>
    </article>
    <div className="grid gap-5 xl:grid-cols-2">
      <section className={panel}>
        <h3 className="mb-4 flex items-center gap-2 font-semibold"><BookOpen className="size-4" />参考来源 <span className="text-muted-foreground">{run.citations.length}</span></h3>
        {!run.citations.length && <p className="text-sm text-muted-foreground">本次没有知识库引用。可先添加相关资料再提问。</p>}
        <div className="space-y-3">{run.citations.map((citation, index) => <details key={`${citation.documentId}-${citation.chunkId}-${index}`} className="rounded-xl border p-3">
          <summary className="cursor-pointer text-sm font-medium">[{index + 1}] {citation.title}</summary>
          <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-muted-foreground">{citation.text}</p>
          <p className="mt-3 break-all text-xs text-muted-foreground">{citation.source || "未标注来源"} · 分数 {citation.score.toFixed(4)}</p>
          <p className="mt-1 break-all font-mono text-[11px] text-muted-foreground">{citation.chunkId}</p>
        </details>)}</div>
      </section>
      <section className={panel}>
        <h3 className="mb-4 flex items-center gap-2 font-semibold"><Activity className="size-4" />执行轨迹</h3>
        <ol className="space-y-4">{run.steps.map((step, index) => <li key={`${step.name}-${index}`} className="flex gap-3">
          <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">{index + 1}</span>
          <div className="min-w-0 flex-1"><div className="flex flex-wrap justify-between gap-1 text-sm"><span className="break-all font-medium">{step.name}</span><span className="text-xs text-muted-foreground">{step.status} · {step.durationMs} ms</span></div><p className="mt-1 break-words text-xs leading-5 text-muted-foreground">{step.detail}</p></div>
        </li>)}</ol>
        <div className="mt-5 border-t pt-3 text-xs text-muted-foreground">Token 用量：输入 {run.promptTokens} / 输出 {run.completionTokens}（离线模式不调用模型）</div>
      </section>
    </div>
  </div>
}

export function AgentWorkspace() {
  const isClient = useIsClient()
  const user = useAuthStore((state) => state.user)
  const canUse = hasRoleLevel(user?.roleLevel, 1)
  const canManage = hasRoleLevel(user?.roleLevel, 3)
  const [status, setStatus] = useState<AiStatus | null>(null)
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState("")
  const [question, setQuestion] = useState("")
  const [run, setRun] = useState<AgentRun | null>(null)
  const [asking, setAsking] = useState(false)
  const [askError, setAskError] = useState("")
  const [draft, setDraft] = useState<DocumentInput>(emptyDraft)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [dataset, setDataset] = useState("[]")
  const [evaluating, setEvaluating] = useState(false)
  const [evaluationError, setEvaluationError] = useState("")
  const [report, setReport] = useState<EvaluationReport | null>(null)
  const [reportDataset, setReportDataset] = useState("")

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError("")
    try {
      const [nextStatus, nextDocuments] = await Promise.all([aiService.status(), aiService.documents()])
      setStatus(nextStatus)
      setDocuments(nextDocuments)
    } catch (error) {
      setLoadError(errorText(error))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isClient && canUse) void load()
  }, [isClient, canUse, load])

  async function ask(event: FormEvent) {
    event.preventDefault()
    if (!question.trim() || asking) return
    setAsking(true)
    setAskError("")
    setRun(null)
    try {
      setRun(await aiService.run(question.trim()))
    } catch (error) {
      setAskError(errorText(error))
    } finally {
      setAsking(false)
    }
  }

  async function saveDocument(event: FormEvent) {
    event.preventDefault()
    if (saving) return
    setSaving(true)
    try {
      await aiService.addDocument({ ...draft, title: draft.title.trim(), source: draft.source.trim(), content: draft.content.trim() })
      setDraft(emptyDraft)
      toast.success("资料已加入知识库")
      await load()
    } catch (error) {
      toast.error(errorText(error))
    } finally {
      setSaving(false)
    }
  }

  async function removeDocument(doc: KnowledgeDocument) {
    if (!window.confirm(`删除“${doc.title}”及其检索分片？`)) return
    setDeleting(doc.id)
    try {
      await aiService.removeDocument(doc.id)
      setDocuments((current) => current.filter((item) => item.id !== doc.id))
      toast.success("资料已删除")
    } catch (error) {
      toast.error(errorText(error))
    } finally {
      setDeleting(null)
    }
  }

  async function readFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""
    if (!file) return
    if (!/\.(md|txt)$/i.test(file.name) || file.size > 150_000) {
      toast.error("请选择 150 KB 以内的 Markdown 或 TXT 文件。")
      return
    }
    try {
      const content = await file.text()
      if (content.length > 30_000) throw new Error("文档不能超过 30,000 个字符。")
      setDraft((current) => ({ ...current, title: current.title || file.name, source: current.source || file.name, content }))
    } catch (error) {
      toast.error(errorText(error))
    }
  }

  function useExample() {
    const doc = documents[0]
    if (!doc) return
    setDataset(JSON.stringify([{ question: `请介绍${doc.title}`, expectedDocumentIds: [doc.id], expectedTools: ["search_knowledge"], expectedAnswerTerms: [] }], null, 2))
    setEvaluationError("")
  }

  async function evaluate() {
    if (evaluating) return
    setEvaluationError("")
    try {
      const cases = parseEvaluationCases(dataset)
      setEvaluating(true)
      setReport(null)
      const nextReport = await aiService.evaluate(cases)
      setReport(nextReport)
      setReportDataset(JSON.stringify(cases, null, 2))
    } catch (error) {
      setEvaluationError(errorText(error))
    } finally {
      setEvaluating(false)
    }
  }

  function downloadReport() {
    if (!report) return
    const blob = new Blob([JSON.stringify({ dataset: JSON.parse(reportDataset), report }, null, 2)], { type: "application/json" })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement("a")
    anchor.href = url
    anchor.download = `agent-evaluation-${report.id}.json`
    anchor.click()
    URL.revokeObjectURL(url)
  }

  if (!isClient) return <div className="h-64 animate-pulse rounded-2xl border bg-muted" aria-label="正在加载实验室" />
  if (!canUse) return <div className={panel}><h1 className="text-xl font-semibold">Agent 实验室</h1><p className="mt-2 text-muted-foreground">会员及以上可使用知识库问答。</p></div>

  return <div className="mx-auto max-w-7xl space-y-6">
    <header className="relative overflow-hidden rounded-2xl border bg-gradient-to-br from-primary/10 via-background to-cyan-500/5 p-6 md:p-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="mb-3 flex items-center gap-2 text-xs font-semibold tracking-[0.18em] text-primary"><FlaskConical className="size-4" />CSA · AGENT LAB</p><h1 className="text-3xl font-bold tracking-tight">让知识成为行动的起点</h1><p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">提问、检索、调用工具，再用评测验证效果。在同一个工作台观察每一次回答的来源与执行过程。</p></div>
        <Button variant="outline" size="sm" onClick={() => void load()} disabled={loading}><RefreshCw className={loading ? "animate-spin" : ""} />刷新</Button>
      </div>
      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        <Metric label="运行模式" value={status ? status.mode === "offline" ? "离线摘录" : "在线模型" : "待连接"} />
        <Metric label="可访问资料" value={loading ? "…" : documents.length} />
        <Metric label="检索方式" value={<span className="break-all text-base">{status?.embeddingMode || "待连接"}</span>} />
      </div>
      {status?.mode === "offline" && <p className="mt-4 text-xs leading-5 text-muted-foreground">当前使用真实知识库检索与原文摘录，不产生模型调用费用。在线模式由后端配置启用。</p>}
    </header>
    {loadError && <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">连接失败：{loadError}，请使用上方刷新按钮重试。</div>}
    <Tabs defaultValue="ask" className="gap-5">
      <TabsList className="h-11 w-full justify-start rounded-xl p-1 sm:w-fit">
        <TabsTrigger value="ask" className="px-5"><Bot />问答</TabsTrigger>
        <TabsTrigger value="knowledge" className="px-5"><BookOpen />知识库</TabsTrigger>
        {canManage && <TabsTrigger value="evaluation" className="px-5"><FlaskConical />评测</TabsTrigger>}
      </TabsList>
      <TabsContent value="ask" className="space-y-5">
        <form onSubmit={ask} className={panel}>
          <label htmlFor="agent-question" className="font-semibold">今天想了解什么？</label>
          <Textarea id="agent-question" value={question} onChange={(event) => setQuestion(event.target.value)} maxLength={2000} required disabled={asking} placeholder="例如：Java 后端应该怎么学？有哪些相关资料和比赛？" className="mt-4 min-h-28 resize-y border-0 bg-muted/40 p-4 text-base shadow-none" />
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3"><p className="text-xs text-muted-foreground">独立单轮问答 · {question.length}/2000</p><Button type="submit" disabled={asking || !question.trim()}>{asking ? <Loader2 className="animate-spin" /> : <Send />}{asking ? "正在检索与执行…" : "发送问题"}</Button></div>
          {!run && !asking && <div className="mt-4 flex flex-wrap gap-2">{["Java 学习路径有哪些步骤？", "有什么适合新人的比赛？", "查找 Spring Boot 学习资源"].map((item) => <button type="button" key={item} onClick={() => setQuestion(item)} className="rounded-full border px-3 py-1.5 text-xs text-muted-foreground transition hover:border-primary/40 hover:text-primary">{item}<ArrowUpRight className="ml-1 inline size-3" /></button>)}</div>}
        </form>
        {askError && <p role="alert" className="rounded-xl border border-destructive/30 p-4 text-sm text-destructive">{askError}</p>}
        {asking && <div role="status" className={`${panel} flex items-center gap-3 text-sm text-muted-foreground`}><Loader2 className="size-5 animate-spin" />正在执行，完成后展示回答、引用与轨迹。</div>}
        {run && <RunResult run={run} />}
        {!run && !asking && !askError && <div className="flex min-h-44 flex-col items-center justify-center rounded-2xl border border-dashed p-8 text-center"><Search className="mb-3 size-8 text-primary/50" /><p className="font-medium">从一个问题开始</p><p className="mt-2 text-sm text-muted-foreground">回答完成后，可以展开来源并检查每一步工具调用。</p></div>}
      </TabsContent>
      <TabsContent value="knowledge" className="grid items-start gap-5 xl:grid-cols-2">
        <section className={panel}><h2 className="text-lg font-semibold">知识资料</h2><p className="mt-1 text-sm text-muted-foreground">仅显示当前身份可以访问的内容。</p>
          {loading ? <p className="py-10 text-sm text-muted-foreground">正在加载资料…</p> : documents.length === 0 ? <p className="py-10 text-sm text-muted-foreground">知识库暂无资料。部长及以上可在此添加 Markdown 或纯文本。</p> : <ul className="mt-5 space-y-3">{documents.map((doc) => <li key={doc.id} className="rounded-xl border p-4"><div className="flex items-start justify-between gap-3"><h3 className="break-words font-medium">{doc.title}</h3>{canManage && <Button variant="ghost" size="icon" disabled={deleting !== null} onClick={() => void removeDocument(doc)} aria-label={`删除 ${doc.title}`}>{deleting === doc.id ? <Loader2 className="animate-spin" /> : <Trash2 className="text-muted-foreground" />}</Button>}</div><p className="mt-2 break-words text-xs text-muted-foreground">{doc.source || "未标注来源"} · {doc.chunkCount} 个分片 · Level {doc.minRoleLevel} 起</p><p className="mt-2 select-all break-all font-mono text-[11px] text-muted-foreground">文档 ID：{doc.id}</p></li>)}</ul>}
        </section>
        {canManage ? <form onSubmit={saveDocument} className={`${panel} space-y-4`}>
          <div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-lg font-semibold">添加资料</h2><Button type="button" size="sm" variant="outline" onClick={() => setDraft(sampleDraft)} disabled={saving}>填入演示资料</Button></div>
          <p className="text-xs leading-5 text-muted-foreground">保存后自动切片并建立检索索引。演示资料会明确标注来源。</p>
          <div className="space-y-2"><label htmlFor="doc-title" className="text-sm">标题</label><Input id="doc-title" required maxLength={200} value={draft.title} onChange={(event) => setDraft({ ...draft, title: event.target.value })} disabled={saving} /></div>
          <div className="space-y-2"><label htmlFor="doc-source" className="text-sm">来源标注</label><Input id="doc-source" required maxLength={500} value={draft.source} onChange={(event) => setDraft({ ...draft, source: event.target.value })} placeholder="例如：协会手册 / 文档地址" disabled={saving} /></div>
          <div className="space-y-2"><label htmlFor="doc-role" className="text-sm">最低可访问等级</label><select id="doc-role" value={draft.minRoleLevel} onChange={(event) => setDraft({ ...draft, minRoleLevel: Number(event.target.value) })} className="h-10 w-full rounded-md border bg-background px-3 text-sm" disabled={saving}>{[[1, "会员"], [2, "核心成员"], [3, "部长"], [4, "会长"], [99, "Root"]].filter(([level]) => Number(level) <= (user?.roleLevel ?? 0)).map(([level, label]) => <option key={level} value={level}>{label}及以上</option>)}</select></div>
          <div className="space-y-2"><label htmlFor="doc-file" className="text-sm">从本地文本文件填入</label><Input id="doc-file" type="file" accept=".md,.txt" onChange={(event) => void readFile(event)} disabled={saving} /></div>
          <div className="space-y-2"><label htmlFor="doc-content" className="text-sm">正文</label><Textarea id="doc-content" required maxLength={30000} value={draft.content} onChange={(event) => setDraft({ ...draft, content: event.target.value })} rows={8} disabled={saving} /><p className="text-right text-xs text-muted-foreground">{draft.content.length}/30000</p></div>
          <Button type="submit" disabled={saving || !draft.content.trim() || !draft.title.trim() || !draft.source.trim()}>{saving ? <Loader2 className="animate-spin" /> : <Plus />}{saving ? "正在建立索引…" : "加入知识库"}</Button>
        </form> : <div className={panel}><h2 className="font-semibold">资料维护</h2><p className="mt-2 text-sm text-muted-foreground">部长及以上可添加和删除资料。你可以在问答页检索现有知识。</p></div>}
      </TabsContent>
      {canManage && <TabsContent value="evaluation" className="space-y-5">
        <section className={panel}><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-lg font-semibold">让回答接受检验</h2><p className="mt-1 text-sm text-muted-foreground">为问题标注预期文档、工具和答案关键词，每批最多 5 条。</p></div><Button variant="outline" onClick={useExample} disabled={evaluating || documents.length === 0}>填入用例示例</Button></div>
          {documents.length === 0 && <p className="mt-3 text-sm text-muted-foreground">首次体验请先在“知识库”填入并保存演示资料，再生成带真实文档 ID 的评测用例。</p>}
          <label htmlFor="eval-dataset" className="mt-5 block text-sm font-medium">评测数据集（JSON 数组）</label><Textarea id="eval-dataset" value={dataset} onChange={(event) => setDataset(event.target.value)} disabled={evaluating} rows={12} spellCheck={false} className="mt-2 font-mono text-xs leading-6" />
          <p className="mt-3 text-xs leading-6 text-muted-foreground">文档 ID 可从知识库复制。工具名：search_knowledge、search_resources、search_competitions。没有标注的维度显示“—”，不参与该维度均值。答案关键词覆盖率是字符串匹配指标，不代表语义正确性或忠实度。在线评测会调用已配置的模型。</p>
          <div className="mt-4 flex flex-wrap items-center gap-3"><Button onClick={() => void evaluate()} disabled={evaluating}>{evaluating ? <Loader2 className="animate-spin" /> : <FlaskConical />}{evaluating ? "正在逐条评测…" : "运行评测"}</Button>{evaluating && <span role="status" className="text-xs text-muted-foreground">请保留此页面，完成后可导出报告。</span>}</div>
          {evaluationError && <p role="alert" className="mt-4 text-sm text-destructive">{evaluationError}</p>}
        </section>
        {report && <section className={`${panel} space-y-5`} aria-live="polite"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="flex items-center gap-2 text-lg font-semibold"><CheckCircle2 className="size-5 text-primary" />评测报告</h2><p className="mt-1 break-all font-mono text-xs text-muted-foreground">{report.id} · {report.mode}</p></div><Button variant="outline" onClick={downloadReport}><Download />导出 JSON</Button></div>
          <div className="grid gap-3 sm:grid-cols-3"><Metric label={`通过率（${report.passed}/${report.total}）`} value={percent(report.passRate)} /><Metric label="文档 Recall@K" value={percent(report.meanRecallAtK)} /><Metric label="MRR" value={report.meanReciprocalRank?.toFixed(3) ?? "—"} /><Metric label="工具召回率" value={percent(report.meanToolRecall)} /><Metric label="答案关键词覆盖率" value={percent(report.meanAnswerTermCoverage)} /><Metric label="平均耗时" value={`${(report.averageLatencyMs / 1000).toFixed(2)} s`} /></div>
          <div className="overflow-x-auto"><table className="w-full min-w-[650px] text-left text-sm"><thead className="border-b text-xs text-muted-foreground"><tr>{["问题 / Run", "结果", "Recall@K", "MRR", "工具", "关键词", "耗时"].map((label) => <th key={label} className="p-3 font-medium">{label}</th>)}</tr></thead><tbody>{report.results.map((result, index) => <tr key={index} className="border-b last:border-0"><td className="max-w-80 p-3"><p className="break-words">{result.question}</p><p className="mt-1 break-all font-mono text-[10px] text-muted-foreground">{result.runId || "无运行记录"}</p>{result.error && <p className="mt-1 text-xs text-destructive">{result.error}</p>}</td><td className="p-3">{result.passed ? "通过" : "未通过"}</td><td className="p-3">{percent(result.recallAtK)}</td><td className="p-3">{result.reciprocalRank?.toFixed(3) ?? "—"}</td><td className="p-3">{percent(result.toolRecall)}</td><td className="p-3">{percent(result.answerTermCoverage)}</td><td className="p-3">{result.latencyMs} ms</td></tr>)}</tbody></table></div>
        </section>}
      </TabsContent>}
    </Tabs>
  </div>
}
