import { expect, test, type APIRequestContext } from "@playwright/test"

const username = process.env.E2E_USERNAME
const password = process.env.E2E_PASSWORD
const hasCredentials = Boolean(username && password)
const apiBaseURL = process.env.E2E_API_BASE_URL || process.env.E2E_BASE_URL || "http://127.0.0.1:8080"

type ApiEnvelope<T> = { data?: T; code?: number; message?: string }

async function login(context: APIRequestContext) {
  const csrfResponse = await context.get("/api/auth/csrf")
  expect(csrfResponse.ok()).toBeTruthy()
  const csrfPayload = (await csrfResponse.json()) as ApiEnvelope<{ csrfToken: string }>
  const csrfToken = csrfPayload.data?.csrfToken
  expect(csrfToken).toBeTruthy()

  const loginResponse = await context.post("/api/auth/login", {
    data: { username, password },
  })
  expect(loginResponse.status()).toBe(200)
  const loginPayload = (await loginResponse.json()) as ApiEnvelope<{ csrfToken: string }>

  const resolvedToken = loginPayload.data?.csrfToken || csrfToken
  if (!resolvedToken) {
    throw new Error("Login response did not include a CSRF token")
  }
  return resolvedToken
}

test("public privacy page is reachable", async ({ page }) => {
  await page.goto("/privacy")
  await expect(page.getByRole("heading", { name: "隐私说明" })).toBeVisible()
})

test("dashboard redirects unauthenticated visitors", async ({ page }) => {
  await page.goto("/dashboard")
  await expect(page).toHaveURL(/\/login\?redirect=%2Fdashboard/)
})

test("level 4 can open and edit the mocked carousel management workspace", async ({
  context,
  page,
}) => {
  await context.addCookies([
    {
      name: "CSA_AUTH_TOKEN",
      value: "playwright-carousel-session",
      url: "http://127.0.0.1:3000",
      httpOnly: true,
      sameSite: "Lax",
    },
  ])
  await page.addInitScript(() => {
    localStorage.setItem(
      "csa-auth-storage",
      JSON.stringify({
        state: { user: { username: "president", roleLevel: 4 } },
        version: 3,
      })
    )
  })

  const corsHeaders = {
    "access-control-allow-origin": "http://127.0.0.1:3000",
    "access-control-allow-credentials": "true",
  }
  await page.route("**/api/sys/user/info", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      headers: corsHeaders,
      body: JSON.stringify({
        code: 200,
        message: "Success",
        data: {
          id: 1,
          username: "president",
          realName: "测试会长",
          avatar: null,
          email: null,
          phone: null,
          contact: null,
          positionType: 1,
          roleLevel: 4,
          balance: 0,
          college: null,
          className: null,
          studentId: null,
        },
      }),
    })
  )
  await page.route("**/api/sys/carousel/list", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      headers: corsHeaders,
      body: JSON.stringify({
        code: 200,
        message: "Success",
        data: [
          {
            id: 7,
            title: "招新开放日",
            imgUrl: "https://assets.example.test/carousel.png",
            targetUrl: "/register",
            sortOrder: 2,
            status: 1,
            createTime: "2026-08-27T09:00:00",
            updateTime: "2026-08-27T09:30:00",
          },
        ],
      }),
    })
  )
  await page.route("https://assets.example.test/carousel.png", (route) =>
    route.fulfill({
      status: 200,
      contentType: "image/png",
      body: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
        "base64"
      ),
    })
  )

  await page.goto("/dashboard/carousels")

  await expect(page.getByRole("heading", { name: "首页轮播管理" })).toBeVisible()
  await expect(page.getByRole("heading", { name: "招新开放日" })).toBeVisible()
  await expect(page.getByText("已启用")).toBeVisible()

  await page.getByRole("button", { name: "编辑" }).click()
  await expect(page.getByLabel("标题")).toHaveValue("招新开放日")
  await expect(page.getByRole("textbox", { name: "跳转地址", exact: true })).toHaveValue(
    "/register"
  )
})

test.describe("authenticated security boundary", () => {
  test.skip(!hasCredentials, "Set E2E_USERNAME and E2E_PASSWORD for the authenticated flow")

  test("login reaches dashboard", async ({ page }) => {
    await page.goto("/login?redirect=%2Fdashboard")
    await page.getByPlaceholder("输入你的用户名").fill(username!)
    await page.getByPlaceholder("输入密码").fill(password!)

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
