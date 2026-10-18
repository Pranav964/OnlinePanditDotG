const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function getActivities() {
  return request('/activities')
}

export async function getPandits({ city, country, religion, activityId }) {
  const params = new URLSearchParams()
  if (city) params.set('city', city)
  if (country) params.set('country', country)
  if (religion) params.set('religion', religion)
  if (activityId) params.set('activityId', activityId)
  return request(`/pandits?${params}`)
}

export async function createBooking(payload, token) {
  return request('/bookings', {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: JSON.stringify(payload),
  })
}

export async function authenticate(path, payload) {
  return request(`/auth/${path}`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getCurrentUser(token) {
  return request('/auth/me', { headers: bearer(token) })
}

export function applyForPandit(payload, token) {
  return request('/pandits/apply', { method: 'POST', headers: bearer(token), body: JSON.stringify(payload) })
}

export function getMyBookings(token) {
  return request('/bookings/mine', { headers: bearer(token) })
}

export function getPanditBookings(token) {
  return request('/bookings/pandit', { headers: bearer(token) })
}

export function getAdminBookings(token) {
  return request('/admin/bookings', { headers: bearer(token) })
}

export function getAdminUsers(token) {
  return request('/admin/users', { headers: bearer(token) })
}

export function getPendingPandits(token) {
  return request('/admin/pandits/pending', { headers: bearer(token) })
}

export function reviewPandit(id, approved, token) {
  return request(`/admin/pandits/${id}/approval?approved=${approved}`, { method: 'PATCH', headers: bearer(token) })
}

export function createActivity(payload, token) {
  return request('/admin/activities', { method: 'POST', headers: bearer(token), body: JSON.stringify(payload) })
}

export function getPendingFeedback(token) {
  return request('/admin/feedback/pending', { headers: bearer(token) })
}

export function moderateFeedback(id, approved, token) {
  return request(`/admin/feedback/${id}/moderation?approved=${approved}`, { method: 'PATCH', headers: bearer(token) })
}

export function updateBookingStatus(id, status, token) {
  return request(`/bookings/${id}/status`, { method: 'PATCH', headers: bearer(token), body: JSON.stringify({ status }) })
}

export function cancelBooking(id, token) {
  return request(`/bookings/${id}/cancel`, { method: 'PATCH', headers: bearer(token) })
}

export function rescheduleBooking(id, scheduledDate, token) {
  return request(`/bookings/${id}/reschedule`, { method: 'PATCH', headers: bearer(token), body: JSON.stringify({ scheduledDate }) })
}

export function submitFeedback(payload, token) {
  return request('/feedback', { method: 'POST', headers: bearer(token), body: JSON.stringify(payload) })
}

export function getNotifications(token, { unreadOnly = false, category = '', page = 0, size = 20 } = {}) {
  const params = new URLSearchParams({ unreadOnly: String(unreadOnly), page: String(page), size: String(size) })
  if (category) params.set('category', category)
  return request(`/notifications?${params}`, { headers: bearer(token) })
}

export function markNotificationRead(id, token) {
  return request(`/notifications/${id}/read`, { method: 'PATCH', headers: bearer(token) })
}

export function markAllNotificationsRead(token) {
  return request('/notifications/read-all', { method: 'POST', headers: bearer(token) })
}

export function askRitualAssistant(message, token) {
  return request('/ai/chat', { method: 'POST', headers: bearer(token), body: JSON.stringify({ message }) })
}

function bearer(token) {
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed (${response.status})`)
  }
  return response.json()
}
