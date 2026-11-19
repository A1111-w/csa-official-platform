import type { Page } from '@playwright/test'

const emptyPage = { records: [], total: 0, pages: 1, current: 1, size: 10 }
export async function fixtures(page: Page, overrides: Record<string, unknown> = {}) {
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
    ...overrides,
  }
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    const found = Object.hasOwn(data, path) && route.request().method() === 'GET'
    if (!found) requests.push(route.request().method() + ' ' + path)
    await route.fulfill({ status: found ? 200 : 404, json: { code: found ? 200 : 404, data: data[path], message: 'fixture' } })
  })
  return requests
}
