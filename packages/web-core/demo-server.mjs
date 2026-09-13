import { createServer } from 'node:http'

const trip = {
  id: 'trip-sp-rio-001', code: 'EF4821', departureTime: '22:30', origin: 'São Paulo',
  destination: 'Rio de Janeiro', company: 'Viação Cometa', platform: '18', status: 'on-time',
  activeJourneys: 12, terminal: 'Terminal Tietê', gateClosesAt: '22:20',
}
let state = {
  trips: [trip,
    { id: 'trip-sp-cps-002', code: 'EF1130', departureTime: '21:50', origin: 'São Paulo', destination: 'Campinas', company: 'Lirabus', platform: '07', status: 'boarding', activeJourneys: 7, terminal: 'Terminal Tietê', gateClosesAt: '21:40' },
    { id: 'trip-sp-bh-003', code: 'EF7204', departureTime: '23:10', origin: 'São Paulo', destination: 'Belo Horizonte', company: 'Gontijo', platform: '24', status: 'attention', activeJourneys: 4, terminal: 'Terminal Tietê', gateClosesAt: '23:00' },
  ],
  alerts: [{ id: 'alert-seed-1', tripId: 'trip-sp-bh-003', severity: 'warning', message: 'Embarque previsto com 10 minutos de atraso.', createdAt: new Date(Date.now() - 18 * 60_000).toISOString() }],
  helpRequests: [], csat: 4.8, updatedAt: new Date().toISOString(),
}

const clients = new Set()
const headers = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': 'Content-Type', 'Access-Control-Allow-Methods': 'GET,POST,PATCH,OPTIONS', 'Content-Type': 'application/json; charset=utf-8' }
const notify = () => clients.forEach((client) => client.write(`data: ${JSON.stringify(state)}\n\n`))
const body = async (request) => {
  const chunks = []
  for await (const chunk of request) chunks.push(chunk)
  return JSON.parse(Buffer.concat(chunks).toString() || '{}')
}

createServer(async (request, response) => {
  if (request.method === 'OPTIONS') { response.writeHead(204, headers); return response.end() }
  const url = new URL(request.url, 'http://localhost')
  if (url.pathname === '/health') { response.writeHead(200, headers); return response.end(JSON.stringify({ status: 'UP', mode: 'demo' })) }
  if (url.pathname === '/events') {
    response.writeHead(200, { ...headers, 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', Connection: 'keep-alive' })
    response.write(`data: ${JSON.stringify(state)}\n\n`)
    clients.add(response)
    request.on('close', () => clients.delete(response))
    return
  }
  if (url.pathname === '/state' && request.method === 'GET') { response.writeHead(200, headers); return response.end(JSON.stringify(state)) }
  if (url.pathname === '/reset' && request.method === 'POST') {
    state = { ...state, trips: state.trips.map((item) => item.id === trip.id ? { ...trip } : item), alerts: state.alerts.filter((alert) => alert.id === 'alert-seed-1'), helpRequests: [], updatedAt: new Date().toISOString() }
    notify(); response.writeHead(200, headers); return response.end(JSON.stringify(state))
  }
  if (url.pathname.match(/^\/trips\/[^/]+\/platform$/) && request.method === 'PATCH') {
    const input = await body(request); const id = url.pathname.split('/')[2]; const current = state.trips.find((item) => item.id === id)
    if (!current) { response.writeHead(404, headers); return response.end(JSON.stringify({ message: 'Viagem não encontrada' })) }
    const createdAt = new Date().toISOString()
    state = { ...state, trips: state.trips.map((item) => item.id === id ? { ...item, previousPlatform: item.platform, platform: input.platform, status: 'attention' } : item), alerts: [{ id: `alert-${Date.now()}`, tripId: id, severity: 'critical', platform: input.platform, message: input.message || `Plataforma alterada de ${current.platform} para ${input.platform}.`, createdAt }, ...state.alerts], updatedAt: createdAt }
    notify(); response.writeHead(200, headers); return response.end(JSON.stringify(state))
  }
  if (url.pathname === '/help-requests' && request.method === 'POST') {
    const input = await body(request); const exists = state.helpRequests.some((item) => item.status !== 'resolved' && item.totemId === input.totemId)
    if (!exists) state = { ...state, helpRequests: [{ id: `help-${Date.now()}`, tripId: trip.id, totemId: input.totemId, pointLabel: 'Entrada A · Piso térreo', category: input.category, status: 'waiting', createdAt: new Date().toISOString() }, ...state.helpRequests], updatedAt: new Date().toISOString() }
    notify(); response.writeHead(201, headers); return response.end(JSON.stringify(state))
  }
  if (url.pathname.match(/^\/help-requests\/[^/]+$/) && request.method === 'PATCH') {
    const input = await body(request); const id = url.pathname.split('/')[2]
    state = { ...state, helpRequests: state.helpRequests.map((item) => item.id === id ? { ...item, status: input.status } : item), updatedAt: new Date().toISOString() }
    notify(); response.writeHead(200, headers); return response.end(JSON.stringify(state))
  }
  response.writeHead(404, headers); response.end(JSON.stringify({ message: 'Rota não encontrada' }))
}).listen(3100, () => console.log('Servidor demo em http://localhost:3100'))
