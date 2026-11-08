import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

const username = process.env.E2E_USERNAME
const password = process.env.E2E_PASSWORD
const hasCredentials = Boolean(username && password)
const apiBaseURL = process.env.E2E_API_BASE_URL || process.env.E2E_BASE_URL || 'http://127.0.0.1:8080'
type ApiEnvelope<T> = { data?: T; code?: number; message?: string }
async function login(context: APIRequestContext) {
  const csrfResponse = await context.get('/api/auth/csrf')
  expect(csrfResponse.ok()).toBeTruthy()
  const csrf = (await csrfResponse.json()) as ApiEnvelope<{ csrfToken: string }>
  const response = await context.post('/api/auth/login', { data: { username, password } })
  expect(response.status()).toBe(200)
  const payload = (await response.json()) as ApiEnvelope<{ csrfToken: string }>
  const token = payload.data?.csrfToken || csrf.data?.csrfToken
  if (!token) throw new Error('Missing CSRF token')
  return token
}
async function mockSession(page: Page, roleLevel: number | null) {
  await page.route('**/api/sys/user/info', route => route.fulfill({
    status: roleLevel === null ? 401 : 200, contentType: 'application/json',
    body: JSON.stringify(roleLevel === null ? { code: 401, message: '请登录' } : { code: 200, data: { id: 1, username: 'fixture-user', roleLevel, realName: '演示成员' } }),
  }))
}
test('public privacy page renders the server policy and stays accessible without a session', async ({ page }) => {
  await mockSession(page, null)
  await page.route('**/api/public/privacy', route => route.fulfill({ json: { code: 200, data: {
    policyVersion: 'fixture-2026', collectedFields: ['用户名'], purposes: { account: '提供账号服务' },
    retention: { account: '按保留策略处理' }, userRights: ['导出个人数据'], contactEmail: 'privacy@example.invalid',
  } } }))
  await page.goto('/privacy')
  await expect(page.getByRole('heading', { name: '隐私说明' })).toBeVisible()
  await expect(page.getByText('当前版本：fixture-2026')).toBeVisible()
})
test('dashboard rejects a stale privileged localStorage role and preserves the deep link', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('csa-auth-storage', JSON.stringify({ state: { user: { username: 'stale-root', roleLevel: 99 } } })))
  await mockSession(page, null)
  await page.goto('/dashboard/ai?tab=knowledge')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  expect(new URL(page.url()).searchParams.get('redirect')).toBe('/dashboard/ai?tab=knowledge')
  await expect(page.getByRole('button', { name: '发送问题' })).toHaveCount(0)
})
test('member cannot navigate directly to an admin page', async ({ page }) => {
  await mockSession(page, 1)
  await page.goto('/dashboard/audit')
  await expect(page).toHaveURL(/\/forbidden$/)
  await expect(page.getByRole('heading', { name: '你暂时没有访问权限' })).toBeVisible()
})
test('session outage offers a retry instead of showing protected content', async ({ page }) => {
  await page.route('**/api/sys/user/info', route => route.fulfill({ status: 503, json: { code: 503, message: 'fixture outage' } }))
  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/session-error\?redirect=/)
  await expect(page.getByRole('heading', { name: '暂时无法验证会话' })).toBeVisible()
  await mockSession(page, 1)
  await page.getByRole('link', { name: '重新验证' }).click()
  await expect(page.getByRole('heading', { name: '控制台概览' })).toBeVisible()
})
test('president can load a carousel for editing with its original values', async ({ page }) => {
  await mockSession(page, 4)
  await page.route('**/api/sys/carousel/list', route => route.fulfill({ json: { code: 200, data: [{
    id: 7, title: '招新开放日', imgUrl: '/fixture.png', targetUrl: '/register', sortOrder: 2, status: 1,
    createTime: '2026-11-08T09:00:00', updateTime: '2026-11-08T09:30:00',
  }] } }))
  await page.route('**/fixture.png', route => route.fulfill({ status: 200, contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="90"><rect width="160" height="90" fill="#116477"/></svg>' }))
  await page.goto('/dashboard/carousels')
  await expect(page.getByRole('heading', { name: '首页轮播管理' })).toBeVisible()
  await page.getByRole('button', { name: '编辑', exact: true }).click()
  await expect(page.getByLabel('标题', { exact: true })).toHaveValue('招新开放日')
  await expect(page.getByLabel('跳转地址（选填）')).toHaveValue('/register')
  await expect(page.getByLabel('排序值')).toHaveValue('2')
})

test.describe("authenticated security boundary", () => {
  test.skip(!hasCredentials, "Set E2E_USERNAME and E2E_PASSWORD for the authenticated flow")

  test("login reaches dashboard", async ({ page }) => {
    await page.goto("/login?redirect=%2Fdashboard")
    await page.getByRole("textbox", { name: "用户名", exact: true }).fill(username!)
    await page.locator('input[name="password"]').fill(password!)

    const loginResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === "POST" &&
        new URL(response.url()).pathname === "/api/auth/login"
    )

    await page.getByRole("button", { name: "立即登录" }).click()
    const loginResponse = await loginResponsePromise

    expect(loginResponse.status()).toBe(200)
    await expect(page).toHaveURL(/\/dashboard$/, { timeout: 15_000 })
  })

  test("cookie-authenticated writes require CSRF and enforce role permissions", async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: apiBaseURL })
    const csrfToken = await login(request)

    try {
      const missingCsrf = await request.post("/api/account/deletion-request")
      expect(missingCsrf.status()).toBe(403)

      const forbiddenConfigWrite = await request.post("/api/sys/config/update-about", {
        headers: { "X-CSRF-Token": csrfToken },
        data: { content: "<p>e2e permission probe</p>" },
      })
      expect(forbiddenConfigWrite.status()).toBe(403)
    } finally {
      await request.dispose()
    }
  })

  test("authenticated member can upload a signature-checked PNG", async ({ playwright }) => {
    const request = await playwright.request.newContext({ baseURL: apiBaseURL })
    const csrfToken = await login(request)
    const png = Buffer.from([
      0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
      0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
      0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
      0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, 0xc4,
      0x89,
    ])

    try {
      const uploadResponse = await request.post("/api/common/file/upload", {
        headers: { "X-CSRF-Token": csrfToken },
        multipart: {
          file: { name: "e2e.png", mimeType: "image/png", buffer: png },
        },
      })

      expect(uploadResponse.status()).toBe(200)
      const payload = (await uploadResponse.json()) as ApiEnvelope<string>
      expect(payload.data).toMatch(/^\/files\/\d+\/.+\.png$/)
    } finally {
      await request.dispose()
    }
  })
})
