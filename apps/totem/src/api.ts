import { addHelpRequest, seedState, type DemoState, type HelpCategory } from '@embarque-facil/web-core'

const explicitApi = import.meta.env.VITE_TOTEM_API_URL as string | undefined
const apiBase = explicitApi || '/demo-api'
export const totemId = (import.meta.env.VITE_TOTEM_ID as string | undefined) || 'totem-tiete-01'
const sharedDemo = !explicitApi || apiBase.includes(':3100')
let kioskToken = ''
let lastTouch = 0

const mapJourney = (payload: any): DemoState => {
  const latestChange = (payload.alerts || []).find((alert: any) => alert.type === 'PLATFORM_CHANGE')
  const trip = payload.trip
  return {
    ...seedState(),
    trips: [{
      id: trip.id, code: 'EF4821',
      departureTime: new Date(trip.departureAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }),
      origin: trip.origin, destination: trip.destination, company: trip.carrier, platform: trip.platform,
      previousPlatform: latestChange?.previousPlatform,
      status: trip.status === 'BOARDING' ? 'boarding' : trip.status === 'ON_TIME' ? 'on-time' : 'attention',
      activeJourneys: trip.activeJourneys, terminal: 'Terminal Tietê', gateClosesAt: '22:20',
    }],
    alerts: (payload.alerts || []).map((alert: any) => ({ ...alert, severity: alert.severity.toLowerCase() })),
    updatedAt: new Date().toISOString(),
  }
}

export async function loadDemoState(): Promise<DemoState> {
  try { const response = await fetch(`${apiBase}/state`); if (response.ok) return response.json() } catch { /* seed below */ }
  return seedState()
}

export async function consumeCode(code: string): Promise<{ state: DemoState; offline: boolean }> {
  if (sharedDemo) {
    if (!['EF4821', '482121', 'DEMO21'].includes(code.toUpperCase())) throw new Error('Código não reconhecido. Confira e tente novamente.')
    return { state: await loadDemoState(), offline: false }
  }
  try {
    const login = await fetch(`${apiBase}/v1/auth/demo`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email: 'totem@demo.local', password: 'Totem123!' }) })
    if (!login.ok) throw new Error()
    const identity = await login.json()
    const response = await fetch(`${apiBase}/v1/totems/${totemId}/handoffs/consume`, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${identity.accessToken}` }, body: JSON.stringify({ token: code }) })
    if (!response.ok) throw new Error('Código expirado ou já utilizado.')
    const payload = await response.json(); kioskToken = payload.accessToken
    return { state: mapJourney(payload), offline: false }
  } catch (reason) {
    if (code.toUpperCase() === 'EF4821') return { state: seedState(), offline: true }
    throw reason
  }
}

export function subscribeState(onState: (state: DemoState) => void): () => void {
  if (sharedDemo) {
    const events = new EventSource(`${apiBase}/events`); events.onmessage = (event) => onState(JSON.parse(event.data)); return () => events.close()
  }
  const poll = setInterval(async () => {
    if (!kioskToken) return
    try { const response = await fetch(`${apiBase}/v1/trips/trip-demo/journey`, { headers: { Authorization: `Bearer ${kioskToken}` } }); if (response.ok) onState(mapJourney(await response.json())) } catch { /* keep last useful state */ }
  }, 2000)
  return () => clearInterval(poll)
}

export async function requestHelp(state: DemoState, category: HelpCategory): Promise<DemoState> {
  const apiCategory = { mobilidade: 'MOBILITY', 'visao-audicao': 'VISION_HEARING', informacao: 'TRIP_INFO', seguranca: 'SECURITY' }[category]
  try {
    const endpoint = sharedDemo ? `${apiBase}/help-requests` : `${apiBase}/v1/totems/${totemId}/help-requests`
    const response = await fetch(endpoint, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(kioskToken ? { Authorization: `Bearer ${kioskToken}` } : {}) }, body: JSON.stringify(sharedDemo ? { category, totemId } : { journeyId: 'journey-demo', category: apiCategory }) })
    if (!response.ok) throw new Error()
    return sharedDemo ? response.json() : addHelpRequest(state, category, totemId)
  } catch { return addHelpRequest(state, category, totemId) }
}

export function clearKioskSession() { kioskToken = '' }

export async function touchKioskSession() {
  if (sharedDemo || !kioskToken || Date.now() - lastTouch < 3000) return
  lastTouch = Date.now()
  try { await fetch(`${apiBase}/v1/auth/session/touch`, { method: 'POST', headers: { Authorization: `Bearer ${kioskToken}` } }) } catch { /* visual timeout remains authoritative */ }
}
