import { useState, useRef, useEffect } from 'react'
import type { FormEvent } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { streamMessage } from './api'
import './App.css'

type Role = 'user' | 'assistant'

interface ChatMessage {
  role: Role
  text: string
}

const SUGGESTIONS = [
  'What is the difference between a deduction and a credit?',
  'How do federal tax brackets work?',
  'What is a W-2 form?',
]

function App() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const conversationId = useRef<string | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  function appendChunk(chunk: string) {
    setMessages((prev) => {
      const last = prev[prev.length - 1]
      if (last?.role === 'assistant') {
        return [...prev.slice(0, -1), { ...last, text: last.text + chunk }]
      }
      return [...prev, { role: 'assistant', text: chunk }]
    })
  }

  async function send(text: string) {
    if (!text || loading) return

    setInput('')
    setError(null)
    setMessages((prev) => [...prev, { role: 'user', text }])
    setLoading(true)

    try {
      const issuedId = await streamMessage(text, conversationId.current, appendChunk)
      if (issuedId) conversationId.current = issuedId
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed')
    } finally {
      setLoading(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void send(input.trim())
  }

  const awaitingFirstChunk = loading && messages[messages.length - 1]?.role !== 'assistant'

  return (
    <div className="app">
      <header className="header">
        <div className="brand">
          <span className="logo" aria-hidden="true">
            TF
          </span>
          <div className="brand-text">
            <h1>TaxInfoBot</h1>
            <p>General U.S. federal tax information &middot; not professional advice</p>
          </div>
        </div>
      </header>

      <main className="messages">
        {messages.length === 0 && (
          <div className="welcome">
            <h2>How can I help with U.S. federal taxes?</h2>
            <p>Ask about tax concepts, common forms, or filing basics. I share general information, not individualized advice.</p>
            <div className="suggestions">
              {SUGGESTIONS.map((suggestion) => (
                <button key={suggestion} type="button" onClick={() => void send(suggestion)}>
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message, index) => (
          <div key={index} className={`row ${message.role}`}>
            <div className={`bubble ${message.role}`}>
              {message.role === 'assistant' ? (
                <div className="markdown">
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>{message.text}</ReactMarkdown>
                </div>
              ) : (
                message.text
              )}
            </div>
          </div>
        ))}

        {awaitingFirstChunk && (
          <div className="row assistant">
            <div className="bubble assistant thinking">
              <span className="dot" />
              <span className="dot" />
              <span className="dot" />
            </div>
          </div>
        )}
        {error && <div className="error">{error}</div>}
        <div ref={bottomRef} />
      </main>

      <form className="composer" onSubmit={handleSubmit}>
        <input
          type="text"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          placeholder="Ask a general U.S. federal tax question…"
          autoFocus
        />
        <button type="submit" disabled={loading || !input.trim()}>
          Send
        </button>
      </form>

      <p className="disclaimer">
        Demo for testing and educational purposes only &middot; not for commercial use &middot; not professional tax advice
      </p>
    </div>
  )
}

export default App
