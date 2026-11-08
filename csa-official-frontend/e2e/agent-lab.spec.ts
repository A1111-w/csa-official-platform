import { expect, test, type Page, type BrowserContext } from "@playwright/test"

test.setTimeout(90_000)

async function prepare(context: BrowserContext, page: Page, level = 3) {
  await context.addCookies([{ name: "CSA_AUTH_TOKEN", value: "agent-ui-test", url: "http://127.0.0.1:3000", httpOnly: true, sameSite: "Lax" }])
  await page.addInitScript((roleLevel) => {
    localStorage.setItem("csa-auth-storage", JSON.stringify({ state: { user: { username: "lab-user", roleLevel } }, version: 3 }))
  }, level)
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname
    let data: unknown = {}
    if (path === "/api/sys/user/info") data = { username: "lab-user", roleLevel: level }
    if (path === "/api/auth/csrf") data = { csrfToken: "ui-test-csrf" }
    if (path === "/api/ai/status") data = { mode: "offline", model: "extractive", embeddingMode: "bm25" }
    if (path === "/api/ai/documents") data = [{ id: "doc-1", title: "Java 学习路径", source: "演示资料", minRoleLevel: 1, chunkCount: 1 }]
    if (path === "/api/ai/runs") data = { id: "run-1", answer: "先学习 Java，再学习 Spring Boot。", mode: "offline", model: "extractive", durationMs: 30, promptTokens: 0, completionTokens: 0, citations: [{ documentId: "doc-1", chunkId: "chunk-1", title: "Java 学习路径", source: "演示资料", text: "先学 Java，再学 Spring Boot。", score: 0.12 }], steps: [{ name: "search_knowledge", status: "success", durationMs: 20, detail: "检索到 1 条内容" }] }
    if (path === "/api/ai/evaluations") data = { id: "eval-1", mode: "offline", total: 1, passed: 1, passRate: 1, meanReciprocalRank: 1, meanRecallAtK: 1, meanToolRecall: 1, meanAnswerTermCoverage: null, averageLatencyMs: 30, results: [{ question: "请介绍Java 学习路径", runId: "run-1", passed: true, recallAtK: 1, reciprocalRank: 1, toolRecall: 1, answerTermCoverage: null, latencyMs: 30, error: null }] }
    await route.fulfill({ status: 200, contentType: "application/json", headers: { "access-control-allow-origin": "http://127.0.0.1:3000", "access-control-allow-credentials": "true", "access-control-allow-headers": "content-type,x-csrf-token", "access-control-allow-methods": "GET,POST,DELETE,OPTIONS" }, body: JSON.stringify({ code: 200, message: "Success", data }) })
  })
}

test("member can inspect a grounded answer and cannot access management controls", async ({ context, page }) => {
  await prepare(context, page, 1)
  await page.goto("/dashboard/ai")
  await expect(page.getByRole("heading", { name: "让知识成为行动的起点" })).toBeVisible()
  await page.getByLabel("今天想了解什么？").fill("Java 怎么学？")
  await page.getByRole("button", { name: "发送问题" }).click()
  await expect(page.getByText("先学习 Java，再学习 Spring Boot。", { exact: true })).toBeVisible()
  await expect(page.getByText("search_knowledge", { exact: true })).toBeVisible()
  await page.locator("summary").click()
  await expect(page.getByText("先学 Java，再学 Spring Boot。", { exact: true })).toBeVisible()
  await page.screenshot({ path: "test-results/agent-lab.png", fullPage: true })
  await expect(page.getByRole("tab", { name: "评测" })).toHaveCount(0)
  await page.getByRole("tab", { name: "知识库" }).click()
  await expect(page.getByRole("button", { name: "加入知识库" })).toHaveCount(0)
})

test("minister can evaluate annotated cases and export a report", async ({ context, page }) => {
  await prepare(context, page)
  await page.goto("/dashboard/ai")
  await page.getByRole("tab", { name: "评测" }).click()
  await page.getByRole("button", { name: "运行评测" }).click()
  await expect(page.getByRole("alert")).toContainText("1–5")
  await page.getByRole("button", { name: "填入用例示例" }).click()
  await page.getByRole("button", { name: "运行评测" }).click()
  await expect(page.getByRole("heading", { name: "评测报告" })).toBeVisible()
  const metric = page.getByText("答案关键词覆盖率", { exact: true }).locator("..")
  await expect(metric).toContainText("—")
  const download = page.waitForEvent("download")
  await page.getByRole("button", { name: "导出 JSON" }).click()
  expect((await download).suggestedFilename()).toBe("agent-evaluation-eval-1.json")
})
