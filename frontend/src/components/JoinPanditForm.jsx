import { useState } from 'react'
import { ArrowRight, ShieldCheck } from 'lucide-react'
import { applyForPandit } from '../api.js'

export default function JoinPanditForm({ token, onSubmitted }) {
  const [form, setForm] = useState({ yearsExperience: '', dateOfBirth: '', city: '', region: '', country: '', religion: 'Hindu', languages: '', specialties: '' })
  const [status, setStatus] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setSubmitting(true)
    setStatus('')
    try {
      await applyForPandit({ ...form, yearsExperience: Number(form.yearsExperience) }, token)
      setStatus('Application submitted. An admin must approve your profile. Sign in again after approval to refresh your account role.')
      onSubmitted()
    } catch (error) { setStatus(error.message || 'Application could not be submitted.') }
    finally { setSubmitting(false) }
  }

  function update(event) { setForm((current) => ({ ...current, [event.target.name]: event.target.value })) }

  return <section className="pandit-application"><div className="application-intro"><ShieldCheck size={21} /><div><h2>Apply to join the practitioner network</h2><p>Your profile stays private until it has been reviewed.</p></div></div><form onSubmit={submit}><div className="application-grid"><label>Years of experience<input name="yearsExperience" type="number" min="0" max="80" required value={form.yearsExperience} onChange={update} /></label><label>Date of birth<input name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={update} /></label><label>City<input name="city" required maxLength="80" value={form.city} onChange={update} /></label><label>Region / state<input name="region" maxLength="80" value={form.region} onChange={update} /></label><label>Country<input name="country" required maxLength="80" value={form.country} onChange={update} /></label><label>Religion<input name="religion" required maxLength="60" value={form.religion} onChange={update} /></label><label>Languages<input name="languages" maxLength="255" placeholder="Hindi, Sanskrit, English" value={form.languages} onChange={update} /></label><label>Specialties<input name="specialties" maxLength="500" placeholder="Wedding, havan, naming ceremony" value={form.specialties} onChange={update} /></label></div><button className="primary-small" type="submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit for review'} <ArrowRight size={15} /></button></form>{status && <p className="inline-message" role="status">{status}</p>}</section>
}