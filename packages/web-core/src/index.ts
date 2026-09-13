export type TripStatus = 'on-time' | 'attention' | 'boarding'
export type HelpCategory = 'mobilidade' | 'visao-audicao' | 'informacao' | 'seguranca'
export type HelpStatus = 'waiting' | 'assigned' | 'resolved'

export interface Trip {
  id: string
  code: string
  departureTime: string
  origin: string
  destination: string
  company: string
  platform: string
  previousPlatform?: string
  status: TripStatus
  activeJourneys: number
  terminal: string
  gateClosesAt: string
}

export interface OperationalAlert {
  id: string
  tripId: string
  severity: 'info' | 'warning' | 'critical'
  message: string
  platform?: string
  createdAt: string
}

export interface HelpRequest {
  id: string
  tripId: string
  totemId: string
  pointLabel: string
  category: HelpCategory
  status: HelpStatus
  createdAt: string
}

export interface DemoState {
  trips: Trip[]
  alerts: OperationalAlert[]
  helpRequests: HelpRequest[]
  csat: number
  updatedAt: string
}

export const helpCategoryLabels: Record<HelpCategory, string> = {
  mobilidade: 'Apoio de mobilidade',
  'visao-audicao': 'Visão ou audição',
  informacao: 'Informação da viagem',
  seguranca: 'Segurança',
}

export const seedState = (): DemoState => ({
  trips: [
    {
      id: 'trip-sp-rio-001', code: 'EF4821', departureTime: '22:30', origin: 'São Paulo',
      destination: 'Rio de Janeiro', company: 'Viação Cometa', platform: '18', status: 'on-time',
      activeJourneys: 12, terminal: 'Terminal Tietê', gateClosesAt: '22:20',
    },
    {
      id: 'trip-sp-cps-002', code: 'EF1130', departureTime: '21:50', origin: 'São Paulo',
      destination: 'Campinas', company: 'Lirabus', platform: '07', status: 'boarding',
      activeJourneys: 7, terminal: 'Terminal Tietê', gateClosesAt: '21:40',
    },
    {
      id: 'trip-sp-bh-003', code: 'EF7204', departureTime: '23:10', origin: 'São Paulo',
      destination: 'Belo Horizonte', company: 'Gontijo', platform: '24', status: 'attention',
      activeJourneys: 4, terminal: 'Terminal Tietê', gateClosesAt: '23:00',
    },
  ],
  alerts: [
    {
      id: 'alert-seed-1', tripId: 'trip-sp-bh-003', severity: 'warning',
      message: 'Embarque previsto com 10 minutos de atraso.', createdAt: new Date(Date.now() - 18 * 60_000).toISOString(),
    },
  ],
  helpRequests: [],
  csat: 4.8,
  updatedAt: new Date().toISOString(),
})

export function updatePlatform(state: DemoState, tripId: string, platform: string, message?: string): DemoState {
  const current = state.trips.find((trip) => trip.id === tripId)
  if (!current) throw new Error('Viagem não encontrada')
  const createdAt = new Date().toISOString()
  return {
    ...state,
    trips: state.trips.map((trip) => trip.id === tripId ? {
      ...trip, previousPlatform: trip.platform, platform, status: 'attention',
    } : trip),
    alerts: [{
      id: `alert-${Date.now()}`, tripId, severity: 'critical', platform,
      message: message || `Plataforma alterada de ${current.platform} para ${platform}.`, createdAt,
    }, ...state.alerts],
    updatedAt: createdAt,
  }
}

export function addHelpRequest(state: DemoState, category: HelpCategory, totemId: string): DemoState {
  const createdAt = new Date().toISOString()
  const existing = state.helpRequests.find((request) => request.status !== 'resolved' && request.totemId === totemId)
  if (existing) return state
  return {
    ...state,
    helpRequests: [{
      id: `help-${Date.now()}`, tripId: 'trip-sp-rio-001', totemId,
      pointLabel: 'Entrada A · Piso térreo', category, status: 'waiting', createdAt,
    }, ...state.helpRequests],
    updatedAt: createdAt,
  }
}

export function setHelpStatus(state: DemoState, requestId: string, status: HelpStatus): DemoState {
  const updatedAt = new Date().toISOString()
  return {
    ...state,
    helpRequests: state.helpRequests.map((request) => request.id === requestId ? { ...request, status } : request),
    updatedAt,
  }
}

export function relativeTime(iso: string): string {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60_000))
  if (minutes < 1) return 'agora'
  if (minutes === 1) return 'há 1 min'
  return `há ${minutes} min`
}
