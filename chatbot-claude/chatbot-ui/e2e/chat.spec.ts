import { test, expect } from '@playwright/test'

// Build a Server-Sent Events body from a list of text chunks, matching what the
// streaming backend emits (one `data:` frame per chunk, separated by a blank line).
function sse(chunks: string[]): { contentType: string; body: string } {
  return {
    contentType: 'text/event-stream',
    body: chunks.map((chunk) => `data:${chunk}\n\n`).join(''),
  }
}

// Mock /api so the test is deterministic and never calls the real Claude API.
test.beforeEach(async ({ page }) => {
  await page.route('**/api', async (route) => {
    await route.fulfill(sse(['Fo', 'ur']))
  })
})

test('streams the assistant response into the chat', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByText('How can I help with U.S. federal taxes?')).toBeVisible()

  await page.getByPlaceholder('Ask a general U.S. federal tax question…').fill('What is 2 + 2?')
  await page.getByRole('button', { name: 'Send' }).click()

  await expect(page.getByText('What is 2 + 2?')).toBeVisible()
  await expect(page.getByText('Four')).toBeVisible()
})

test('adopts the server-issued conversationId and reuses it across turns', async ({ page }) => {
  // The mock plays the role of a backend that owns the conversation id: it issues
  // one via the X-Conversation-Id header and records every id the UI sends back,
  // streaming a running total (4, then 4 + 7 = 11).
  const sentConversationIds: (string | undefined)[] = []
  await page.route('**/api', async (route) => {
    const { message, conversationId } = route.request().postDataJSON()
    sentConversationIds.push(conversationId)
    await route.fulfill({
      ...sse([message.includes('2 + 2') ? '4' : '11']),
      headers: { 'X-Conversation-Id': 'server-issued-id' },
    })
  })

  await page.goto('/')

  await page.getByPlaceholder('Ask a general U.S. federal tax question…').fill('what is 2 + 2')
  await page.getByRole('button', { name: 'Send' }).click()
  await expect(page.getByText('4', { exact: true })).toBeVisible()

  await page.getByPlaceholder('Ask a general U.S. federal tax question…').fill('add 7')
  await page.getByRole('button', { name: 'Send' }).click()
  await expect(page.getByText('11', { exact: true })).toBeVisible()

  // First turn sends no id; the UI then reuses the server-issued id so the
  // backend can thread history.
  expect(sentConversationIds).toHaveLength(2)
  expect(sentConversationIds[0]).toBeUndefined()
  expect(sentConversationIds[1]).toBe('server-issued-id')
})

test('shows an error when the API fails', async ({ page }) => {
  await page.route('**/api', (route) => route.fulfill({ status: 500 }))

  await page.goto('/')
  await page.getByPlaceholder('Ask a general U.S. federal tax question…').fill('boom')
  await page.getByRole('button', { name: 'Send' }).click()

  await expect(page.getByText('Request failed: 500')).toBeVisible()
})
