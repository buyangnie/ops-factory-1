/**
 * E2E Tests: Local Tiny Agent — Real webapp flow
 *
 * Prerequisites: gateway + webapp + Ollama must be running.
 * The test uses qwen3.5:9b through the Local Tiny Agent configuration.
 */
import { test, expect, type Page } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

const USER = `e2e-local-tiny-agent-${Date.now()}`
const USER_STORAGE_KEY = 'opsfactory:userId'
const AGENT_NAME = 'Local Tiny Agent'
const AGENT_ID = 'local-tiny-agent'
const FINAL_MARKER = '当前目录：'

function repoRoot() {
  return path.basename(process.cwd()) === 'test'
    ? path.resolve(process.cwd(), '..')
    : process.cwd()
}

async function loginAs(page: Page, username: string) {
  await page.goto('/#/')
  await page.evaluate(([storageKey, userId]) => {
    localStorage.setItem(storageKey, userId)
  }, [USER_STORAGE_KEY, username])
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForURL(/\/#\/?$/)
  await page.waitForTimeout(500)
}

test.afterAll(() => {
  fs.rmSync(path.join(repoRoot(), 'gateway', 'users', USER), {
    recursive: true,
    force: true,
  })
})

test.describe('Local Tiny Agent', () => {
  test('is last in web agent selector and completes a tool-backed chat turn', async ({ page }) => {
    test.setTimeout(300_000)
    await loginAs(page, USER)

    const trigger = page.locator('.agent-selector-trigger')
    await expect(trigger).toBeVisible({ timeout: 15_000 })
    await trigger.click()

    const options = page.locator('.agent-option')
    await expect(options.last()).toContainText(AGENT_NAME, { timeout: 10_000 })
    await options.filter({ hasText: AGENT_NAME }).click()
    await expect(trigger).toContainText(AGENT_NAME, { timeout: 10_000 })

    const chatInput = page.locator('.chat-input')
    await expect(chatInput).toBeVisible({ timeout: 60_000 })
    await page.waitForFunction(
      () => {
        const input = document.querySelector('.chat-input') as HTMLTextAreaElement | null
        return input && !input.disabled
      },
      { timeout: 60_000 }
    )
    await chatInput.fill('请调用 run_command 执行 pwd。拿到工具结果后只回答一行：当前目录：<stdout>')
    await page.locator('.chat-send-btn-new').click()

    await expect(page).toHaveURL(/\/chat/, { timeout: 15_000 })
    await expect(page.locator('.agent-selector-trigger')).toContainText(AGENT_NAME, { timeout: 10_000 })

    const messageArea = page.locator('.chat-messages-area')
    const expectedWorkingDir = path.join(repoRoot(), 'gateway', 'users', USER, 'agents', AGENT_ID)
    await expect(messageArea.locator('.tool-call-name')).toContainText('Run command', { timeout: 120_000 })
    await expect(messageArea).toContainText(`${FINAL_MARKER}${expectedWorkingDir}`, { timeout: 180_000 })
  })
})
