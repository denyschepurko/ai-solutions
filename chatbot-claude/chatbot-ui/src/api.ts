// Streams the assistant reply from the chatbot API, invoking onChunk for each
// text delta as it arrives (Server-Sent Events over a POST request). The server
// owns the conversation id: send null on the first turn and reuse the id it
// returns (via the X-Conversation-Id header) on subsequent turns.
export async function streamMessage(
  message: string,
  conversationId: string | null,
  onChunk: (text: string) => void,
): Promise<string | null> {
  const response = await fetch('/api', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(conversationId ? { message, conversationId } : { message }),
  })

  if (!response.ok || !response.body) {
    throw new Error(`Request failed: ${response.status}`)
  }

  const issuedConversationId = response.headers.get('X-Conversation-Id')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })

    // SSE frames are separated by a blank line; keep the trailing partial frame.
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''

    for (const frame of frames) {
      const data = frame
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5))
        .join('\n')
      if (data) onChunk(data)
    }
  }

  return issuedConversationId
}
