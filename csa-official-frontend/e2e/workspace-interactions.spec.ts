import { expect, test } from '@playwright/test'
import { fixtures } from './route-fixtures'

const firstPage = (records: object[]) => ({ records, total: records.length, pages: 1, current: 1 })

for (const width of [390, 1440]) {
  test.describe('workspace tasks at ' + width + 'px', () => {
    test.use({ viewport: { width, height: 880 } })

    test('personal tools respect the role, reveal on request and keep the working draft', async ({ page }) => {
      const unexpected = await fixtures(page, {
        '/api/sys/user/info': { id: 7, username: 'member', roleLevel: 2, email: 'member@example.invalid' },
        '/api/resume/my': { status: 0, content: '已有经历', gitRepoUrl: '' },
        '/api/ai/documents': [{ id: 'doc-1', title: '项目手册', chunkCount: 2, minRoleLevel: 1 }],
      })
      await page.goto('/dashboard')
      const search = page.getByRole('searchbox', { name: '查找工作台功能' })
      await search.fill('审核队列')
      await expect(page.getByRole('status')).toContainText('没有匹配的入口')
      await search.fill('简历')
      await expect(page.locator('.quick-link')).toHaveCount(1)
      await page.locator('.quick-link').click()
      const editor = page.getByLabel('详细介绍（Markdown）')
      await page.getByRole('button', { name: '追加项目提纲' }).click()
      await expect(editor).toHaveValue(/^已有经历\n\n## 项目经历/)
      await expect(editor).toBeFocused()
      await page.goto('/dashboard/profile')
      const facts = page.locator('#profile-fields')
      await expect(facts).not.toContainText('member@example.invalid')
      await page.getByRole('button', { name: '显示联系方式与学号' }).click()
      await expect(facts).toContainText('member@example.invalid')
      await page.getByRole('button', { name: '隐藏联系方式与学号' }).click()
      await expect(facts).not.toContainText('member@example.invalid')
      await page.goto('/dashboard/ai')
      await page.locator('#agent-question').fill('怎样开始？')
      await page.getByRole('tab', { name: '知识库' }).click()
      await page.getByRole('button', { name: '围绕这份资料提问' }).click()
      await expect(page.locator('#agent-question')).toHaveValue('怎样开始？\n请结合《项目手册》回答。')
      await expect(page.locator('#agent-question')).toBeFocused()
      expect(unexpected).toEqual([])
    })

    test('reviewers sort the current queue, browse proposal topics and check appointable candidates', async ({ page }) => {
      const unexpected = await fixtures(page, {
        '/api/resume/reviews': firstPage([
          { id: 1, realName: '新提交', status: 1, updateTime: '2026-11-18T10:00:00' },
          { id: 2, realName: '早提交', status: 1, updateTime: '2026-11-10T10:00:00' },
        ]),
        '/api/sys/vote/list': [
          { id: 1, type: 'ACTIVITY', title: '开放日', status: 0 },
          { id: 2, type: 'CODE_DEPLOY', title: '发布版本', status: 1 },
        ],
        '/api/sys/dept/list': [{ id: 1, name: '开发组', leaderId: 2 }],
        '/api/sys/user/list': [
          { id: 2, username: 'lead', realName: '现任部长', roleLevel: 3, departmentId: 1 },
          { id: 3, username: 'peer', realName: '候选成员', roleLevel: 2 },
        ],
      })
      await page.goto('/dashboard/resume-reviews')
      await page.getByLabel('本页审核顺序').selectOption('oldest')
      await expect(page.locator('.reviews-grid article h2').first()).toHaveText('早提交')
      await page.goto('/dashboard/vote')
      await page.getByLabel('按议题查看').selectOption('CODE_DEPLOY')
      await expect(page.locator('.vote-ledger article')).toHaveCount(1)
      await expect(page.locator('.vote-ledger article h2')).toHaveText('发布版本')
      await page.goto('/dashboard/departments')
      await page.getByLabel('只看可任命成员').check()
      await expect(page.locator('tbody tr')).toHaveCount(1)
      await expect(page.locator('tbody tr')).toContainText('候选成员')
      expect(unexpected).toEqual([])
    })

    test('management workspaces trace a request and total scores only for the loaded page', async ({ page }) => {
      const unexpected = await fixtures(page, {
        '/api/sys/audit/list': firstPage([{ id: 1, action: 'ROLE_CHANGE', result: 'SUCCESS', requestId: 'trace-7' }]),
        '/api/sys/contribution/awards': firstPage([
          { id: 1, type: 'DEV', typeLabel: '官网建设', score: '1.24', sourceLabel: '手动' },
          { id: 2, type: 'DEV', typeLabel: '官网建设', score: 2.05, sourceLabel: '手动' },
        ]),
      })
      await page.goto('/dashboard/audit')
      await page.getByText('查看详情 #1').click()
      const traced = page.waitForRequest(req => new URL(req.url()).searchParams.get('requestId') === 'trace-7')
      await page.getByRole('button', { name: '追踪同一请求' }).click()
      expect(new URL((await traced).url()).searchParams.get('page')).toBe('1')
      await expect(page.getByLabel('请求 ID', { exact: true })).toHaveValue('trace-7')
      await page.goto('/dashboard/contributions')
      await page.getByRole('button', { name: '本页按类型汇总' }).click()
      await expect(page.locator('#contribution-summary')).toContainText('2 条 · 3.29 分')
      expect(unexpected).toEqual([])
    })

    test('export presets, image crops and password checks preview a result before mutation', async ({ page }) => {
      const unexpected = await fixtures(page)
      await page.goto('/dashboard/member-export')
      await page.getByRole('button', { name: '联络名单' }).click()
      await expect(page.locator('.column-preview')).toContainText('姓名 / 系统账号 / 手机号 / 其他联系方式')
      await expect(page.getByLabel('学号', { exact: true }).first()).not.toBeChecked()
      await page.goto('/dashboard/carousels')
      await page.getByLabel('图片地址', { exact: true }).fill('/brand/csa-optical-archive.webp')
      await page.getByRole('button', { name: '手机裁切' }).click()
      await expect(page.locator('.crop-frame')).toHaveAttribute('data-frame', 'narrow')
      await expect(page.locator('.crop-frame img')).toBeVisible()
      await page.goto('/dashboard/settings')
      await page.getByLabel('新密码', { exact: true }).fill('example123')
      await expect(page.locator('#password-checks .met')).toHaveCount(3)
      await page.getByLabel('确认新密码').fill('example124')
      await expect(page.getByRole('status')).toHaveText('两次输入尚未一致')
      expect(unexpected).toEqual([])
    })
  })
}
