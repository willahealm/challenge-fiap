import crypto from 'node:crypto'

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'embarque'
const COLLECTION_ID = process.env.APPWRITE_STATE_COLLECTION_ID || 'mvp_state'
const DOCUMENT_ID = 'demo'

const nowIso = () => new Date().toISOString()
const iso = (value) => new Date(value).toISOString()
const token = () => crypto.randomBytes(32).toString('hex')
const shortCode = () => Array.from({ length: 6 }, () => 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'[crypto.randomInt(32)]).join('')
const cors = {
  'access-control-allow-origin': '*',
  'access-control-allow-headers': 'Authorization, Content-Type, X-Appwrite-Project',
  'access-control-allow-methods': 'GET, POST, PATCH, DELETE, OPTIONS',
  'cache-control': 'no-store',
}

function seed() {
  return {
    trip: {
      id: 'trip-demo', terminalId: 'terminal-tiete', origin: 'São Paulo', destination: 'Rio de Janeiro',
      departureAt: new Date(Date.now() + 86400000).toISOString(), carrier: 'Viação Cometa', platform: '18',
      status: 'ON_TIME', activeJourneys: 12,
    },
    journey: {
      id: 'journey-demo', tripId: 'trip-demo', userId: 'user-lucas', currentPointId: 'TIETE-ENTRADA',
      stage: 'PLANNING', checklist: { document: false, ticket: false, luggage: false, departureTime: false },
      updatedAt: nowIso(),
    },
    alerts: [], helpRequests: [], sessions: {}, handoff: {}, feedback: null,
  }
}

function route(state) {
  const platform = state.trip.platform
  return {
    id: 'route-demo', terminalId: 'terminal-tiete', fromPointId: 'point-entrada-a', toPointId: `point-p${platform}`,
    accessible: true, distanceMeters: 175, headingDegrees: 92,
    steps: [
      { pointId: 'point-corredor-b', instruction: 'Siga pelo corredor B até as escadas rolantes.', distanceMeters: 65, headingDegrees: 92 },
      { pointId: `point-p${platform}`, instruction: `Continue à direita até a plataforma ${platform}.`, distanceMeters: 110, headingDegrees: 128 },
    ],
  }
}

const journeyView = (state) => ({ journey: state.journey, trip: state.trip, route: route(state), alerts: state.alerts })

function secondaryTrip() {
  return {
    id: 'trip-campinas', terminalId: 'terminal-tiete', origin: 'São Paulo', destination: 'Campinas',
    departureAt: new Date(Date.now() + 14400000).toISOString(), carrier: 'Lirabus', platform: '07',
    status: 'BOARDING', activeJourneys: 7,
  }
}

const terminalPoints = () => [
  { id: 'point-entrada-a', terminalId: 'terminal-tiete', code: 'TIETE-ENTRADA', name: 'Entrada A', kind: 'ENTRANCE' },
  { id: 'point-totem', terminalId: 'terminal-tiete', code: 'TIETE-TOTEM-01', name: 'Totem Entrada A', kind: 'TOTEM' },
  { id: 'point-p18', terminalId: 'terminal-tiete', code: 'TIETE-P18', name: 'Plataforma 18', kind: 'PLATFORM' },
  { id: 'point-p21', terminalId: 'terminal-tiete', code: 'TIETE-P21', name: 'Plataforma 21', kind: 'PLATFORM' },
]

function appwriteHeaders(req) {
  return {
    'x-appwrite-project': process.env.APPWRITE_FUNCTION_PROJECT_ID,
    'x-appwrite-key': req.headers['x-appwrite-key'] || process.env.APPWRITE_FUNCTION_API_KEY || '',
    'content-type': 'application/json',
  }
}

function documentUrl() {
  const endpoint = process.env.APPWRITE_FUNCTION_API_ENDPOINT || 'https://fra.cloud.appwrite.io/v1'
  return `${endpoint}/databases/${encodeURIComponent(DATABASE_ID)}/collections/${encodeURIComponent(COLLECTION_ID)}/documents/${DOCUMENT_ID}`
}

async function loadState(req) {
  const response = await fetch(documentUrl(), { headers: appwriteHeaders(req) })
  if (response.status === 404) {
    const initial = seed()
    await createState(req, initial)
    return initial
  }
  if (!response.ok) throw new Error(`Database GET ${response.status}: ${await response.text()}`)
  const document = await response.json()
  return JSON.parse(document.payload)
}

async function createState(req, state) {
  const response = await fetch(documentUrl().replace(`/${DOCUMENT_ID}`, ''), {
    method: 'POST', headers: appwriteHeaders(req),
    body: JSON.stringify({ documentId: DOCUMENT_ID, data: { payload: JSON.stringify(state) } }),
  })
  if (!response.ok && response.status !== 409) throw new Error(`Database POST ${response.status}: ${await response.text()}`)
}

async function saveState(req, state) {
  const response = await fetch(documentUrl(), {
    method: 'PATCH', headers: appwriteHeaders(req), body: JSON.stringify({ data: { payload: JSON.stringify(state) } }),
  })
  if (response.status === 404) return createState(req, state)
  if (!response.ok) throw new Error(`Database PATCH ${response.status}: ${await response.text()}`)
}

const bearer = (req) => (req.headers.authorization || '').replace(/^Bearer\s+/i, '')

function authenticate(req, state) {
  const accessToken = bearer(req)
  const actor = state.sessions[accessToken]
  if (!actor || actor.expiresAt < Date.now()) {
    if (accessToken) delete state.sessions[accessToken]
    return null
  }
  return { ...actor, accessToken }
}

const hasRole = (actor, ...roles) => roles.includes(actor.role)
const pathId = (path, position) => path.split('/')[position] || ''
const apiError = (res, status, code, message) => res.json({ code, message, timestamp: nowIso() }, status, cors)

export default async ({ req, res, error }) => {
  if (req.method === 'OPTIONS') return res.text('', 204, cors)

  try {
    const method = req.method.toUpperCase()
    const path = req.path.length > 1 ? req.path.replace(/\/$/, '') : req.path
    const body = req.bodyText ? req.bodyJson : {}
    const state = await loadState(req)

    if (method === 'GET' && path === '/actuator/health') return res.json({ status: 'UP', service: 'appwrite-function' }, 200, cors)

    if (method === 'POST' && path === '/v1/auth/demo') {
      const email = String(body.email || '').toLowerCase()
      const password = String(body.password || '')
      let user
      if (email === 'lucas@demo.local' && password === 'Demo123!') user = { id: 'user-lucas', name: 'Lucas Almeida', role: 'PASSENGER' }
      else if (email === 'operador@demo.local' && password === 'Operador123!') user = { id: 'operator-demo', name: 'Marina Costa', role: 'OPERATOR' }
      else if (email === 'totem@demo.local' && password === 'Totem123!') user = { id: 'totem-tiete-01', name: 'Totem Tietê', role: 'TOTEM' }
      else return apiError(res, 401, 'INVALID_CREDENTIALS', 'E-mail ou senha inválidos.')

      const accessToken = token()
      const expiresAt = Date.now() + 7200000
      state.sessions[accessToken] = { ...user, expiresAt, absoluteExpiresAt: expiresAt, journeyId: user.role === 'PASSENGER' ? 'journey-demo' : '', totemId: user.role === 'TOTEM' ? 'totem-tiete-01' : '' }
      await saveState(req, state)
      return res.json({ accessToken, tokenType: 'Bearer', expiresAt: iso(expiresAt), user: { ...user, role: user.role.toLowerCase() } }, 200, cors)
    }

    const actor = authenticate(req, state)
    if (!actor) return apiError(res, 401, 'UNAUTHORIZED', 'Sessão inválida ou expirada.')

    if (method === 'DELETE' && path === '/v1/auth/session') {
      delete state.sessions[actor.accessToken]
      await saveState(req, state)
      return res.text('', 204, cors)
    }
    if (method === 'POST' && path === '/v1/auth/session/touch') {
      if (!hasRole(actor, 'KIOSK')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      actor.expiresAt = Math.min(Date.now() + 30000, actor.absoluteExpiresAt)
      state.sessions[actor.accessToken] = actor
      await saveState(req, state)
      return res.json({ expiresAt: iso(actor.expiresAt) }, 200, cors)
    }
    if (method === 'GET' && path === '/v1/me/trips/next') {
      if (!hasRole(actor, 'PASSENGER')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      return res.json(state.trip, 200, cors)
    }
    if (method === 'GET' && path === '/v1/ops/trips') {
      if (!hasRole(actor, 'OPERATOR')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      return res.json([state.trip, secondaryTrip()], 200, cors)
    }
    if (method === 'GET' && path === '/v1/ops/help-requests') {
      if (!hasRole(actor, 'OPERATOR')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      return res.json(state.helpRequests, 200, cors)
    }
    if (method === 'GET' && path === '/v1/terminal/points') return res.json(terminalPoints(), 200, cors)
    if (method === 'GET' && /^\/v1\/trips\/[^/]+\/journey$/.test(path)) {
      if (pathId(path, 3) !== 'trip-demo') return apiError(res, 404, 'NOT_FOUND', 'Recurso não encontrado.')
      if (!hasRole(actor, 'PASSENGER', 'KIOSK')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      return res.json(journeyView(state), 200, cors)
    }
    if (method === 'PATCH' && path === '/v1/journeys/journey-demo/checklist') {
      if (!hasRole(actor, 'PASSENGER')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      state.journey.checklist = { ...state.journey.checklist, ...(body.checklist || {}) }
      state.journey.updatedAt = nowIso()
      await saveState(req, state)
      return res.json(state.journey, 200, cors)
    }
    if (method === 'POST' && path === '/v1/journeys/journey-demo/checkpoints') {
      if (!hasRole(actor, 'PASSENGER')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      const point = terminalPoints().find((item) => item.code === body.code)
      if (!point) return apiError(res, 400, 'INVALID_CHECKPOINT', 'Código do terminal inválido.')
      state.journey.currentPointId = point.id
      state.journey.stage = 'AT_TERMINAL'
      state.journey.updatedAt = nowIso()
      await saveState(req, state)
      return res.json(state.journey, 200, cors)
    }
    if (method === 'POST' && path === '/v1/journeys/journey-demo/handoffs') {
      if (!hasRole(actor, 'PASSENGER')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      state.handoff = { token: token(), code: shortCode(), expiresAt: Date.now() + 60000, consumed: false }
      await saveState(req, state)
      return res.json({ token: state.handoff.token, code: state.handoff.code, expiresAt: iso(state.handoff.expiresAt) }, 201, cors)
    }
    if (method === 'POST' && path === '/v1/totems/totem-tiete-01/handoffs/consume') {
      if (!hasRole(actor, 'TOTEM')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      const supplied = String(body.token || '')
      const demoShortcut = supplied.toUpperCase() === 'EF4821'
      if (!demoShortcut && (!state.handoff.token || state.handoff.consumed || state.handoff.expiresAt < Date.now()
        || (supplied !== state.handoff.token && supplied.toUpperCase() !== state.handoff.code))) {
        return apiError(res, 410, 'HANDOFF_UNAVAILABLE', 'Código expirado ou já utilizado.')
      }
      if (!demoShortcut) state.handoff.consumed = true
      state.journey.currentPointId = 'point-entrada-a'
      state.journey.stage = 'AT_TERMINAL'
      const accessToken = token()
      const createdAt = Date.now()
      state.sessions[accessToken] = { id: 'kiosk-demo', name: 'Totem Tietê', role: 'KIOSK', journeyId: 'journey-demo', totemId: 'totem-tiete-01', expiresAt: createdAt + 30000, absoluteExpiresAt: createdAt + 300000 }
      await saveState(req, state)
      return res.json({ accessToken, expiresAt: iso(createdAt + 30000), ...journeyView(state) }, 200, cors)
    }
    if (method === 'POST' && path === '/v1/totems/totem-tiete-01/help-requests') {
      if (!hasRole(actor, 'TOTEM', 'KIOSK')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      const help = { id: `help-${Date.now()}`, journeyId: 'journey-demo', tripId: 'trip-demo', totemId: 'totem-tiete-01', pointId: 'point-entrada-a', pointLabel: 'Entrada A · Piso térreo', category: body.category, status: 'WAITING', createdAt: nowIso() }
      state.helpRequests.unshift(help)
      await saveState(req, state)
      return res.json(help, 201, cors)
    }
    if (method === 'PATCH' && /^\/v1\/ops\/help-requests\/[^/]+$/.test(path)) {
      if (!hasRole(actor, 'OPERATOR')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      const help = state.helpRequests.find((item) => item.id === pathId(path, 4))
      if (!help) return apiError(res, 404, 'NOT_FOUND', 'Recurso não encontrado.')
      help.status = body.status
      await saveState(req, state)
      return res.json(help, 200, cors)
    }
    if (method === 'POST' && path === '/v1/ops/trips/trip-demo/alerts') {
      if (!hasRole(actor, 'OPERATOR')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      const previousPlatform = state.trip.platform
      state.trip.platform = String(body.platform || state.trip.platform)
      state.trip.status = 'ATTENTION'
      const alert = { id: `alert-${Date.now()}`, tripId: 'trip-demo', type: body.type, severity: body.severity, message: body.message, previousPlatform, platform: state.trip.platform, createdAt: nowIso() }
      state.alerts.unshift(alert)
      await saveState(req, state)
      return res.json(alert, 201, cors)
    }
    if (method === 'POST' && path === '/v1/journeys/journey-demo/complete') {
      if (!hasRole(actor, 'PASSENGER', 'KIOSK')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      state.journey.stage = 'COMPLETED'
      state.journey.updatedAt = nowIso()
      await saveState(req, state)
      return res.json(state.journey, 200, cors)
    }
    if (method === 'POST' && path === '/v1/feedback') {
      if (!hasRole(actor, 'PASSENGER')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      state.feedback = { ...body, id: 'feedback-demo', createdAt: nowIso() }
      await saveState(req, state)
      return res.json(state.feedback, 201, cors)
    }
    if (method === 'POST' && path === '/v1/demo/reset') {
      if (!hasRole(actor, 'OPERATOR')) return apiError(res, 403, 'FORBIDDEN', 'Acesso não permitido.')
      await saveState(req, seed())
      return res.json({ status: 'RESET', tripId: 'trip-demo', journeyId: 'journey-demo' }, 200, cors)
    }

    return apiError(res, 404, 'NOT_FOUND', 'Recurso não encontrado.')
  } catch (cause) {
    error(`${cause.name}: ${cause.message}`)
    return apiError(res, 500, 'INTERNAL_ERROR', 'Não foi possível concluir a operação.')
  }
}
