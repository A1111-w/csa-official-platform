import { expect, test } from '@playwright/test'
import { dashboardMenu } from '../src/config/menu'
import { fixtures } from './route-fixtures'

test.describe.configure({ mode: 'parallel' })

const publicPaths = ['/', '/about', '/competitions', '/resources', '/contributors', '/privacy']
const authPaths = ['/login', '/register', '/forgot-password', '/reset-password']
const statusPaths = ['/forbidden', '/session-error', '/unknown-layout-check']
const paths = [...publicPaths, ...authPaths, ...dashboardMenu.map(item => item.href), ...statusPaths]

const member = { id: 2, realName: '测试成员', avatar: '', title: '前端开发', deptName: '开发组', roleLevel: 3 }
const event = {
  id: 1, title: '开源项目交流与协作工作坊', summary: '交流技术与项目进展。', coverImg: '/brand/csa-observatory-sky.webp',
  startTime: '2026-11-20T18:00:00', endTime: '2026-11-20T21:00:00', status: 1,
}
const resource = {
  id: 2, title: '计算机协会资源库示例：公开课配套资料与小组实践项目', summary: '用于检查不同宽度下的项目资料排版。',
  category: '课程资料', fileUrl: '/files/course.pdf', uploaderId: 2, downloadCount: 24, createTime: '2026-11-19T09:30:00',
}
function pageOf<T>(item: T) { return { records: [item], size: 10, pages: 1, current: 1, total: 1 } }
const filled = {
  '/api/public/contributors': [member],
  '/api/public/contribution/rank': [{ userId: member.id, realName: member.realName, deptName: member.deptName, score: 35, contributionCount: 3 }],
  '/api/public/competitions': pageOf(event), '/api/biz/comp/list': pageOf(event),
  '/api/sys/resource/list': pageOf(resource), '/api/sys/resource/categories': ['课程资料'],
}

const screens = [{ width: 2124, height: 1106 }, { width: 390, height: 844 }, { width: 320, height: 740 }]
for (const screen of screens) {
  for (const theme of ['dark', 'light']) {
    test(`all pages fit ${screen.width}px in the ${theme} theme`, async ({ page }, testInfo) => {
      test.setTimeout(120_000)
      await page.setViewportSize(screen)
      await page.emulateMedia({ reducedMotion: 'reduce' })
      await page.addInitScript(value => localStorage.setItem('csa-theme', value), theme)
      const unhandled = await fixtures(page, filled)
      const errors: string[] = []
      page.on('pageerror', error => errors.push(error.message))

      for (const path of paths) {
        await test.step(path, async () => {
          await page.goto(path)
          await page.waitForLoadState('networkidle')
          const heading = page.locator('main h1').first()
          await expect.soft(heading, path + ' main heading').toBeVisible()
          expect.soft(new URL(page.url()).pathname, path + ' location').toBe(path)
          await expect.soft(page.locator('html')).toHaveAttribute('data-theme', theme)
          await expect.soft(page.locator('main [role="alert"]'), path + ' unexpected alert').toHaveCount(0)
          const contentWidth = await page.evaluate(() => document.documentElement.scrollWidth)
          expect.soft(contentWidth, path + ' document width').toBeLessThanOrEqual(screen.width)
          const clipped = await page.locator('main').evaluate(main =>
            [...main.querySelectorAll<HTMLElement>('a[href], button, input:not([type=hidden]), select, textarea')].filter(control => {
              const bounds = control.getBoundingClientRect()
              if (bounds.width <= 1 || bounds.height <= 1 || getComputedStyle(control).visibility === 'hidden') return false
              if (bounds.left >= -1 && bounds.right <= innerWidth + 1) return false
              for (let parent = control.parentElement; parent && parent !== main; parent = parent.parentElement) {
                if (parent.scrollWidth > parent.clientWidth && /auto|scroll/.test(getComputedStyle(parent).overflowX)) return false
              }
              return true
            }).map(control => control.innerText.trim().slice(0, 36) || control.getAttribute('name') || control.tagName))
          expect.soft(clipped, path + ' controls outside the viewport without local scrolling').toEqual([])
          if (await heading.isVisible()) {
            const bounds = await heading.boundingBox()
            expect.soft(bounds?.x ?? -1, path + ' heading left edge').toBeGreaterThanOrEqual(-1)
            expect.soft((bounds?.x ?? 0) + (bounds?.width ?? 0), path + ' heading right edge').toBeLessThanOrEqual(screen.width + 1)
          }
          if (process.env.CSA_CAPTURE_LAYOUT === '1') {
            const name = path.slice(1).replaceAll('/', '-') || 'home'
            await page.screenshot({ path: testInfo.outputPath(name + '.jpg'), quality: 70, animations: 'disabled' })
          }
        })
      }
      expect(errors).toEqual([])
      expect(unhandled).toEqual([])
    })
  }
}

test('member, guest and outage states retain the right recovery actions', async ({ page }) => {
  const unexpected = await fixtures(page)
  let status = 401
  await page.route('**/api/sys/user/info', route => route.fulfill({
    status,
    json: { code: status, data: status === 200 ? { id: 7, roleLevel: 1, username: 'member' } : null, message: 'fixture' },
  }))
  await page.goto('/dashboard/ai')
  await expect(page).toHaveURL(url => url.pathname === '/login' && url.searchParams.get('redirect') === '/dashboard/ai')
  await page.getByRole('button', { name: '立即登录' }).click()
  await expect(page.locator('input[name="username"]')).toBeFocused()
  status = 200
  await page.goto('/dashboard/audit')
  await expect(page).toHaveURL(/\/forbidden$/)
  await expect(page.getByRole('link', { name: '返回首页', exact: true })).toHaveAttribute('href', '/')
  status = 503
  await page.goto('/dashboard/ai')
  await expect(page.getByRole('heading', { name: '暂时无法验证会话' })).toBeVisible()
  status = 200
  await page.getByRole('link', { name: '重新验证' }).click()
  await expect(page).toHaveURL(/\/dashboard\/ai$/)
  await expect(page.locator('main h1')).toBeVisible()
  expect(unexpected).toEqual([])
})
