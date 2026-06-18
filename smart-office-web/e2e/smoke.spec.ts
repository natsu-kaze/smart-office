import { expect, type Page, test } from '@playwright/test'
import { writeFile } from 'node:fs/promises'

function collectPageProblems(page: Page) {
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

  return { apiFailures, pageErrors, consoleErrors }
}

async function expectNoPageProblems(problems: ReturnType<typeof collectPageProblems>) {
  expect(problems.apiFailures, 'API requests should not fail').toEqual([])
  expect(problems.pageErrors, 'Page runtime errors should not occur').toEqual([])
  expect(problems.consoleErrors, 'Console errors should not occur').toEqual([])
}

async function login(page: Page, username: string, password = '123456') {
  await page.goto('/login')
  await page.evaluate(() => localStorage.clear())
  await page.reload()
  await expect(page).toHaveURL(/\/login/)
  await expect(page.getByRole('heading', { name: 'Smart Office' })).toBeVisible()

  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard/)
  await expect(page.getByText('今日工作台')).toBeVisible()
  await expect.poll(() => page.evaluate(() => localStorage.getItem('smart-office-token'))).not.toBeNull()
}

test('smart office microservice pages smoke test', async ({ page }) => {
  const problems = collectPageProblems(page)

  await login(page, 'admin')

  const pages = [
    ['/dashboard', () => page.getByText('今日工作台')],
    ['/system/users', () => page.getByRole('heading', { name: '用户管理' })],
    ['/org', () => page.getByRole('heading', { name: '组织架构' })],
    ['/approvals', () => page.getByRole('button', { name: '新建审批' })],
    ['/messages', () => page.getByRole('heading', { name: '待办中心' })],
    ['/files', () => page.locator('.page-card').getByRole('heading', { name: '文件中心' })],
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

  await expectNoPageProblems(problems)
})

test('approval message and file flow works through gateway', async ({ page }, testInfo) => {
  const problems = collectPageProblems(page)
  const title = `Playwright approval ${Date.now()}`
  const fileName = `playwright-${Date.now()}.txt`
  const filePath = testInfo.outputPath(fileName)

  await login(page, 'employee')
  await page.goto('/approvals')
  await page.getByRole('button', { name: '新建审批' }).click()
  const createDialog = page.getByRole('dialog', { name: '新建审批' })
  await expect(createDialog).toBeVisible()
  await createDialog.locator('.el-form-item').filter({ hasText: '标题' }).locator('input').fill(title)
  await createDialog.locator('.el-form-item').filter({ hasText: '内容' }).locator('textarea').fill('Created by Playwright microservice smoke.')
  const createResponse = page.waitForResponse((response) => {
    const url = new URL(response.url())
    return url.pathname === '/api/approvals' && response.request().method() === 'POST' && response.status() < 400
  })
  const submitResponse = page.waitForResponse((response) =>
    response.url().includes('/api/approvals/') &&
    response.url().endsWith('/submit') &&
    response.request().method() === 'POST' &&
    response.status() < 400,
  )
  await Promise.all([
    createResponse,
    submitResponse,
    createDialog.getByRole('button', { name: '保存并提交' }).click(),
  ])
  await expect(createDialog).toBeHidden()
  await expect(page.getByRole('row', { name: new RegExp(title) })).toBeVisible()

  await login(page, 'manager')
  await page.goto('/approvals')
  await page.getByText('我的审批待办').click()
  const managerRow = page.getByRole('row', { name: new RegExp(title) })
  await expect(managerRow).toBeVisible()
  await managerRow.getByRole('button', { name: '通过' }).click()
  const approveDialog = page.getByRole('dialog', { name: '审批通过' })
  await expect(approveDialog).toBeVisible()
  await approveDialog.getByRole('textbox').fill('Approved by Playwright.')
  await approveDialog.getByRole('button', { name: '通过' }).click()
  await expect(page.getByText('已审批通过，申请人通知已生成')).toBeVisible()

  await login(page, 'employee')
  await page.goto('/messages')
  const noticeRow = page.getByRole('row', { name: new RegExp(`${title} has been approved`) })
  await expect(noticeRow).toBeVisible()
  await expect(noticeRow).toContainText('Approval passed')

  await writeFile(filePath, 'Uploaded by Playwright microservice smoke.')
  await page.goto('/files')
  await page.locator('input[type="file"]').setInputFiles(filePath)
  await expect(page.getByText('文件已上传')).toBeVisible()
  await expect(page.getByRole('row', { name: new RegExp(fileName) })).toBeVisible()

  await expectNoPageProblems(problems)
})
