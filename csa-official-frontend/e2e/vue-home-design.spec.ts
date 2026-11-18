import { expect, test, type Page } from '@playwright/test'

async function mockPublicHome(page: Page, { failFirstCarousel = false } = {}) {
  let carouselRequests = 0
  const data: Record<string, unknown> = {
    '/api/sys/user/info': { id: 1, username: 'vue-demo', realName: '演示用户', roleLevel: 99, balance: 0 },
    '/api/public/about': '<p>这是浏览器验收所用的协会介绍。</p>',
    '/api/public/contributors': [
      { id: 2, realName: '演示贡献成员', avatar: '', deptName: '开发组', title: '项目成员', roleLevel: 3 },
    ],
    '/api/public/carousel/list': [
      { id: 1, title: '演示活动一', imgUrl: '/files/home-design-fixture.svg', targetUrl: '/about' },
      { id: 2, title: '演示活动二', imgUrl: '/files/home-design-fixture.svg', targetUrl: null },
    ],
    '/api/public/contribution/rank': [
      { userId: 2, realName: '演示贡献成员', username: null, avatar: null, deptName: '开发组', contributionCount: 7, score: 98.5 },
    ],
    '/api/public/competitions': { records: [], total: 0, pages: 1, current: 1, size: 10 },
  }
  const unexpected: string[] = []
  await page.route('**/api/**', route => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/public/carousel/list') {
      carouselRequests += 1
      if (failFirstCarousel && carouselRequests === 1) {
        return route.fulfill({ status: 503, json: { code: 503, message: 'fixture' } })
      }
    }
    if (!Object.hasOwn(data, path)) {
      unexpected.push(path)
      return route.fulfill({ status: 404, json: { code: 404, message: 'fixture' } })
    }
    return route.fulfill({ json: { code: 200, data: data[path] } })
  })
  await page.route('**/files/home-design-fixture.svg', route => route.fulfill({
    contentType: 'image/svg+xml',
    body: '<svg xmlns="http://www.w3.org/2000/svg" width="960" height="360"><rect width="960" height="360" fill="#246778"/></svg>',
  }))
  return { unexpected, carouselRequests: () => carouselRequests }
}

test('home star map keeps real routes while independently loading and retrying public data', async ({ page }) => {
  const fixture = await mockPublicHome(page, { failFirstCarousel: true })
  await page.goto('/')

  const map = page.getByRole('region', { name: '协会功能星图' })
  for (const [name, href] of [
    ['竞赛活动', '/competitions'],
    ['资源库', '/resources'],
    ['简历工作区', '/dashboard/resume'],
    ['Agent 实验室', '/dashboard/ai'],
    ['公开成员与贡献榜', '/contributors'],
  ]) {
    await expect(map.getByRole('link', { name })).toHaveAttribute('href', href)
  }

  const error = page.getByRole('alert').filter({ hasText: '部分首页数据加载失败' })
  await expect(error).toBeVisible()
  await expect(page.getByText('这是浏览器验收所用的协会介绍。')).toBeVisible()
  await expect(page.getByRole('heading', { name: '演示贡献成员' })).toBeVisible()
  await expect(page.getByRole('cell', { name: '98.5' })).toBeVisible()
  await error.getByRole('button', { name: '重新加载' }).click()
  await expect(error).toHaveCount(0)

  const carousel = page.getByRole('region', { name: '协会动态' })
  await expect(carousel.getByRole('heading', { name: '演示活动一' })).toBeVisible()
  await expect(carousel.getByRole('link', { name: '查看详情' })).toHaveAttribute('href', '/about')
  await carousel.getByRole('button', { name: '下一张轮播图' }).click()
  await expect(carousel.getByRole('heading', { name: '演示活动二' })).toBeVisible()
  await carousel.getByRole('button', { name: '上一张轮播图' }).click()
  await expect(carousel.getByRole('heading', { name: '演示活动一' })).toBeVisible()
  expect(fixture.carouselRequests()).toBe(2)

  await map.getByRole('link', { name: '竞赛活动' }).click()
  await expect(page).toHaveURL(/\/competitions$/)
  await expect(page.getByRole('heading', { name: '比赛与活动', level: 1 })).toBeVisible()
  expect(fixture.unexpected).toEqual([])
})

test('time preview follows Beijing and the interactive sky supports rotation and reset', async ({ page }) => {
  await mockPublicHome(page)
  await page.clock.setFixedTime(new Date('2026-11-18T02:30:00Z'))
  await page.goto('/')

  const hero = page.locator('.hero')
  const slider = hero.getByRole('slider', { name: '时段预览' })
  const followClock = hero.getByRole('button', { name: '跟随北京时间' })
  await expect(slider).toHaveAttribute('aria-valuetext', '10:30 · 午间')
  await expect(hero).toHaveAttribute('data-daypart', 'day')
  await slider.fill('1035')
  await expect(hero).toHaveAttribute('data-daypart', 'dusk')
  const grade = () => hero.evaluate(node => getComputedStyle(node).getPropertyValue('--land-grade'))
  const earlyEvening = await grade()
  await slider.fill('1065')
  await expect(hero).toHaveAttribute('data-daypart', 'dusk')
  await expect.poll(grade).not.toBe(earlyEvening)
  await slider.fill('1320')
  await expect(slider).toHaveAttribute('aria-valuetext', '22:00 · 夜间')
  await expect(hero).toHaveAttribute('data-daypart', 'night')
  await expect(followClock).toHaveAttribute('aria-pressed', 'false')
  await followClock.click()
  await expect(followClock).toHaveAttribute('aria-pressed', 'true')
  await expect(hero).toHaveAttribute('data-daypart', 'day')

  const sky = hero.getByRole('region', { name: '可交互的星空' })
  const canvas = sky.locator('canvas[data-horizon-scene]')
  await expect(canvas).toBeVisible()
  await hero.getByRole('button', { name: '暂停星空动效' }).click()
  await expect(hero).toHaveAttribute('data-motion', 'paused')
  await hero.getByRole('button', { name: '重置视角' }).click()

  const frame = () => canvas.evaluate(node => (node as HTMLCanvasElement).toDataURL())
  const original = await frame()
  await sky.focus()
  await sky.press('ArrowRight')
  await expect.poll(frame).not.toBe(original)
  await hero.getByRole('button', { name: '重置视角' }).click()
  await expect.poll(frame).toBe(original)

  await sky.scrollIntoViewIfNeeded()
  const bounds = await sky.boundingBox()
  if (!bounds) throw new Error('The sky interaction region is outside of the browser layout')
  const x = bounds.x + bounds.width * .72
  const y = bounds.y + bounds.height * .30
  await page.mouse.move(x, y)
  await page.mouse.down()
  await page.mouse.move(x + 72, y + 28, { steps: 6 })
  await page.mouse.up()
  await expect.poll(frame).not.toBe(original)
  await hero.getByRole('button', { name: '重置视角' }).click()
  await expect.poll(frame).toBe(original)

  const shape = hero.getByRole('button', { name: '聚成 CSA', exact: true })
  await expect(shape).toHaveAttribute('aria-pressed', 'false')
  await shape.click()
  await expect(shape).toHaveAttribute('aria-pressed', 'true')
  await expect.poll(frame).not.toBe(original)
  await shape.click()
  await expect(shape).toHaveAttribute('aria-pressed', 'false')
  await expect.poll(frame).toBe(original)
})

test('mobile reduced-motion visitors can read the homepage and preview another time', async ({ page }) => {
  await mockPublicHome(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/')

  const hero = page.locator('.hero')
  await expect(hero.getByRole('heading', { level: 1 })).toBeVisible()
  await expect(hero).toHaveAttribute('data-motion', 'reduced')
  await expect(hero.getByRole('button', { name: /星空动效/ })).toHaveCount(0)
  await hero.getByRole('slider', { name: '时段预览' }).fill('720')
  await expect(hero).toHaveAttribute('data-daypart', 'day')
  await expect(page.getByRole('region', { name: '协会功能星图' }).getByRole('link', { name: '资源库' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)

  await page.setViewportSize({ width: 320, height: 740 })
  const rank = page.getByRole('region', { name: '贡献排行榜' })
  await expect(rank.getByRole('table')).toBeVisible()
  await expect(rank.getByRole('cell', { name: '98.5' })).toHaveCount(1)
  const table = rank.getByRole('region', { name: '贡献积分表格' })
  const scrollsInPlace = await table.evaluate(node => node.scrollWidth > node.clientWidth)
  expect(scrollsInPlace).toBe(true)
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(320)
})
