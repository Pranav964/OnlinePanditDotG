import { useEffect, useState } from 'react'
import { ArrowDown, ArrowRight, CalendarDays, Check, ChevronDown, CircleHelp, Clock3, Globe2, MapPin, Menu, Search, ShieldCheck, Sparkles, Star, X } from 'lucide-react'
import { authenticate, createBooking, getActivities, getPandits } from './api.js'
import Dashboard from './components/Dashboard.jsx'
import './workspace.css'

const sampleActivities = [
  { id: 1, name: 'Griha Pravesh', category: 'House & family', duration: '3–4 hours', price: 6500, icon: '⌂' },
  { id: 2, name: 'Wedding ceremony', category: 'Life celebrations', duration: 'Full day', price: 15000, icon: '✳' },
  { id: 3, name: 'Satyanarayan Puja', category: 'Puja & havan', duration: '2–3 hours', price: 4500, icon: '◉' },
  { id: 4, name: 'Havan', category: 'Puja & havan', duration: '2 hours', price: 3500, icon: '♨' },
]
const samplePandits = [
  { id: 1, fullName: 'Pandit Raghav Sharma', yearsExperience: 18, city: 'Mumbai', rating: 4.9, reviewCount: 128, languages: 'Hindi · Sanskrit · Marathi', specialties: ['Griha Pravesh', 'Vedic havan'], image: 'https://images.unsplash.com/photo-1604881991720-f91add269bed?auto=format&fit=crop&w=900&q=85' },
  { id: 2, fullName: 'Acharya Devendra Joshi', yearsExperience: 24, city: 'Mumbai', rating: 4.8, reviewCount: 94, languages: 'Hindi · Sanskrit · Gujarati', specialties: ['Wedding rituals', 'Puja'], image: 'https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=900&q=85' },
  { id: 3, fullName: 'Pandit Kiran Mishra', yearsExperience: 15, city: 'Mumbai', rating: 4.9, reviewCount: 76, languages: 'Hindi · Sanskrit · English', specialties: ['Havan', 'Griha Pravesh'], image: 'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&w=900&q=85' },
]

export default function App() {
  const [activities, setActivities] = useState(sampleActivities)
  const [pandits, setPandits] = useState(samplePandits)
  const [city, setCity] = useState('Mumbai')
  const [country, setCountry] = useState('India')
  const [religion, setReligion] = useState('Hindu')
  const [activityId, setActivityId] = useState('')
  const [selected, setSelected] = useState(null)
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState({ date: '', address: '' })
  const [authForm, setAuthForm] = useState({ fullName: '', email: '', password: '', city: '', country: '', dateOfBirth: '' })
  const [authMode, setAuthMode] = useState('login')
  const [loggedInUser, setLoggedInUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('onlinepandit-user')) } catch { return null }
  })
  const [status, setStatus] = useState('')
  const [loading, setLoading] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [workspaceOpen, setWorkspaceOpen] = useState(false)
  const [workspaceSection, setWorkspaceSection] = useState('overview')

  useEffect(() => {
    getActivities().then(setActivities).catch(() => {})
  }, [])

  async function searchPandits(event) {
    event?.preventDefault()
    setLoading(true)
    try {
      const data = await getPandits({ city, country, religion, activityId })
      setPandits(data.length ? data : [])
    } catch {
      setPandits(samplePandits.filter((pandit) => pandit.city.toLowerCase() === city.toLowerCase()
        && (!country || (pandit.country || 'India').toLowerCase() === country.toLowerCase())
        && (!religion || (pandit.religion || 'Hindu') === religion)))
    } finally {
      document.getElementById('specialists')?.scrollIntoView({ behavior: 'smooth' })
      setLoading(false)
    }
  }

  async function submitBooking(event) {
    event.preventDefault()
    setStatus('')
    try {
      const chosenActivity = activities.find((item) => String(item.id) === String(activityId)) || activities[0]
      await createBooking({
        panditId: selected.id,
        activityId: chosenActivity.id,
        scheduledDate: form.date,
        serviceAddress: form.address,
      }, localStorage.getItem('onlinepandit-token'))
      setStatus('Your request is in. Track its status and updates from your workspace.')
    } catch (error) {
      setStatus(error.message.includes('Failed to fetch') ? 'API is offline. Start the backend to place a booking.' : error.message)
    }
  }

  async function submitAuth(event) {
    event.preventDefault()
    setStatus('')
    try {
      const result = await authenticate(authMode === 'register' ? 'register' : 'login', authMode === 'register'
        ? authForm
        : { email: authForm.email, password: authForm.password })
      localStorage.setItem('onlinepandit-token', result.accessToken)
      localStorage.setItem('onlinepandit-user', JSON.stringify(result.user))
      setLoggedInUser(result.user)
      setModal(false)
      setStatus('')
    } catch (error) {
      setStatus(error.message.includes('Failed to fetch') ? 'API is offline. Start the backend to sign in.' : error.message)
    }
  }

  return (
    <main>
      <header className="topbar">
        <a className="brand" href="#home" aria-label="OnlinePandit home"><span className="brand-mark">ॐ</span><span>onlinepandit<span className="brand-dot">.g</span></span></a>
        <nav className={menuOpen ? 'nav nav-open' : 'nav'}>
          <a href="#ceremonies" onClick={() => setMenuOpen(false)}>Ceremonies</a>
          <a href="#specialists" onClick={() => setMenuOpen(false)}>Our specialists</a>
          <a href="#how" onClick={() => setMenuOpen(false)}>How it works</a>
        </nav>
        <div className="nav-actions">{loggedInUser ? <><span className="signed-in">{loggedInUser.fullName}</span><button className="login-btn" onClick={() => setWorkspaceOpen(true)}>Workspace</button><button className="login-btn" onClick={() => { localStorage.removeItem('onlinepandit-token'); localStorage.removeItem('onlinepandit-user'); setLoggedInUser(null); setWorkspaceOpen(false) }}>Log out</button></> : <button className="login-btn" onClick={() => { setAuthMode('login'); setModal('login') }}>Log in</button>}<button className="become-btn" onClick={() => { if (loggedInUser) { setWorkspaceSection('apply'); setWorkspaceOpen(true) } else { setAuthMode('login'); setModal('login') } }}>Join as a Pandit <ArrowRight size={15} /></button></div>
        <button className="menu-btn" aria-label="Toggle navigation" onClick={() => setMenuOpen(!menuOpen)}>{menuOpen ? <X /> : <Menu />}</button>
      </header>

      <section className="hero" id="home">
        <img className="hero-image" src="https://images.unsplash.com/photo-1604608672516-86c7c77a2d3d?auto=format&fit=crop&w=2400&q=90" alt="A warmly lit Hindu ceremony with marigold flowers and lamps" />
        <div className="hero-shade" />
        <div className="hero-content">
          <p className="eyebrow light"><span /> CEREMONY, THOUGHTFULLY ARRANGED</p>
          <h1>Make space for<br /><em>what matters.</em></h1>
          <p className="hero-copy">Find a trusted ceremony specialist for the moments that bring us together.</p>
          <form className="search-panel" onSubmit={searchPandits}>
            <label className="search-field"><MapPin size={18} /><span><small>YOUR LOCATION</small><input value={city} onChange={(event) => setCity(event.target.value)} placeholder="City or region" aria-label="City or region" /></span><ChevronDown size={15} className="field-chevron" /></label>
            <label className="search-field country-field"><Globe2 size={17} /><span><small>COUNTRY</small><input value={country} onChange={(event) => setCountry(event.target.value)} placeholder="Country" aria-label="Country" /></span></label>
            <label className="search-field religion-field"><Sparkles size={17} /><span><small>TRADITION</small><select value={religion} onChange={(event) => setReligion(event.target.value)} aria-label="Religion or tradition">{['Hindu', 'Multi-faith', 'Christian', 'Muslim', 'Sikh', 'Buddhist', 'Jain', 'Other'].map((value) => <option key={value}>{value}</option>)}</select></span></label>
            <label className="search-field activity-field"><Sparkles size={18} /><span><small>WHAT ARE YOU PLANNING?</small><select value={activityId} onChange={(event) => setActivityId(event.target.value)} aria-label="Choose a ceremony"><option value="">Choose a ceremony</option>{activities.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></span><ChevronDown size={15} className="field-chevron" /></label>
            <button className="search-submit" type="submit"><Search size={17} /><span>Find a specialist</span></button>
          </form>
          <div className="hero-note"><ShieldCheck size={16} /> Every specialist is personally verified</div>
        </div>
        <div className="hero-caption"><span>01 / 04</span><span className="caption-line" /><span>THE BEGINNING OF SOMETHING MEANINGFUL</span></div>
      </section>

      <section className="trust-strip" aria-label="Platform benefits"><div><span className="trust-number">01</span><span>Verified specialists<br /><small>Carefully reviewed, always</small></span></div><div><span className="trust-number">02</span><span>Clear, upfront pricing<br /><small>No last-minute surprises</small></span></div><div><span className="trust-number">03</span><span>Rituals, your way<br /><small>Traditions across communities</small></span></div><span className="trust-aside">A more considered way<br />to come together <ArrowDown size={15} /></span></section>

      <section className="ceremonies section-wrap" id="ceremonies">
        <div className="section-heading"><div><p className="eyebrow">FIND YOUR MOMENT</p><h2>For every <em>new beginning.</em></h2></div><a href="#specialists" className="text-link">Explore all ceremonies <ArrowRight size={16} /></a></div>
        <div className="ceremony-grid">{activities.slice(0, 4).map((activity, index) => <button className={`ceremony-tile tile-${index + 1}`} key={activity.id} onClick={() => { setActivityId(String(activity.id)); document.getElementById('specialists')?.scrollIntoView({ behavior: 'smooth' }) }}><span className="tile-index">0{index + 1}</span><span className="tile-icon">{activity.icon || '✳'}</span><span className="tile-category">{activity.category || 'Ritual & ceremony'}</span><span className="tile-title">{activity.name}</span><span className="tile-meta"><Clock3 size={14} /> {activity.duration || 'By arrangement'} <ArrowRight size={17} className="tile-arrow" /></span></button>)}</div>
      </section>

      <section className="specialists section-wrap" id="specialists">
        <div className="section-heading"><div><p className="eyebrow">PEOPLE WHO MAKE IT PERSONAL</p><h2>Meet your <em>specialist.</em></h2><p className="section-note">Thoughtful, experienced practitioners near {city || 'you'}.</p></div><button className="outline-btn" onClick={() => searchPandits()}>View all specialists <ArrowRight size={15} /></button></div>
        {loading && <p className="empty-state">Finding specialists in {city}…</p>}
        {!loading && pandits.length === 0 && <p className="empty-state">No specialists found in {city}. Try another city.</p>}
        <div className="pandit-grid">{pandits.slice(0, 3).map((pandit, index) => <article className="pandit-card" key={pandit.id}><div className="portrait-wrap"><img src={pandit.imageUrl || pandit.image || samplePandits[index % 3].image} alt={pandit.fullName} loading="lazy" /><span className="verified"><ShieldCheck size={13} /> VERIFIED</span><button className="save-btn" aria-label={`Save ${pandit.fullName}`}><Star size={17} /></button></div><div className="pandit-info"><div className="pandit-title"><h3>{pandit.fullName}</h3><span className="rating"><Star size={13} fill="currentColor" /> {pandit.rating || '4.9'}</span></div><p className="pandit-meta"><MapPin size={13} /> {pandit.city || city} <span>·</span> {pandit.yearsExperience || 12} years experience</p><p className="pandit-language">{pandit.languages || 'Hindi · Sanskrit · English'}</p><div className="specialty-row">{(pandit.specialties || ['Vedic rituals', 'Family ceremonies']).slice(0, 2).map((tag) => <span key={tag}>{tag}</span>)}</div><button className="profile-link" onClick={() => { setSelected(pandit); setStatus(''); setModal(localStorage.getItem('onlinepandit-token') ? 'booking' : 'login') }}>View profile <ArrowRight size={15} /></button></div></article>)}</div>
      </section>

      <section className="story-band" id="how"><div className="story-image"><img src="https://images.unsplash.com/photo-1606293926075-69a00dbfde81?auto=format&fit=crop&w=1300&q=85" alt="Hands arranging flowers for a ceremony" loading="lazy" /></div><div className="story-copy"><p className="eyebrow light">A LITTLE MORE PEACE OF MIND</p><h2>Tradition deserves<br />a <em>thoughtful</em> beginning.</h2><p>Finding the right person for a meaningful day should feel just as considered as the day itself. We bring the people, details, and care together.</p><a href="#specialists" className="story-link">How it works <ArrowRight size={16} /></a><div className="story-count"><strong>01</strong><span>PERSONAL GUIDANCE, FROM FIRST HELLO TO FINAL PRAYER</span></div></div></section>

      <section className="closing section-wrap"><CircleHelp size={23} /><div><h2>Not sure where to begin?</h2><p>Tell us what you're planning. We'll help you find the right fit.</p></div><button className="outline-btn" onClick={() => { if (loggedInUser) { setWorkspaceSection('assistant'); setWorkspaceOpen(true) } else { setAuthMode('login'); setModal('login') } }}>Open ritual assistant <ArrowRight size={15} /></button></section>
      <footer><a className="brand footer-brand" href="#home"><span className="brand-mark">ॐ</span><span>onlinepandit<span className="brand-dot">.g</span></span></a><span>Thoughtfully bringing people and tradition together.</span><div><a href="#ceremonies">Ceremonies</a><a href="#specialists">Specialists</a><a href="mailto:hello@onlinepandit.g">Contact</a></div><small>© 2026 OnlinePanditDotG</small></footer>

      {modal && <div className="modal-backdrop" onClick={() => { setModal(false); setStatus('') }}><section className="booking-modal" onClick={(event) => event.stopPropagation()}><button className="modal-close" onClick={() => { setModal(false); setStatus('') }} aria-label="Close"><X size={19} /></button>{modal === 'booking' ? <><p className="eyebrow">A GOOD PLACE TO START</p><h2>Request a booking</h2><p className="modal-intro">{selected?.fullName} will confirm availability for your ceremony.</p><form onSubmit={submitBooking}><label>Preferred date<input type="date" required value={form.date} onChange={(event) => setForm({ ...form, date: event.target.value })} /></label><label>Ceremony address<textarea required value={form.address} onChange={(event) => setForm({ ...form, address: event.target.value })} placeholder="Address, city, region" rows="3" /></label><button className="search-submit modal-submit" type="submit">Send booking request <ArrowRight size={16} /></button></form>{status && <p className="form-status"><Check size={16} /> {status}</p>}</> : modal === 'login' ? <><p className="eyebrow">ONLINEPANDITDOTG ACCOUNT</p><h2>{authMode === 'login' ? 'Welcome back.' : 'Create your account.'}</h2><p className="modal-intro">{authMode === 'login' ? 'Sign in to request and manage your bookings.' : 'A few details, then you can start planning.'}</p><form onSubmit={submitAuth}>{authMode === 'register' && <><label>Full name<input required maxLength="120" value={authForm.fullName} onChange={(event) => setAuthForm({ ...authForm, fullName: event.target.value })} /></label><div className="auth-location"><label>City<input value={authForm.city} onChange={(event) => setAuthForm({ ...authForm, city: event.target.value })} /></label><label>Country<input value={authForm.country} onChange={(event) => setAuthForm({ ...authForm, country: event.target.value })} /></label></div></>}<label>Email<input type="email" required value={authForm.email} onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })} /></label><label>Password<input type="password" required minLength="10" maxLength="72" value={authForm.password} onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })} /></label><button className="search-submit modal-submit" type="submit">{authMode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={16} /></button></form><button className="auth-switch" onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setStatus('') }}>{authMode === 'login' ? 'New here? Create an account' : 'Already registered? Sign in'}</button>{status && <p className="form-status">{status}</p>}</> : <><p className="eyebrow">ONLINEPANDITDOTG</p><h2>{modal === 'join' ? 'Join our community.' : 'We’re here to help.'}</h2><p className="modal-intro">{modal === 'join' ? 'Practitioner onboarding and verification are managed by our team. Contact hello@onlinepandit.g to begin.' : 'Send us a note at hello@onlinepandit.g and we’ll help you plan.'}</p><button className="search-submit modal-submit" onClick={() => setModal(false)}>Got it <Check size={16} /></button></>}</section></div>}
      {workspaceOpen && loggedInUser && <Dashboard user={loggedInUser} initialView={workspaceSection} onClose={() => setWorkspaceOpen(false)} onLogout={() => { localStorage.removeItem('onlinepandit-token'); localStorage.removeItem('onlinepandit-user'); setLoggedInUser(null); setWorkspaceOpen(false) }} />}
    </main>
  )
}
