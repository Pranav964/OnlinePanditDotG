import { useEffect, useState } from 'react'
import { Activity, Bell, CalendarDays, Check, CircleDollarSign, ClipboardList, LogOut, MessageCircle, ShieldCheck, Users, X } from 'lucide-react'
import {
  cancelBooking,
  createActivity,
  getActivities,
  getAdminBookings,
  getAdminUsers,
  getMyBookings,
  getNotifications,
  getPanditBookings,
  getPendingFeedback,
  getPendingPandits,
  moderateFeedback,
  rescheduleBooking,
  reviewPandit,
  submitFeedback,
  updateBookingStatus,
} from '../api.js'
import AIChat from './AIChat.jsx'
import JoinPanditForm from './JoinPanditForm.jsx'
import Notifications from './Notifications.jsx'
import './dashboard.css'

const tabsByRole = {
  USER: [
    ['overview', 'Overview', Activity],
    ['bookings', 'My bookings', CalendarDays],
    ['notifications', 'Notifications', Bell],
    ['assistant', 'Ritual assistant', MessageCircle],
    ['apply', 'Become a pandit', ShieldCheck],
  ],
  PANDIT: [
    ['overview', 'Overview', Activity],
    ['bookings', 'Bookings', CalendarDays],
    ['notifications', 'Notifications', Bell],
  ],
  ADMIN: [
    ['overview', 'Overview', Activity],
    ['approvals', 'Pandit approvals', ShieldCheck],
    ['bookings', 'Bookings', CalendarDays],
    ['users', 'Users', Users],
    ['activities', 'Activities', ClipboardList],
    ['feedback', 'Feedback', MessageCircle],
    ['notifications', 'Notifications', Bell],
  ],
}

const viewTitles = {
  overview: 'Your workspace', bookings: 'Booking management', notifications: 'Notifications',
  assistant: 'Ritual planning', apply: 'Practitioner application', approvals: 'Practitioner approvals',
  users: 'User directory', activities: 'Ceremony catalogue', feedback: 'Feedback moderation',
}

export default function Dashboard({ user, onClose, onLogout, initialView = 'overview' }) {
  const token = localStorage.getItem('onlinepandit-token')
  const [activeView, setActiveView] = useState(initialView)
  const [bookings, setBookings] = useState([])
  const [users, setUsers] = useState([])
  const [pandits, setPandits] = useState([])
  const [activities, setActivities] = useState([])
  const [feedback, setFeedback] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [activityForm, setActivityForm] = useState({ name: '', category: '', duration: '', basePrice: '', religion: 'Hindu' })
  const role = user?.role || 'USER'
  const tabs = tabsByRole[role] || tabsByRole.USER

  useEffect(() => {
    getNotifications(token, { size: 1 }).then((result) => setUnreadCount(result.unreadCount)).catch(() => {})
  }, [token, activeView, refreshKey])

  useEffect(() => {
    let current = true
    async function loadView() {
      setLoading(true)
      setError('')
      try {
        if (activeView === 'bookings') {
          const result = role === 'ADMIN' ? await getAdminBookings(token) : role === 'PANDIT' ? await getPanditBookings(token) : await getMyBookings(token)
          if (current) setBookings(result)
        } else if (activeView === 'users') {
          if (current) setUsers(await getAdminUsers(token))
        } else if (activeView === 'approvals') {
          if (current) setPandits(await getPendingPandits(token))
        } else if (activeView === 'activities') {
          if (current) setActivities(await getActivities())
        } else if (activeView === 'feedback') {
          if (current) setFeedback(await getPendingFeedback(token))
        } else if (activeView === 'overview') {
          if (role === 'ADMIN') {
            const [bookingRows, userRows, panditRows, feedbackRows] = await Promise.all([
              getAdminBookings(token), getAdminUsers(token), getPendingPandits(token), getPendingFeedback(token),
            ])
            if (current) {
              setBookings(bookingRows)
              setUsers(userRows)
              setPandits(panditRows)
              setFeedback(feedbackRows)
            }
          } else {
            const rows = role === 'PANDIT' ? await getPanditBookings(token) : await getMyBookings(token)
            if (current) setBookings(rows)
          }
        }
      } catch (requestError) {
        if (current) setError(requestError.message || 'Could not load this section.')
      } finally {
        if (current) setLoading(false)
      }
    }
    loadView()
    return () => { current = false }
  }, [activeView, refreshKey, role, token])

  async function perform(action) {
    setError('')
    try {
      await action()
      setRefreshKey((value) => value + 1)
    } catch (requestError) {
      setError(requestError.message || 'The request could not be completed.')
    }
  }

  async function submitActivity(event) {
    event.preventDefault()
    await perform(async () => {
      await createActivity({ ...activityForm, basePrice: Number(activityForm.basePrice) }, token)
      setActivityForm({ name: '', category: '', duration: '', basePrice: '', religion: 'Hindu' })
    })
  }

  return (
    <div className="workspace-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="workspace-shell" role="dialog" aria-modal="true" aria-label="OnlinePandit workspace">
        <aside className="workspace-sidebar">
          <div className="workspace-brand"><span className="brand-mark">ॐ</span><span>onlinepandit<span className="brand-dot">.g</span></span></div>
          <div className="workspace-account"><strong>{user?.fullName}</strong><span>{role.toLowerCase()}</span></div>
          <nav className="workspace-nav" aria-label="Workspace sections">
            {tabs.map(([key, label, Icon]) => <button key={key} className={activeView === key ? 'workspace-nav-item active' : 'workspace-nav-item'} onClick={() => setActiveView(key)}><Icon size={17} /><span className="workspace-nav-label">{label}</span>{key === 'notifications' && unreadCount > 0 && <span className="workspace-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}</button>)}
          </nav>
          <div className="workspace-sidebar-bottom">
            <button className="workspace-nav-item" onClick={onClose}><X size={17} />Back to discovery</button>
            <button className="workspace-nav-item" onClick={onLogout}><LogOut size={17} />Sign out</button>
          </div>
        </aside>

        <section className="workspace-content">
          <header className="workspace-header"><div><p className="eyebrow">ONLINEPANDITDOTG / {role}</p><h1>{viewTitles[activeView] || 'Your workspace'}</h1></div><button className="icon-button" onClick={onClose} aria-label="Close workspace"><X size={20} /></button></header>
          {error && <p className="workspace-alert" role="alert">{error}</p>}
          {loading && <p className="workspace-muted" role="status">Loading workspace…</p>}

          {activeView === 'overview' && <Overview role={role} bookings={bookings} users={users} pandits={pandits} feedback={feedback} onNavigate={setActiveView} />}
          {activeView === 'bookings' && <BookingList bookings={bookings} role={role} token={token} onChange={() => setRefreshKey((value) => value + 1)} />}
          {activeView === 'notifications' && <Notifications token={token} />}
          {activeView === 'assistant' && <AIChat token={token} />}
          {activeView === 'apply' && <JoinPanditForm token={token} onSubmitted={() => setRefreshKey((value) => value + 1)} />}
          {activeView === 'approvals' && <PanditApprovals pandits={pandits} token={token} onAction={perform} />}
          {activeView === 'users' && <UserDirectory users={users} />}
          {activeView === 'activities' && <ActivityManager activities={activities} form={activityForm} setForm={setActivityForm} onSubmit={submitActivity} />}
          {activeView === 'feedback' && <FeedbackModeration feedback={feedback} token={token} onAction={perform} />}
        </section>
      </section>
    </div>
  )
}

function Overview({ role, bookings, users, pandits, feedback, onNavigate }) {
  const upcoming = bookings.filter((booking) => ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(booking.bookingStatus)).length
  const cards = role === 'ADMIN'
    ? [['Users', users.length, Users], ['Bookings', bookings.length, CalendarDays], ['Pandits to review', pandits.length, ShieldCheck], ['Feedback to review', feedback.length, MessageCircle]]
    : [['Bookings', bookings.length, CalendarDays], ['Upcoming', upcoming, Activity], ['Payments pending', bookings.filter((booking) => booking.paymentStatus === 'PENDING').length, CircleDollarSign]]
  return <div className="workspace-view"><div className="metric-grid">{cards.map(([label, value, Icon]) => <article className="metric-card" key={label}><Icon size={19} /><span>{label}</span><strong>{value}</strong></article>)}</div><section className="workspace-section"><div className="workspace-section-heading"><div><p className="eyebrow">NEXT STEP</p><h2>{role === 'ADMIN' ? 'Keep the marketplace moving.' : role === 'PANDIT' ? 'Stay close to your schedule.' : 'Plan a meaningful occasion.'}</h2></div><button className="outline-btn" onClick={() => onNavigate(role === 'USER' ? 'bookings' : role === 'ADMIN' ? 'approvals' : 'bookings')}>{role === 'USER' ? 'View bookings' : role === 'ADMIN' ? 'Review applications' : 'Open schedule'}</button></div><p className="workspace-muted">{role === 'ADMIN' ? 'Review practitioner applications, monitor bookings, and keep activity listings current.' : role === 'PANDIT' ? 'Review new requests and keep customers informed as each ceremony progresses.' : 'Your bookings, practitioner updates, and ceremony planning tools are gathered here.'}</p></section></div>
}

function BookingList({ bookings, role, token, onChange }) {
  if (!bookings.length) return <div className="workspace-empty"><CalendarDays size={23} /><h2>No bookings yet</h2><p>New requests and upcoming ceremonies will appear here.</p></div>
  return <div className="workspace-list">{bookings.map((booking) => <BookingCard key={booking.id} booking={booking} role={role} token={token} onChange={onChange} />)}</div>
}

function BookingCard({ booking, role, token, onChange }) {
  const [newDate, setNewDate] = useState(booking.scheduledDate)
  const [review, setReview] = useState({ rating: '5', comment: '' })
  const [reviewSent, setReviewSent] = useState(false)
  const [message, setMessage] = useState('')
  const customerActions = role === 'USER' && ['PENDING', 'CONFIRMED'].includes(booking.bookingStatus)
  const panditActions = role === 'PANDIT' && ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(booking.bookingStatus)
  async function action(request) {
    setMessage('')
    try { await request(); onChange() } catch (error) { setMessage(error.message || 'Action failed.') }
  }
  async function submitReview(event) {
    event.preventDefault()
    await action(async () => {
      await submitFeedback({ bookingId: booking.id, rating: Number(review.rating), comment: review.comment }, token)
      setReviewSent(true)
      setMessage('Feedback submitted for moderation.')
    })
  }
  return (
    <article className="booking-card">
      <div className="booking-card-heading"><div><span className="booking-id">BOOKING #{booking.id}</span><h2>{new Date(`${booking.scheduledDate}T12:00:00`).toLocaleDateString()}</h2></div><span className={`status-label status-${booking.bookingStatus.toLowerCase()}`}>{booking.bookingStatus.replaceAll('_', ' ')}</span></div>
      <div className="booking-details"><span><small>{role === 'USER' ? 'PRACTITIONER' : 'CUSTOMER'}</small>{role === 'USER' ? booking.panditName || `#${booking.panditId}` : booking.userName || `#${booking.userId}`}</span><span><small>CEREMONY</small>{booking.activityName || `#${booking.activityId}`}</span><span><small>LOCATION</small>{booking.serviceAddress}</span><span><small>PAYMENT</small>{booking.paymentStatus} · {booking.currency} {Number(booking.totalAmount).toLocaleString()}</span></div>
      {customerActions && <div className="booking-actions"><label>New date<input type="date" min={new Date().toISOString().slice(0, 10)} value={newDate} onChange={(event) => setNewDate(event.target.value)} /></label><button className="outline-btn" onClick={() => action(() => rescheduleBooking(booking.id, newDate, token))}>Reschedule</button><button className="quiet-danger" onClick={() => action(() => cancelBooking(booking.id, token))}>Cancel booking</button></div>}
      {panditActions && <div className="booking-actions">{booking.bookingStatus === 'PENDING' && <><button className="primary-small" onClick={() => action(() => updateBookingStatus(booking.id, 'CONFIRMED', token))}>Accept request</button><button className="quiet-danger" onClick={() => action(() => updateBookingStatus(booking.id, 'CANCELLED', token))}>Decline</button></>}{booking.bookingStatus === 'CONFIRMED' && <><button className="primary-small" onClick={() => action(() => updateBookingStatus(booking.id, 'IN_PROGRESS', token))}>Start ceremony</button><button className="quiet-danger" onClick={() => action(() => updateBookingStatus(booking.id, 'CANCELLED', token))}>Cancel</button></>}{booking.bookingStatus === 'IN_PROGRESS' && <button className="primary-small" onClick={() => action(() => updateBookingStatus(booking.id, 'COMPLETED', token))}>Mark complete</button>}</div>}
      {role === 'USER' && booking.bookingStatus === 'COMPLETED' && !reviewSent && <form className="feedback-form" onSubmit={submitReview}><strong>How did it go?</strong><select aria-label="Rating" value={review.rating} onChange={(event) => setReview({ ...review, rating: event.target.value })}>{[5, 4, 3, 2, 1].map((rating) => <option key={rating} value={rating}>{rating} stars</option>)}</select><input maxLength="1000" placeholder="Share a short note" value={review.comment} onChange={(event) => setReview({ ...review, comment: event.target.value })} /><button className="primary-small" type="submit">Send feedback</button></form>}
      {message && <p className="inline-message" role="status">{message}</p>}
    </article>
  )
}

function PanditApprovals({ pandits, token, onAction }) {
  if (!pandits.length) return <div className="workspace-empty"><ShieldCheck size={23} /><h2>All applications reviewed</h2><p>New practitioner registrations will appear here.</p></div>
  return <div className="workspace-list">{pandits.map((pandit) => <article className="booking-card" key={pandit.id}><div className="booking-card-heading"><div><span className="booking-id">APPLICATION #{pandit.id}</span><h2>{pandit.fullName}</h2></div><span className="status-label status-pending">Pending review</span></div><div className="booking-details"><span><small>EXPERIENCE</small>{pandit.yearsExperience} years</span><span><small>LOCATION</small>{[pandit.city, pandit.region, pandit.country].filter(Boolean).join(', ')}</span><span><small>RELIGION</small>{pandit.religion}</span><span><small>LANGUAGES</small>{pandit.languages || 'Not provided'}</span><span><small>SPECIALTIES</small>{pandit.specialties?.join(', ') || 'Not provided'}</span></div><div className="booking-actions"><button className="primary-small" onClick={() => onAction(() => reviewPandit(pandit.id, true, token))}>Approve</button><button className="quiet-danger" onClick={() => onAction(() => reviewPandit(pandit.id, false, token))}>Reject</button></div></article>)}</div>
}

function UserDirectory({ users }) {
  return <div className="table-scroll"><table className="workspace-table"><thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Region</th><th>Role</th></tr></thead><tbody>{users.map((user) => <tr key={user.id}><td>{user.fullName}</td><td>{user.email}</td><td>{user.phone || '—'}</td><td>{[user.city, user.country].filter(Boolean).join(', ') || '—'}</td><td>{user.role}</td></tr>)}</tbody></table>{!users.length && <p className="workspace-muted">No accounts found.</p>}</div>
}

function ActivityManager({ activities, form, setForm, onSubmit }) {
  return <div className="activity-manager"><form className="activity-form" onSubmit={onSubmit}><p className="eyebrow">ADD A CEREMONY</p><div className="activity-form-grid"><label>Name<input required maxLength="120" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label><label>Category<input required maxLength="80" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} /></label><label>Duration<input required maxLength="60" value={form.duration} onChange={(event) => setForm({ ...form, duration: event.target.value })} /></label><label>Base price<input required min="0.01" step="0.01" type="number" value={form.basePrice} onChange={(event) => setForm({ ...form, basePrice: event.target.value })} /></label><label>Religion<input required maxLength="60" value={form.religion} onChange={(event) => setForm({ ...form, religion: event.target.value })} /></label></div><button className="primary-small" type="submit">Add activity</button></form><section className="workspace-list compact-list">{activities.map((activity) => <div className="catalogue-row" key={activity.id}><div><strong>{activity.name}</strong><span>{activity.category} · {activity.religion} · {activity.duration}</span></div><b>{activity.price} base price</b></div>)}</section></div>
}

function FeedbackModeration({ feedback, token, onAction }) {
  if (!feedback.length) return <div className="workspace-empty"><Check size={23} /><h2>No feedback waiting</h2><p>New customer reviews will appear here for moderation.</p></div>
  return <div className="workspace-list">{feedback.map((item) => <article className="booking-card" key={item.id}><div className="booking-card-heading"><div><span className="booking-id">FEEDBACK #{item.id} · BOOKING #{item.bookingId}</span><h2>{item.rating} / 5 stars</h2></div><span className="status-label status-pending">Needs review</span></div><p className="feedback-copy">{item.comment || 'No written comment.'}</p><div className="booking-actions"><button className="primary-small" onClick={() => onAction(() => moderateFeedback(item.id, true, token))}>Publish</button><button className="quiet-danger" onClick={() => onAction(() => moderateFeedback(item.id, false, token))}>Keep hidden</button></div></article>)}</div>
}