import { useEffect, useState } from 'react'
import { Activity, Bell, CalendarDays, Check, CircleDollarSign, ClipboardList, LogOut, MessageCircle, ShieldCheck, Users, X } from 'lucide-react'
import {
  cancelBooking,
  createActivity,
  createPaymentCheckout,
  downloadPaymentReceipt,
  getActivities,
  getAdminBookings,
  getAdminComplaints,
  getAdminPayments,
  getAdminUsers,
  getMyBookings,
  getMyComplaints,
  getNotificationPreferences,
  getNotifications,
  getPanditBookings,
  getPendingFeedback,
  getPendingPandits,
  getPayoutStatus,
  moderateFeedback,
  rescheduleBooking,
  reviewPandit,
  submitFeedback,
  submitComplaint,
  startPayoutOnboarding,
  updateComplaint,
  updateNotificationPreferences,
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
    ['support', 'Help & complaints', CircleDollarSign],
    ['preferences', 'Notification settings', Bell],
  ],
  PANDIT: [
    ['overview', 'Overview', Activity],
    ['bookings', 'Bookings', CalendarDays],
    ['payouts', 'Payout setup', CircleDollarSign],
    ['preferences', 'Notification settings', Bell],
    ['notifications', 'Notifications', Bell],
  ],
  ADMIN: [
    ['overview', 'Overview', Activity],
    ['approvals', 'Pandit approvals', ShieldCheck],
    ['bookings', 'Bookings', CalendarDays],
    ['payments', 'Payments', CircleDollarSign],
    ['users', 'Users', Users],
    ['activities', 'Activities', ClipboardList],
    ['feedback', 'Feedback', MessageCircle],
    ['complaints', 'Complaints', CircleDollarSign],
    ['notifications', 'Notifications', Bell],
  ],
}

const viewTitles = {
  overview: 'Your workspace', bookings: 'Booking management', notifications: 'Notifications',
  assistant: 'Ritual planning', apply: 'Practitioner application', payouts: 'Payout account', approvals: 'Practitioner approvals',
  users: 'User directory', activities: 'Ceremony catalogue', feedback: 'Feedback moderation', payments: 'Payment transactions',
  support: 'Help and complaints', complaints: 'Complaint management', preferences: 'Notification settings',
}

export default function Dashboard({ user, onClose, onLogout, initialView = 'overview' }) {
  const token = localStorage.getItem('onlinepandit-token')
  const [activeView, setActiveView] = useState(initialView)
  const [bookings, setBookings] = useState([])
  const [users, setUsers] = useState([])
  const [pandits, setPandits] = useState([])
  const [activities, setActivities] = useState([])
  const [feedback, setFeedback] = useState([])
  const [payments, setPayments] = useState([])
  const [complaints, setComplaints] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [activityForm, setActivityForm] = useState({ name: '', category: '', duration: '', basePrice: '', religion: 'Hindu' })
  const [complaintForm, setComplaintForm] = useState({ bookingId: '', paymentId: '', subject: '', description: '' })
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
        } else if (activeView === 'payments') {
          if (current) setPayments(await getAdminPayments(token))
        } else if (activeView === 'support') {
          if (current) setComplaints(await getMyComplaints(token))
        } else if (activeView === 'complaints') {
          if (current) setComplaints(await getAdminComplaints(token))
        } else if (activeView === 'overview') {
          if (role === 'ADMIN') {
            const [bookingRows, paymentRows, userRows, panditRows, feedbackRows] = await Promise.all([
              getAdminBookings(token), getAdminPayments(token), getAdminUsers(token), getPendingPandits(token), getPendingFeedback(token),
            ])
            if (current) {
              setBookings(bookingRows)
              setPayments(paymentRows)
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

          {activeView === 'overview' && <Overview role={role} bookings={bookings} payments={payments} users={users} pandits={pandits} feedback={feedback} onNavigate={setActiveView} />}
          {activeView === 'bookings' && <BookingList bookings={bookings} role={role} token={token} onChange={() => setRefreshKey((value) => value + 1)} />}
          {activeView === 'payouts' && <PayoutSetup token={token} />}
          {activeView === 'preferences' && <NotificationPreferences token={token} />}
          {activeView === 'support' && <SupportCenter complaints={complaints} form={complaintForm} setForm={setComplaintForm} token={token} onSubmitted={() => setRefreshKey((value) => value + 1)} />}
          {activeView === 'complaints' && <ComplaintQueue complaints={complaints} token={token} onAction={perform} />}
          {activeView === 'payments' && <AdminPayments payments={payments} />}
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

function Overview({ role, bookings, payments, users, pandits, feedback, onNavigate }) {
  const upcoming = bookings.filter((booking) => ['PENDING', 'CONFIRMED', 'IN_PROGRESS'].includes(booking.bookingStatus)).length
  const cards = role === 'ADMIN'
    ? [['Users', users.length, Users], ['Bookings', bookings.length, CalendarDays], ['Payments', payments.length, CircleDollarSign], ['Pandits to review', pandits.length, ShieldCheck], ['Feedback to review', feedback.length, MessageCircle]]
    : [['Bookings', bookings.length, CalendarDays], ['Upcoming', upcoming, Activity], ['Payments pending', bookings.filter((booking) => booking.paymentStatus === 'PENDING').length, CircleDollarSign]]
  return <div className="workspace-view"><div className="metric-grid">{cards.map(([label, value, Icon]) => <article className="metric-card" key={label}><Icon size={19} /><span>{label}</span><strong>{value}</strong></article>)}</div><section className="workspace-section"><div className="workspace-section-heading"><div><p className="eyebrow">NEXT STEP</p><h2>{role === 'ADMIN' ? 'Keep the marketplace moving.' : role === 'PANDIT' ? 'Stay close to your schedule.' : 'Plan a meaningful occasion.'}</h2></div><button className="outline-btn" onClick={() => onNavigate(role === 'USER' ? 'bookings' : role === 'ADMIN' ? 'approvals' : 'bookings')}>{role === 'USER' ? 'View bookings' : role === 'ADMIN' ? 'Review applications' : 'Open schedule'}</button></div><p className="workspace-muted">{role === 'ADMIN' ? 'Review practitioner applications, monitor bookings, and keep activity listings current.' : role === 'PANDIT' ? 'Review new requests and keep customers informed as each ceremony progresses.' : 'Your bookings, practitioner updates, and ceremony planning tools are gathered here.'}</p></section></div>
}

function BookingList({ bookings, role, token, onChange }) {
  if (!bookings.length) return <div className="workspace-empty"><CalendarDays size={23} /><h2>No bookings yet</h2><p>New requests and upcoming ceremonies will appear here.</p></div>
  return <div className="workspace-list">{bookings.map((booking) => <BookingCard key={booking.id} booking={booking} role={role} token={token} onChange={onChange} />)}</div>
}

function BookingCard({ booking, role, token, onChange }) {
  const [newDate, setNewDate] = useState(booking.scheduledDate)
  const [newTime, setNewTime] = useState(booking.scheduledTime || '09:00')
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
      <div className="booking-card-heading"><div><span className="booking-id">BOOKING #{booking.id}</span><h2>{new Date(`${booking.scheduledDate}T12:00:00`).toLocaleDateString()} · {booking.scheduledTime || '09:00'}</h2></div><span className={`status-label status-${booking.bookingStatus.toLowerCase()}`}>{booking.bookingStatus.replaceAll('_', ' ')}</span></div>
      <div className="booking-details"><span><small>{role === 'USER' ? 'PRACTITIONER' : 'CUSTOMER'}</small>{role === 'USER' ? booking.panditName || `#${booking.panditId}` : booking.userName || `#${booking.userId}`}</span><span><small>CEREMONY</small>{booking.activityName || `#${booking.activityId}`}</span><span><small>LOCATION</small>{booking.serviceAddress}</span><span><small>PAYMENT</small>{booking.paymentStatus} · {booking.currency} {Number(booking.totalAmount).toLocaleString()}</span></div>
      {role === 'USER' && booking.bookingStatus === 'CONFIRMED' && booking.paymentStatus === 'PENDING' && <StripeCheckoutButton bookingId={booking.id} token={token} />}
      {role === 'USER' && booking.paymentStatus === 'PAID' && <ReceiptButton bookingId={booking.id} token={token} />}
      {customerActions && <div className="booking-actions"><label>New date<input type="date" min={new Date().toISOString().slice(0, 10)} value={newDate} onChange={(event) => setNewDate(event.target.value)} /></label><label>New time<input type="time" value={newTime} onChange={(event) => setNewTime(event.target.value)} /></label><button className="outline-btn" onClick={() => action(() => rescheduleBooking(booking.id, newDate, newTime, token))}>Reschedule</button><button className="quiet-danger" onClick={() => action(() => cancelBooking(booking.id, token))}>Cancel booking</button></div>}
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

function StripeCheckoutButton({ bookingId, token }) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  async function startCheckout() {
    setBusy(true)
    setMessage('')
    try {
      const checkout = await createPaymentCheckout(bookingId, token)
      window.location.assign(checkout.checkoutUrl)
    } catch (error) {
      setMessage(error.message || 'Could not start checkout.')
    } finally {
      setBusy(false)
    }
  }
  return <div className="booking-actions"><button className="primary-small" disabled={busy} onClick={startCheckout}>{busy ? 'Opening secure checkout…' : 'Pay securely with Stripe'}</button>{message && <p className="inline-message" role="alert">{message}</p>}</div>
}

function ReceiptButton({ bookingId, token }) {
  const [message, setMessage] = useState('')
  async function download() {
    try {
      const blob = await downloadPaymentReceipt(bookingId, token)
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `onlinepandit-booking-receipt-${bookingId}.pdf`
      link.click()
      URL.revokeObjectURL(link.href)
    } catch (error) {
      setMessage(error.message || 'Could not download receipt.')
    }
  }
  return <div className="booking-actions"><button className="outline-btn" onClick={download}>Download payment receipt</button>{message && <p className="inline-message" role="alert">{message}</p>}</div>
}

function AdminPayments({ payments }) {
  if (!payments.length) return <div className="workspace-empty"><CircleDollarSign size={23} /><h2>No payment transactions</h2><p>Checkout attempts and verified payments will appear here.</p></div>
  return <div className="table-scroll"><table className="workspace-table"><thead><tr><th>Payment</th><th>Booking</th><th>Gross</th><th>Platform fee</th><th>Provider</th><th>Status</th><th>Receipt</th><th>Paid at</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>#{payment.id}</td><td>#{payment.bookingId}</td><td>{payment.currency} {Number(payment.amount).toLocaleString()}</td><td>{payment.currency} {Number(payment.platformFee).toLocaleString()}</td><td>{payment.provider}</td><td>{payment.paymentStatus}</td><td>{payment.receiptNumber || '—'}</td><td>{payment.paidAt ? new Date(payment.paidAt).toLocaleString() : '—'}</td></tr>)}</tbody></table></div>
}

function PayoutSetup({ token }) {
  const [status, setStatus] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    getPayoutStatus(token).then(setStatus).catch((requestError) => setError(requestError.message || 'Could not load payout status.'))
  }, [token, refresh])
  async function start() {
    setBusy(true)
    setError('')
    try {
      const result = await startPayoutOnboarding(token)
      window.location.assign(result.onboardingUrl)
    } catch (requestError) {
      setError(requestError.message || 'Could not start payout onboarding.')
      setBusy(false)
    }
  }
  return <section className="workspace-section"><div className="workspace-section-heading"><div><p className="eyebrow">STRIPE CONNECT</p><h2>{status?.payoutsEnabled ? 'Payouts are ready.' : 'Set up practitioner payouts.'}</h2></div><span className={`status-label ${status?.payoutsEnabled ? 'status-completed' : 'status-pending'}`}>{status?.payoutsEnabled ? 'Enabled' : 'Action required'}</span></div><p className="workspace-muted">Stripe securely collects your payout details. OnlinePandit does not store bank account numbers or card data.</p>{status?.payoutsEnabled ? <p className="inline-message">Your account can receive booking payouts.</p> : <button className="primary-small" disabled={busy} onClick={start}>{busy ? 'Opening Stripe…' : status?.accountCreated ? 'Continue Stripe verification' : 'Set up payouts'}</button>}<button className="outline-btn" onClick={() => setRefresh((value) => value + 1)}>Refresh status</button>{error && <p className="workspace-alert" role="alert">{error}</p>}</section>
}

function SupportCenter({ complaints, form, setForm, token, onSubmitted }) {
  const [message, setMessage] = useState('')
  async function submit(event) {
    event.preventDefault()
    setMessage('')
    try {
      await submitComplaint({ ...form, bookingId: form.bookingId ? Number(form.bookingId) : null, paymentId: form.paymentId ? Number(form.paymentId) : null }, token)
      setForm({ bookingId: '', paymentId: '', subject: '', description: '' })
      setMessage('Your support request has been submitted.')
      onSubmitted()
    } catch (error) { setMessage(error.message || 'Could not submit your request.') }
  }
  return <div className="activity-manager"><form className="activity-form" onSubmit={submit}><p className="eyebrow">CONTACT SUPPORT</p><div className="activity-form-grid"><label>Booking ID (optional)<input type="number" min="1" value={form.bookingId} onChange={(event) => setForm({ ...form, bookingId: event.target.value })} /></label><label>Payment ID (optional)<input type="number" min="1" value={form.paymentId} onChange={(event) => setForm({ ...form, paymentId: event.target.value })} /></label><label>Subject<input required maxLength="160" value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} /></label></div><label>Description<textarea required maxLength="2000" rows="4" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label><button className="primary-small" type="submit">Submit request</button>{message && <p className="inline-message" role="status">{message}</p>}</form><div className="workspace-list compact-list">{complaints.map((complaint) => <article className="booking-card" key={complaint.id}><div className="booking-card-heading"><div><span className="booking-id">SUPPORT #{complaint.id}</span><h2>{complaint.subject}</h2></div><span className={`status-label status-${complaint.status.toLowerCase()}`}>{complaint.status.replaceAll('_', ' ')}</span></div><p className="feedback-copy">{complaint.description}</p>{complaint.adminNote && <p className="workspace-muted">Support update: {complaint.adminNote}</p>}</article>)}</div></div>
}

function ComplaintQueue({ complaints, token, onAction }) {
  if (!complaints.length) return <div className="workspace-empty"><CircleDollarSign size={23} /><h2>No support requests</h2><p>Customer support requests will appear here.</p></div>
  return <div className="workspace-list">{complaints.map((complaint) => <ComplaintAdminCard key={complaint.id} complaint={complaint} token={token} onAction={onAction} />)}</div>
}

function ComplaintAdminCard({ complaint, token, onAction }) {
  const [status, setStatus] = useState(complaint.status)
  const [adminNote, setAdminNote] = useState(complaint.adminNote || '')
  return <article className="booking-card"><div className="booking-card-heading"><div><span className="booking-id">SUPPORT #{complaint.id} · USER #{complaint.userId}</span><h2>{complaint.subject}</h2></div><span className={`status-label status-${complaint.status.toLowerCase()}`}>{complaint.status.replaceAll('_', ' ')}</span></div><p className="feedback-copy">{complaint.description}</p><p className="workspace-muted">{complaint.bookingId ? `Booking #${complaint.bookingId}` : ''}{complaint.paymentId ? ` · Payment #${complaint.paymentId}` : ''}</p><div className="booking-actions"><label>Status<select value={status} onChange={(event) => setStatus(event.target.value)}><option>OPEN</option><option>IN_REVIEW</option><option>RESOLVED</option></select></label><label>Response<input maxLength="1000" value={adminNote} onChange={(event) => setAdminNote(event.target.value)} /></label><button className="primary-small" onClick={() => onAction(() => updateComplaint(complaint.id, { status, adminNote }, token))}>Save response</button></div></article>
}

function NotificationPreferences({ token }) {
  const [smsOptIn, setSmsOptIn] = useState(false)
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    getNotificationPreferences(token).then((preferences) => setSmsOptIn(preferences.smsOptIn)).catch((error) => setMessage(error.message || 'Could not load preferences.'))
  }, [token])
  async function save(event) {
    const nextValue = event.target.checked
    setSmsOptIn(nextValue)
    setBusy(true)
    setMessage('')
    try {
      await updateNotificationPreferences(nextValue, token)
      setMessage(nextValue ? 'SMS updates enabled.' : 'SMS updates disabled.')
    } catch (error) {
      setSmsOptIn(!nextValue)
      setMessage(error.message || 'Could not update preferences.')
    } finally { setBusy(false) }
  }
  return <section className="workspace-section"><p className="eyebrow">MESSAGE PREFERENCES</p><h2>Choose how booking updates reach you.</h2><label className="preference-toggle"><input type="checkbox" checked={smsOptIn} disabled={busy} onChange={save} /><span>Send booking confirmations, status updates, and schedule reminders by SMS.</span></label><p className="workspace-muted">SMS is sent only to a verified E.164 phone number after you opt in. Carrier charges may apply. You can turn this off at any time.</p>{message && <p className="inline-message" role="status">{message}</p>}</section>
}