import { expect, test } from '@playwright/test'
import { fixtures } from './route-fixtures'

test('all migrated routes render without JavaScript errors or unhandled API requests', async ({ page }) => {
  const unexpected = await fixtures(page)
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  const publicMotifs: Record<string, string> = {
    '/about': 'circuit', '/contributors': 'network', '/privacy': 'shield',
    '/resources': 'layers', '/competitions': 'meteors',
  }
  const paths = ['/', '/about', '/contributors', '/privacy', '/resources', '/competitions', '/login', '/register', '/forgot-password', '/reset-password', '/dashboard', '/dashboard/profile', '/dashboard/resources', '/dashboard/ai', '/dashboard/resume', '/dashboard/resume-reviews', '/dashboard/competitions', '/dashboard/vote', '/dashboard/departments', '/dashboard/audit', '/dashboard/contributions', '/dashboard/member-export', '/dashboard/carousels', '/dashboard/settings']
  for (const path of paths) {
    await page.goto(path)
    await expect(page.locator('main h1').first()).toBeVisible()
    if (publicMotifs[path]) await expect(page.locator('main [data-motif]')).toHaveAttribute('data-motif', publicMotifs[path])
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
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: '切换浅色模式' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await page.getByRole('button', { name: '切换深色模式' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await expect(page.getByRole('tab', { name: '评测', exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
  await page.screenshot({ path: test.info().outputPath('vue-agent-mobile-dark.png'), fullPage: true, animations: 'disabled' })
})
