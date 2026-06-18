import { expect, test } from '@playwright/test'

test('smart office main flow smoke test', async ({ page }) => {
  const apiFailures: string[] = []
  const pageErrors: string[] = []
  const consoleErrors: string[] = []

  page.on('response', (response) => {
    if (response.url().includes('/api/') && response.status() >= 400) {
      apiFailures.push(`${response.status()} ${response.url()}`)
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error' && !message.text().includes('Failed to load resource')) {
      consoleErrors.push(message.text())
    }
  })

  await page.goto('/')
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('heading', { name: 'Smart Office' })).toBeVisible()

  await page.getByPlaceholder('用户名').fill('admin')
  await page.getByPlaceholder('密码').fill('123456')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard/)
  await expect(page.getByText('今日工作台')).toBeVisible()
  await expect.poll(() => page.evaluate(() => localStorage.getItem('smart-office-token'))).not.toBeNull()

  const pages = [
    ['/dashboard', () => page.getByText('今日工作台')],
    ['/system/users', () => page.getByRole('heading', { name: '用户管理' })],
    ['/org', () => page.getByRole('heading', { name: '组织架构' })],
    ['/approvals', () => page.getByRole('button', { name: '新建审批' })],
    ['/messages', () => page.getByRole('heading', { name: '待办中心' })],
    ['/files', () => page.getByRole('heading', { name: '文件中心' })],
    ['/attendance', () => page.getByText('今日考勤')],
  ] as const

  for (const [path, locatorFactory] of pages) {
    await page.goto(path)
    await expect(locatorFactory()).toBeVisible()
    await page.waitForLoadState('networkidle')
  }

  await page.goto('/approvals')
  await page.getByRole('button', { name: '新建审批' }).click()
  await expect(page.getByRole('dialog').getByText('新建审批')).toBeVisible()
  await expect(page.getByRole('button', { name: '保存草稿' })).toBeVisible()
  await expect(page.getByRole('button', { name: '保存并提交' })).toBeVisible()

  await page.goto('/messages')
  await expect(page.getByRole('heading', { name: '通知消息' })).toBeVisible()

  await page.goto('/attendance')
  await expect(page.getByRole('button', { name: '上班打卡' })).toBeVisible()
  await expect(page.getByRole('button', { name: '下班打卡' })).toBeVisible()

  expect(apiFailures, 'API requests should not fail').toEqual([])
  expect(pageErrors, 'Page runtime errors should not occur').toEqual([])
  expect(consoleErrors, 'Console errors should not occur').toEqual([])
})
