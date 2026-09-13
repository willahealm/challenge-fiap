import { seedState, setHelpStatus, updatePlatform, type DemoState, type HelpStatus } from '@embarque-facil/web-core'

const explicitApi = (import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL) as string | undefined
export const apiBase = explicitApi || '/demo-api'
const storageKey = 'embarque-facil:dashboard:v1'
const isSharedDemo = !explicitApi || apiBase.includes(':3100')

const localRead = (): DemoState => {
  const value = localStorage.getItem(storageKey)
  return value ? JSON.parse(value) as DemoState : seedState()
}
const localWrite = (state: DemoState) => { localStorage.setItem(storageKey, JSON.stringify(state)); return state }

const mapTrip = (trip: any) => ({
  id: trip.id, code: trip.code || 'EF4821',
  departureTime: trip.departureTime || new Date(trip.departureAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
  origin: trip.origin, destination: trip.destination, company: trip.company || trip.carrier,
  platform: trip.platform, previousPlatform: trip.previousPlatform,
  status: trip.status === 'BOARDING' ? 'boarding' as const : trip.status === 'ON_TIME' ? 'on-time' as const : 'attention' as const,
  activeJourneys: trip.activeJourneys, terminal: trip.terminal || 'Terminal Tietê', gateClosesAt: trip.gateClosesAt || '22:20',
})
const mapHelp = (request: any) => ({
  id: request.id, tripId: request.tripId || 'trip-demo', totemId: request.totemId,
  pointLabel: request.pointLabel || 'Entrada A · Piso térreo',
  category: ({ MOBILITY: 'mobilidade', VISION_HEARING: 'visao-audicao', TRIP_INFO: 'informacao', SECURITY: 'seguranca' } as const)[request.category as 'MOBILITY'] || request.category,
  status: request.status === 'ACKNOWLEDGED' ? 'assigned' as const : request.status === 'RESOLVED' ? 'resolved' as const : 'waiting' as const,
  createdAt: request.createdAt,
})

export interface Session { accessToken: string; user: { name: string; role: string } }

export async function signIn(email: string, password: string): Promise<Session> {
  if (isSharedDemo) return { accessToken: 'demo-operator', user: { name: 'Marina Costa', role: 'operator' } }
  const response = await fetch(`${apiBase}/v1/auth/demo`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }) })
  if (!response.ok) throw new Error('E-mail, senha ou permissão de operador inválidos.')
  return response.json()
}

export async function signOut(token?: string): Promise<void> {
  if (isSharedDemo || !token) return
  try { await fetch(`${apiBase}/v1/auth/session`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } }) } catch { /* local state is cleared regardless */ }
}

export async function loadState(token?: string): Promise<{ state: DemoState; offline: boolean }> {
  try {
    if (isSharedDemo) {
      const response = await fetch(`${apiBase}/state`)
      if (!response.ok) throw new Error()
      return { state: await response.json(), offline: false }
    }
    const auth = { Authorization: `Bearer ${token}` }
    const [tripsResponse, helpResponse] = await Promise.all([
      fetch(`${apiBase}/v1/ops/trips`, { headers: auth }), fetch(`${apiBase}/v1/ops/help-requests`, { headers: auth }),
    ])
    if (!tripsResponse.ok || !helpResponse.ok) throw new Error()
    const trips = (await tripsResponse.json()).map(mapTrip)
    const helpRequests = (await helpResponse.json()).map(mapHelp)
    return { state: { ...seedState(), trips, alerts: [], helpRequests, updatedAt: new Date().toISOString() }, offline: false }
  } catch {
    return { state: localRead(), offline: true }
  }
}

export function subscribe(onState: (state: DemoState) => void, token?: string): () => void {
  if (!isSharedDemo) {
    const timer = window.setInterval(async () => {
      const result = await loadState(token)
      if (!result.offline) onState(result.state)
    }, 2000)
    return () => window.clearInterval(timer)
  }
  const events = new EventSource(`${apiBase}/events`)
  events.onmessage = (event) => onState(JSON.parse(event.data))
  return () => events.close()
}

export async function publishPlatform(state: DemoState, tripId: string, platform: string, message: string, token?: string): Promise<DemoState> {
  try {
    const endpoint = isSharedDemo ? `${apiBase}/trips/${tripId}/platform` : `${apiBase}/v1/ops/trips/${tripId}/alerts`
    const response = await fetch(endpoint, { method: isSharedDemo ? 'PATCH' : 'POST', headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: JSON.stringify({ type: 'PLATFORM_CHANGE', severity: 'CRITICAL', platform, message }) })
    if (!response.ok) throw new Error()
    if (isSharedDemo) return response.json()
    return updatePlatform(state, tripId, platform, message)
  } catch { return localWrite(updatePlatform(state, tripId, platform, message)) }
}

export async function changeHelpStatus(state: DemoState, requestId: string, status: HelpStatus, token?: string): Promise<DemoState> {
  try {
    const endpoint = isSharedDemo ? `${apiBase}/help-requests/${requestId}` : `${apiBase}/v1/ops/help-requests/${requestId}`
    const wireStatus = status === 'assigned' ? 'ACKNOWLEDGED' : 'RESOLVED'
    const response = await fetch(endpoint, { method: 'PATCH', headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: JSON.stringify({ status: isSharedDemo ? status : wireStatus }) })
    if (!response.ok) throw new Error()
    return isSharedDemo ? response.json() : setHelpStatus(state, requestId, status)
  } catch { return localWrite(setHelpStatus(state, requestId, status)) }
}

export async function resetDemo(token?: string): Promise<{ state: DemoState; session?: Session }> {
  try {
    if (isSharedDemo) {
      const response = await fetch(`${apiBase}/reset`, { method: 'POST' })
      if (response.ok) return { state: await response.json() }
    } else if (token) {
      const response = await fetch(`${apiBase}/v1/demo/reset`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } })
      if (response.ok) {
        const session = await signIn('operador@demo.local', 'Operador123!')
        const result = await loadState(session.accessToken)
        return { state: result.state, session }
      }
    }
  } catch { /* local reset below */ }
  return { state: localWrite(seedState()) }
}
