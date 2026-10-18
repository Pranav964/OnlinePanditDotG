import { useState } from 'react'
import { ArrowUp, Bot, UserRound } from 'lucide-react'
import { askRitualAssistant } from '../api.js'

export default function AIChat({ token }) {
  const [messages, setMessages] = useState([{ role: 'assistant', text: 'Tell me what you are planning. I can help you prepare questions for a practitioner and find the right ceremony details.' }])
  const [prompt, setPrompt] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')

  async function send(event) {
    event.preventDefault()
    const text = prompt.trim()
    if (!text || sending) return
    setMessages((previous) => [...previous, { role: 'user', text }])
    setPrompt('')
    setSending(true)
    setError('')
    try {
      const result = await askRitualAssistant(text, token)
      setMessages((previous) => [...previous, { role: 'assistant', text: result.answer }])
    } catch {
      setError('The assistant is not available. Ask the team or try again after AI is configured.')
    } finally { setSending(false) }
  }

  return <section className="ai-chat"><div className="ai-chat-heading"><span className="ai-icon"><Bot size={20} /></span><div><h2>Ritual planning assistant</h2><p>Planning support, not religious advice.</p></div></div><div className="ai-messages" aria-live="polite">{messages.map((message, index) => <article className={`ai-message ${message.role}`} key={`${message.role}-${index}`}><span>{message.role === 'assistant' ? <Bot size={16} /> : <UserRound size={16} />}</span><p>{message.text}</p></article>)}{sending && <p className="workspace-muted">Preparing a response…</p>}</div>{error && <p className="workspace-alert" role="alert">{error}</p>}<form className="ai-prompt" onSubmit={send}><input value={prompt} onChange={(event) => setPrompt(event.target.value)} maxLength={1000} placeholder="Ask about planning your ceremony" aria-label="Message the ritual assistant" /><button type="submit" aria-label="Send message" disabled={sending || !prompt.trim()}><ArrowUp size={18} /></button></form></section>
}