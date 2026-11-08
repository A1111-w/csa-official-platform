import { expect, test, type Page } from '@playwright/test'

const emptyPage = { records: [], total: 0, pages: 1, current: 1, size: 10 }
async function fixtures(page: Page) {
  const requests: string[] = []
  const data: Record<string, unknown> = {
    '/api/sys/user/info': { id: 1, username: 'vue-demo', realName: '演示成员', roleLevel: 99, balance: 0 },
    '/api/public/about': '<h2>在实践中学习，在协作中成长。</h2><p>我们分享知识、一起参加比赛，并将学习成果变成真实项目。</p>',
    '/api/public/carousel/list': [],
    '/api/public/contributors': [],
    '/api/public/contribution/rank': [],
    '/api/public/privacy': { policyVersion: 'fixture-2026', collectedFields: ['用户名'], purposes: {}, retention: {}, userRights: [], contactEmail: 'privacy@example.invalid' },
    '/api/public/competitions': emptyPage,
    '/api/sys/resource/list': emptyPage,
    '/api/sys/resource/categories': [],
    '/api/biz/comp/list': emptyPage,
    '/api/sys/dept/list': [],
    '/api/sys/user/list': [],
    '/api/sys/vote/list': [],
    '/api/sys/audit/list': emptyPage,
    '/api/sys/contribution/awards': emptyPage,
    '/api/sys/carousel/list': [],
    '/api/resume/my': null,
    '/api/resume/git-sync': { configured: false, status: 'NOT_SYNCED' },
    '/api/resume/reviews': emptyPage,
    '/api/ai/status': { mode: 'offline', model: 'extractive', embeddingMode: 'bm25' },
    '/api/ai/documents': [],
  }
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    if (!(path in data)) requests.push(path)
    await route.fulfill({ status: path in data ? 200 : 404, json: { code: path in data ? 200 : 404, data: data[path], message: 'fixture' } })
  })
  return requests
}

test('all migrated routes render without JavaScript errors or unhandled API requests', async ({ page }) => {
  const unexpected = await fixtures(page)
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  const paths = ['/', '/about', '/contributors', '/privacy', '/resources', '/competitions', '/login', '/register', '/forgot-password', '/reset-password', '/dashboard', '/dashboard/profile', '/dashboard/resources', '/dashboard/ai', '/dashboard/resume', '/dashboard/resume-reviews', '/dashboard/competitions', '/dashboard/vote', '/dashboard/departments', '/dashboard/audit', '/dashboard/contributions', '/dashboard/member-export', '/dashboard/carousels', '/dashboard/settings']
  for (const path of paths) {
    await page.goto(path)
    await expect(page.locator('main h1').first()).toBeVisible()
    await page.waitForLoadState('networkidle')
    await expect(page.locator('main [role="alert"]')).toHaveCount(0)
    expect(errors, path).toEqual([])
    expect(unexpected, path).toEqual([])
  }
})

test('public and AI layouts remain usable at mobile widths and in the dark theme', async ({ page }) => {
  await fixtures(page)
  await page.goto('/')
  await expect(page.locator('.hero h1')).toBeVisible()
  await page.screenshot({ path: test.info().outputPath('vue-home-desktop.png'), fullPage: true, animations: 'disabled' })
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(page.getByRole('button', { name: '导航', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '导航', exact: true }).click()
  await expect(page.locator('#site-nav')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
  await page.screenshot({ path: test.info().outputPath('vue-home-mobile.png'), fullPage: true, animations: 'disabled' })
  await page.goto('/dashboard/ai')
  await page.getByRole('button', { name: '切换深色模式' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect(page.getByRole('tab', { name: '评测', exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
  await page.screenshot({ path: test.info().outputPath('vue-agent-mobile-dark.png'), fullPage: true, animations: 'disabled' })
})
