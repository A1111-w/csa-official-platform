import api from "@/lib/axios"

export interface AiStatus {
  mode: "offline" | "live"
  model: string
  embeddingMode: string
}

export interface KnowledgeDocument {
  id: string
  title: string
  source: string
  minRoleLevel: number
  chunkCount: number
}

export interface DocumentInput {
  title: string
  source: string
  content: string
  minRoleLevel: number
}

export interface AgentRun {
  id: string
  question?: string
  answer: string
  mode: "offline" | "live"
  model: string
  citations: {
    documentId: string
    chunkId: string
    title: string
    source: string
    text: string
    score: number
  }[]
  steps: { name: string; status: string; durationMs: number; detail: string }[]
  durationMs: number
  promptTokens: number
  completionTokens: number
}

export interface EvaluationCase {
  question: string
  expectedDocumentIds: string[]
  expectedTools: string[]
  expectedAnswerTerms: string[]
}

export interface EvaluationReport {
  id: string
  mode: "offline" | "live" | "unknown" | "mixed"
  evaluatorVersion: string
  dataset: EvaluationCase[]
  total: number
  passed: number
  passRate: number
  meanReciprocalRank: number | null
  meanRecallAtK: number | null
  meanToolRecall: number | null
  meanAnswerTermCoverage: number | null
  averageLatencyMs: number
  results: {
    question: string
    runId: string | null
    passed: boolean
    recallAtK: number | null
    reciprocalRank: number | null
    toolRecall: number | null
    answerTermCoverage: number | null
    latencyMs: number
    error: string | null
  }[]
}

export function parseEvaluationCases(value: string): EvaluationCase[] {
  let parsed: unknown
  try {
    parsed = JSON.parse(value)
  } catch {
    throw new Error("数据集不是有效 JSON，请检查引号和逗号。")
  }
  if (!Array.isArray(parsed) || parsed.length < 1 || parsed.length > 5) {
    throw new Error("数据集必须是包含 1–5 个用例的数组。")
  }
  return parsed.map((entry: unknown, index) => {
    if (!entry || typeof entry !== "object" || Array.isArray(entry)) {
      throw new Error(`第 ${index + 1} 条用例必须是对象。`)
    }
    const row = entry as Record<string, unknown>
    if (typeof row.question !== "string" || !row.question.trim() || row.question.length > 2000) {
      throw new Error(`第 ${index + 1} 条问题须为 1–2000 个字符。`)
    }
    const expectations = ["expectedDocumentIds", "expectedTools", "expectedAnswerTerms"] as const
    for (const key of expectations) {
      if (!Array.isArray(row[key]) || row[key].length > 20 ||
        row[key].some((term: unknown) => typeof term !== "string" || !term.trim())) {
        throw new Error(`第 ${index + 1} 条 ${key} 须为不超过 20 项的非空字符串数组。`)
      }
    }
    const result = row as unknown as EvaluationCase
    if (result.expectedTools.length > 3 || result.expectedTools.some((name) =>
      !["search_knowledge", "search_resources", "search_competitions"].includes(name))) {
      throw new Error(`第 ${index + 1} 条工具须从三个可用检索工具中选择。`)
    }
    if (result.expectedDocumentIds.some((id) => id.length > 100) || result.expectedAnswerTerms.some((term) => term.length > 200)) {
      throw new Error(`第 ${index + 1} 条文档 ID 最长 100 字符，答案关键词最长 200 字符。`)
    }
    if (!expectations.some((key) => result[key].length > 0)) {
      throw new Error(`第 ${index + 1} 条至少需要一项期望文档、工具或答案关键词。`)
    }
    return {
      question: result.question.trim(),
      expectedDocumentIds: result.expectedDocumentIds,
      expectedTools: result.expectedTools,
      expectedAnswerTerms: result.expectedAnswerTerms,
    }
  })
}

export const aiService = {
  status: () => api.get<AiStatus, AiStatus>("/api/ai/status"),
  documents: () => api.get<KnowledgeDocument[], KnowledgeDocument[]>("/api/ai/documents"),
  addDocument: (data: DocumentInput) =>
    api.post<KnowledgeDocument, KnowledgeDocument>("/api/ai/documents", data, { timeout: 180_000 }),
  removeDocument: (id: string) => api.delete<void, void>(`/api/ai/documents/${encodeURIComponent(id)}`),
  run: (question: string) =>
    api.post<AgentRun, AgentRun>("/api/ai/runs", { question }, { timeout: 180_000 }),
  getRun: (id: string) => api.get<AgentRun, AgentRun>(`/api/ai/runs/${encodeURIComponent(id)}`),
  evaluate: (cases: EvaluationCase[]) =>
    api.post<EvaluationReport, EvaluationReport>("/api/ai/evaluations", { cases }, { timeout: 600_000 }),
  getEvaluation: (id: string) =>
    api.get<EvaluationReport, EvaluationReport>(`/api/ai/evaluations/${encodeURIComponent(id)}`),
}
