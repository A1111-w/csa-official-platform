import { afterEach, describe, expect, it, vi } from "vitest"
import api from "@/lib/axios"
import { aiService, parseEvaluationCases } from "@/services/ai"

const validCase = { question: "Java 学习路径", expectedDocumentIds: ["doc-1"], expectedTools: [], expectedAnswerTerms: ["Spring"] }

describe("Agent evaluation dataset", () => {
  it("accepts partial annotations without manufacturing missing expectations", () => {
    expect(parseEvaluationCases(JSON.stringify([validCase]))).toEqual([validCase])
  })

  it.each(["invalid", "{}", "[]", JSON.stringify(Array(6).fill(validCase)),
    JSON.stringify([{ ...validCase, question: " " }]),
    JSON.stringify([{ ...validCase, expectedTools: "search_knowledge" }]),
    JSON.stringify([{ ...validCase, expectedDocumentIds: [], expectedAnswerTerms: [] }]),
    JSON.stringify([{ ...validCase, expectedAnswerTerms: [""] }]),
    JSON.stringify([{ ...validCase, expectedTools: Array(21).fill("search_knowledge") }]),
    JSON.stringify([{ ...validCase, expectedTools: ["delete_resource"] }]),
    JSON.stringify([{ ...validCase, expectedDocumentIds: ["x".repeat(101)] }]),
    JSON.stringify([{ ...validCase, expectedAnswerTerms: ["x".repeat(201)] }]),
  ])("rejects malformed, unannotated or unbounded datasets: %s", (input) => {
    expect(() => parseEvaluationCases(input)).toThrow()
  })
})

describe("Agent API", () => {
  afterEach(() => vi.restoreAllMocks())

  it("reuses the authenticated client and allows bounded long running calls", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue({ id: "report-1" })
    const get = vi.spyOn(api, "get").mockResolvedValue({ id: "run-1" })
    await aiService.run("Java 学习路径")
    await aiService.evaluate([validCase])
    await aiService.getRun("run/1")
    expect(post).toHaveBeenNthCalledWith(1, "/api/ai/runs", { question: "Java 学习路径" }, { timeout: 180_000 })
    expect(post).toHaveBeenNthCalledWith(2, "/api/ai/evaluations", { cases: [validCase] }, { timeout: 600_000 })
    expect(get).toHaveBeenCalledWith("/api/ai/runs/run%2F1")
  })

  it("does not retry a failed model request", async () => {
    const failure = new Error("Provider unavailable")
    const post = vi.spyOn(api, "post").mockRejectedValue(failure)
    await expect(aiService.run("question")).rejects.toThrow(failure)
    expect(post).toHaveBeenCalledTimes(1)
  })
})
