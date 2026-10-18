import { useEffect, useState } from 'react'
import { Bell, CheckCheck, ChevronLeft, ChevronRight, Circle, Radio } from 'lucide-react'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '../api.js'

const categories = ['', 'BOOKING', 'PAYMENT', 'FEEDBACK']

export default function Notifications({ token }) {
  const [result, setResult] = useState({ content: [], unreadCount: 0, page: 0, totalPages: 0 })
  const [category, setCategory] = useState('')
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [page, setPage] = useState(0)
  const [revision, setRevision] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let current = true
    getNotifications(token, { category, unreadOnly, page, size: 15 })
      .then((data) => { if (current) { setResult(data); setError('') } })
      .catch((requestError) => { if (current) setError(requestError.message || 'Could not load notifications.') })
      .finally(() => { if (current) setBusy(false) })
    return () => { current = false }
  }, [token, category, unreadOnly, page, revision])

  useEffect(() => {
    let socket
    let retryTimer
    let stopped = false
    const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    const socketBase = apiBase.replace(/\/api\/?$/, '').replace(/^http/, 'ws')
    function connect() {
      socket = new WebSocket(`${socketBase}/ws`)
      socket.onopen = () => socket.send(`CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer ${token}\n\n\0`)
      socket.onmessage = (event) => {
        for (const frame of String(event.data).split('\0')) {
          if (frame.startsWith('CONNECTED')) {
            socket.send('SUBSCRIBE\nid:notifications\ndestination:/user/queue/notifications\nack:auto\n\n\0')
          } else if (frame.startsWith('MESSAGE')) {
            const body = frame.slice(frame.indexOf('\n\n') + 2)
            try {
              const item = JSON.parse(body)
              setResult((previous) => ({ ...previous, content: [item, ...previous.content.filter((entry) => entry.id !== item.id)].slice(0, 15), unreadCount: previous.unreadCount + 1 }))
            } catch { /* Ignore malformed or heartbeat frames. */ }
          }
        }
      }
      socket.onclose = () => { if (!stopped) retryTimer = window.setTimeout(connect, 5000) }
    }
    connect()
    return () => { stopped = true; window.clearTimeout(retryTimer); socket?.close() }
  }, [token])

  async function markRead(id) {
    setBusy(true)
    try { await markNotificationRead(id, token); setRevision((value) => value + 1) }
    catch (requestError) { setError(requestError.message || 'Could not update notification.') }
  }

  async function markAllRead() {
    setBusy(true)
    try { await markAllNotificationsRead(token); setRevision((value) => value + 1) }
    catch (requestError) { setError(requestError.message || 'Could not update notifications.') }
  }

  return (
    <section className="notification-view">
      <div className="notification-toolbar"><div className="notification-count"><Bell size={18} /><strong>{result.unreadCount}</strong><span>unread</span><Radio size={14} className="live-indicator" /><small>live</small></div><div className="notification-filters"><select value={category} onChange={(event) => { setCategory(event.target.value); setPage(0) }} aria-label="Notification category">{categories.map((value) => <option key={value || 'all'} value={value}>{value || 'All categories'}</option>)}</select><label><input type="checkbox" checked={unreadOnly} onChange={(event) => { setUnreadOnly(event.target.checked); setPage(0) }} /> Unread only</label><button className="outline-btn" disabled={busy || result.unreadCount === 0} onClick={markAllRead}><CheckCheck size={15} /> Mark all read</button></div></div>
      {error && <p className="workspace-alert" role="alert">{error}</p>}
      <div className="notification-list">{result.content.map((item) => <article className={item.readAt ? 'notification-item read' : 'notification-item'} key={item.id}><Circle size={9} fill={item.readAt ? 'transparent' : 'currentColor'} /><div><div className="notification-item-heading"><strong>{item.title}</strong><span>{item.category}</span></div><p>{item.message}</p><time>{new Date(item.createdAt).toLocaleString()}</time></div>{!item.readAt && <button className="icon-button" aria-label={`Mark ${item.title} as read`} onClick={() => markRead(item.id)}><CheckCheck size={16} /></button>}</article>)}{!result.content.length && <div className="workspace-empty"><Bell size={22} /><h2>No notifications</h2><p>Booking and account updates will appear here.</p></div>}</div>
      <div className="notification-pagination"><span>Page {result.totalPages ? result.page + 1 : 0} of {result.totalPages}</span><button className="icon-button" aria-label="Previous page" disabled={page <= 0} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft size={18} /></button><button className="icon-button" aria-label="Next page" disabled={page + 1 >= result.totalPages} onClick={() => setPage((value) => value + 1)}><ChevronRight size={18} /></button></div>
    </section>
  )
}