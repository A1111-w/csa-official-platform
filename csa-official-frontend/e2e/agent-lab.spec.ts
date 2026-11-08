import { readFile } from 'node:fs/promises'
import { expect, test, type Page, type BrowserContext } from '@playwright/test'

test.setTimeout(90_000)

async function prepare(context: BrowserContext, page: Page, level = 3) {
  const origin = new URL(String(test.info().project.use.baseURL || 'http://127.0.0.1:3000')).origin
  await context.addCookies([{ name: 'CSA_AUTH_TOKEN', value: 'agent-ui-test', url: origin, httpOnly: true, sameSite: 'Lax' }])
  const writes: { path: string; csrf: string | undefined; body: unknown }[] = []
  const doc = { id: 'doc-1', title: 'Java 学习路径', source: '演示资料', minRoleLevel: 1, chunkCount: 1 }
  await page.route('**/api/**', async route => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (request.method() === 'POST') writes.push({ path, csrf: request.headers()['x-csrf-token'], body: request.postDataJSON() })
    let data: unknown
    if (path === '/api/sys/user/info') data = { id: 7, username: 'lab-user', roleLevel: level, realName: '实验室用户', avatar: null, email: null, phone: null, contact: null, positionType: null, balance: null, college: null, className: null, studentId: null }
    if (path === '/api/auth/csrf') data = { csrfToken: 'ui-test-csrf' }
    if (path === '/api/ai/status') data = { mode: 'offline', model: 'extractive', embeddingMode: 'bm25' }
    if (path === '/api/ai/documents') data = [doc]
    if (path === '/api/ai/runs') data = {
      id: 'run-1', question: 'Java 怎么学？', answer: '先学习 Java，再学习 Spring Boot。', mode: 'offline', model: 'extractive', durationMs: 30, promptTokens: 0, completionTokens: 0,
      citations: [{ documentId: 'doc-1', chunkId: 'chunk-1', title: 'Java 学习路径', source: '演示资料', text: '先学 Java，再学 Spring Boot。', score: 0.12 }],
      steps: [{ name: 'search_knowledge', status: 'success', durationMs: 20, detail: '检索到 1 条内容' }],
    }
    if (path === '/api/ai/evaluations') data = {
      id: 'eval-1', mode: 'offline', evaluatorVersion: 'rules-v1', dataset: request.postDataJSON().cases,
      total: 1, passed: 1, passRate: 1, meanReciprocalRank: 1, meanRecallAtK: 1, meanToolRecall: 1, meanAnswerTermCoverage: null, averageLatencyMs: 30,
      results: [{ question: '请介绍Java 学习路径', runId: 'run-1', passed: true, recallAtK: 1, reciprocalRank: 1, toolRecall: 1, answerTermCoverage: null, latencyMs: 30, error: null }],
    }
    await route.fulfill({ status: data === undefined ? 404 : 200, contentType: 'application/json', body: JSON.stringify({ code: data === undefined ? 404 : 200, message: data === undefined ? 'Unmocked API' : 'Success', data }) })
  })
  return writes
}

test('member can inspect a grounded answer without management controls; writes carry CSRF', async ({ context, page }) => {
  const writes = await prepare(context, page, 1)
  await page.goto('/dashboard/ai')
  await expect(page.getByRole('heading', { name: '让知识成为行动的起点' })).toBeVisible()
  await page.getByLabel('今天想了解什么？').fill('Java 怎么学？')
  await page.getByRole('button', { name: '发送问题' }).click()
  await expect(page.getByText('先学习 Java，再学习 Spring Boot。', { exact: true })).toBeVisible()
  expect(writes).toEqual([{ path: '/api/ai/runs', csrf: 'ui-test-csrf', body: { question: 'Java 怎么学？' } }])
  await expect(page.getByText('search_knowledge', { exact: true })).toBeVisible()
  await page.locator('#panel-ask summary').click()
  await expect(page.getByText('先学 Java，再学 Spring Boot。', { exact: true })).toBeVisible()
  await page.evaluate(() => window.scrollTo(0, 0))
  await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(0)
  await page.screenshot({ path: test.info().outputPath('agent-lab.png'), fullPage: true })
  await expect(page.getByRole('tab', { name: '评测' })).toHaveCount(0)
  await page.getByRole('tab', { name: '知识库' }).click()
  await expect(page.getByRole('button', { name: '加入知识库' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '删除 Java 学习路径' })).toHaveCount(0)
  expect(await page.evaluate(() => localStorage.getItem('csa-auth-storage'))).toBeNull()
})

test('minister validates JSON, evaluates a real document annotation, and downloads the N/A report', async ({ context, page }) => {
  const writes = await prepare(context, page)
  await page.goto('/dashboard/ai')
  await page.getByRole('tab', { name: '评测' }).click()
  const input = page.getByLabel('评测数据集 JSON')
  await input.fill('not json')
  await page.getByRole('button', { name: '运行评测' }).click()
  await expect(page.getByRole('alert')).toContainText('不是有效 JSON')
  await input.fill('[]')
  await page.getByRole('button', { name: '运行评测' }).click()
  await expect(page.getByRole('alert')).toContainText('1–5')
  expect(writes).toHaveLength(0)
  await page.getByRole('button', { name: '生成示例数据集' }).click()
  const submitted = JSON.parse(await input.inputValue())
  expect(submitted[0].expectedDocumentIds).toEqual(['doc-1'])
  await page.getByRole('button', { name: '运行评测' }).click()
  await expect(page.getByRole('heading', { name: '评测报告' })).toBeVisible()
  expect(writes).toEqual([{ path: '/api/ai/evaluations', csrf: 'ui-test-csrf', body: { cases: submitted } }])
  await expect(page.locator('#panel-evaluation .metrics').getByText('关键词覆盖率', { exact: true }).locator('..')).toContainText('N/A')
  await expect(page.locator('#panel-evaluation tbody')).toContainText('N/A')
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: '导出 JSON 报告' }).click()
  const download = await downloadPromise
  expect(download.suggestedFilename()).toBe('agent-evaluation-eval-1.json')
  const downloaded = JSON.parse(await readFile((await download.path())!, 'utf8'))
  expect(downloaded.dataset).toEqual(submitted)
  expect(downloaded.report.meanAnswerTermCoverage).toBeNull()
  expect(downloaded.report.evaluatorVersion).toBe('rules-v1')
})
