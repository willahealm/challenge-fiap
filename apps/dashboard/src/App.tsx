import { useEffect, useMemo, useState } from 'react'
import { helpCategoryLabels, relativeTime, type DemoState, type HelpRequest, type Trip } from '@embarque-facil/web-core'
import { changeHelpStatus, loadState, publishPlatform, resetDemo, signIn, subscribe, type Session } from './api'
import { Icon } from './icons'

type View = 'overview' | 'trips' | 'alerts' | 'help'

function Logo({ compact = false }: { compact?: boolean }) {
  return <div className="logo"><span className="logo-mark"><span /></span>{!compact && <span>Embarque <b>Fácil</b><small>OPERAÇÃO</small></span>}</div>
}

function Login({ onLogin }: { onLogin: (session: Session) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (demo = false) => {
    setBusy(true); setError('')
    try { onLogin(await signIn(demo ? 'operador@demo.local' : email, demo ? 'Operador123!' : password)) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.') }
    finally { setBusy(false) }
  }
  return <main className="login-page">
    <section className="login-story">
      <Logo />
      <div><span className="eyebrow light">CENTRAL OPERACIONAL</span><h1>A jornada fica simples quando a operação enxerga o todo.</h1><p>Monitore viagens, publique mudanças e responda pedidos de ajuda sem expor dados do passageiro.</p></div>
      <div className="story-card"><Icon name="spark" size={24}/><div><strong>Demonstração sincronizada</strong><span>Dashboard e totem compartilham o mesmo estado local.</span></div></div>
    </section>
    <section className="login-panel"><form onSubmit={(event) => { event.preventDefault(); void submit() }}>
      <span className="eyebrow">ACESSO RESTRITO</span><h2>Olá, operação.</h2><p>Entre com uma conta do time de operadores.</p>
      <label>E-mail<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="operador@empresa.com" required/></label>
      <label>Senha<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Sua senha" required/></label>
      {error && <div className="form-error" role="alert">{error}</div>}
      <button className="primary-button" disabled={busy}>{busy ? 'Entrando…' : 'Entrar no painel'}<Icon name="arrow"/></button>
      <button type="button" className="demo-button" disabled={busy} onClick={() => void submit(true)}><Icon name="spark"/>Entrar no modo demonstração</button>
      <small className="privacy-note">Ambiente protegido · Acesso registrado para auditoria</small>
    </form></section>
  </main>
}

const statusLabel = { 'on-time': 'No horário', attention: 'Atenção', boarding: 'Embarcando' }

function TripTable({ trips, onSelect }: { trips: Trip[]; onSelect: (trip: Trip) => void }) {
  if (!trips.length) return <div className="empty"><span className="empty-icon"><Icon name="search" size={28}/></span><h3>Nenhuma viagem encontrada</h3><p>Ajuste a busca ou o filtro de status.</p></div>
  return <div className="table-wrap"><table><thead><tr><th>Horário</th><th>Viagem</th><th>Plataforma</th><th>Status</th><th>Jornadas</th><th /></tr></thead><tbody>{trips.map((trip) => <tr key={trip.id}>
    <td><strong>{trip.departureTime}</strong><small>{trip.code}</small></td>
    <td><strong>{trip.origin} <span className="route-arrow">→</span> {trip.destination}</strong><small>{trip.company}</small></td>
    <td><span className="platform-number">{trip.platform}</span></td>
    <td><span className={`status ${trip.status}`}><i/>{statusLabel[trip.status]}</span></td>
    <td><span className="journeys"><span className="avatar-stack">●●●</span>{trip.activeJourneys}</span></td>
    <td><button className="row-action" onClick={() => onSelect(trip)}>Gerenciar <Icon name="arrow" size={16}/></button></td>
  </tr>)}</tbody></table></div>
}

function PlatformModal({ trip, onClose, onConfirm }: { trip: Trip; onClose: () => void; onConfirm: (platform: string, message: string) => Promise<void> }) {
  const [platform, setPlatform] = useState(trip.platform === '18' ? '21' : '')
  const [message, setMessage] = useState(`Atenção: o embarque mudou para a plataforma ${trip.platform === '18' ? '21' : ''}. Siga as placas do setor B.`)
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const submit = async () => { setBusy(true); await onConfirm(platform, message); setBusy(false) }
  return <div className="modal-backdrop" role="presentation" onMouseDown={onClose}><section className="modal" role="dialog" aria-modal="true" aria-labelledby="platform-title" onMouseDown={(event) => event.stopPropagation()}>
    <button className="close" onClick={onClose} aria-label="Fechar">×</button><span className="modal-icon"><Icon name="bell"/></span>
    <span className="eyebrow">ALERTA OPERACIONAL</span><h2 id="platform-title">Alterar plataforma</h2><p>{trip.departureTime} · {trip.origin} → {trip.destination}</p>
    <div className="platform-compare"><div><small>ATUAL</small><strong>{trip.platform}</strong></div><Icon name="arrow" size={26}/><label><small>NOVA</small><input value={platform} onChange={(event) => { const next = event.target.value.replace(/\D/g, '').slice(0, 2); setPlatform(next); setMessage(`Atenção: o embarque mudou para a plataforma ${next}. Siga as placas do setor B.`) }} inputMode="numeric" aria-label="Nova plataforma"/></label></div>
    <label className="field">Mensagem ao passageiro<textarea value={message} onChange={(event) => setMessage(event.target.value)} rows={3}/><small>{message.length}/160</small></label>
    {!confirming ? <button className="primary-button danger" disabled={!platform || platform === trip.platform} onClick={() => setConfirming(true)}>Revisar alerta <Icon name="arrow"/></button> : <div className="confirm-box"><strong>Publicar agora para {trip.activeJourneys} jornadas?</strong><p>A mudança aparecerá imediatamente no totem e nos canais conectados.</p><div><button className="secondary-button" onClick={() => setConfirming(false)}>Voltar</button><button className="primary-button danger" disabled={busy} onClick={() => void submit()}>{busy ? 'Publicando…' : 'Publicar mudança'}</button></div></div>}
  </section></div>
}

function HelpCard({ request, onStatus }: { request: HelpRequest; onStatus: (status: 'assigned' | 'resolved') => void }) {
  return <article className={`help-card ${request.status}`}><div className="help-card-top"><span className="help-symbol"><Icon name="help"/></span><div><strong>{helpCategoryLabels[request.category]}</strong><span>{request.pointLabel}</span></div><span className="help-time">{relativeTime(request.createdAt)}</span></div>
    <div className="help-context"><span><small>TOTEM</small>{request.totemId}</span><span><small>VIAGEM</small>EF4821 · Rio de Janeiro</span></div>
    {request.status === 'waiting' && <button className="primary-button small" onClick={() => onStatus('assigned')}>Assumir atendimento</button>}
    {request.status === 'assigned' && <button className="secondary-button full" onClick={() => onStatus('resolved')}><Icon name="check"/>Marcar como resolvido</button>}
    {request.status === 'resolved' && <span className="resolved-label"><Icon name="check"/>Atendimento concluído</span>}
  </article>
}

function App() {
  const [session, setSession] = useState<Session | null>(() => { const raw = sessionStorage.getItem('ef:operator'); return raw ? JSON.parse(raw) : null })
  const [state, setState] = useState<DemoState | null>(null)
  const [loading, setLoading] = useState(true)
  const [offline, setOffline] = useState(false)
  const [error, setError] = useState('')
  const [view, setView] = useState<View>('overview')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('all')
  const [selectedTrip, setSelectedTrip] = useState<Trip | null>(null)
  const [toast, setToast] = useState('')

  const refresh = async () => {
    if (!session) return
    setLoading(true); setError('')
    try { const result = await loadState(session.accessToken); setState(result.state); setOffline(result.offline) }
    catch { setError('Não foi possível carregar a operação.'); }
    finally { setLoading(false) }
  }
  useEffect(() => { void refresh() }, [session])
  useEffect(() => session ? subscribe(setState) : undefined, [session])
  useEffect(() => { if (!toast) return; const timer = setTimeout(() => setToast(''), 3500); return () => clearTimeout(timer) }, [toast])
  const login = (next: Session) => { sessionStorage.setItem('ef:operator', JSON.stringify(next)); setSession(next) }
  const logout = () => { sessionStorage.removeItem('ef:operator'); setSession(null); setState(null) }
  const filtered = useMemo(() => (state?.trips || []).filter((trip) => {
    const term = `${trip.origin} ${trip.destination} ${trip.code} ${trip.company}`.toLowerCase()
    return term.includes(search.toLowerCase()) && (status === 'all' || trip.status === status)
  }), [state, search, status])
  if (!session) return <Login onLogin={login}/>

  const waiting = state?.helpRequests.filter((request) => request.status === 'waiting').length || 0
  const nav = [{ id: 'overview', label: 'Visão geral', icon: 'grid' }, { id: 'trips', label: 'Viagens', icon: 'bus' }, { id: 'alerts', label: 'Alertas', icon: 'bell' }, { id: 'help', label: 'Pedidos de ajuda', icon: 'help', badge: waiting }]
  const confirmPlatform = async (platform: string, message: string) => {
    if (!state || !selectedTrip) return
    const next = await publishPlatform(state, selectedTrip.id, platform, message, session.accessToken)
    setState(next); setSelectedTrip(null); setToast(`Plataforma ${platform} publicada para ${selectedTrip.activeJourneys} jornadas.`)
  }
  const helpStatus = async (id: string, nextStatus: 'assigned' | 'resolved') => {
    if (!state) return
    setState(await changeHelpStatus(state, id, nextStatus, session.accessToken)); setToast(nextStatus === 'assigned' ? 'Atendimento assumido.' : 'Pedido resolvido.')
  }

  return <div className="app-shell">
    <aside><Logo/><nav>{nav.map((item) => <button key={item.id} className={view === item.id ? 'active' : ''} onClick={() => setView(item.id as View)}><Icon name={item.icon}/><span>{item.label}</span>{item.badge ? <b>{item.badge}</b> : null}</button>)}</nav>
      <div className="aside-bottom"><div className="terminal-status"><i/><span><strong>Terminal Tietê</strong><small>Operação online</small></span></div><button onClick={logout}><Icon name="logout"/><span>Sair</span></button></div>
    </aside>
    <main className="dashboard-main">
      <header><div><span className="eyebrow">DOMINGO, 13 DE SETEMBRO</span><h1>{view === 'help' ? 'Pedidos de ajuda' : view === 'alerts' ? 'Alertas operacionais' : view === 'trips' ? 'Viagens do dia' : 'Boa noite, Marina.'}</h1><p>{view === 'overview' ? 'Aqui está o pulso da operação no Terminal Tietê.' : 'Acompanhe e responda em tempo real.'}</p></div><div className="header-actions">{offline && <span className="offline-pill">Modo offline</span>}<button className="icon-button" onClick={() => void refresh()} aria-label="Atualizar"><Icon name="refresh"/></button><span className="operator-avatar">MC</span></div></header>
      {loading ? <div className="skeleton-grid" aria-label="Carregando"><i/><i/><i/><i/></div> : error ? <div className="error-state"><Icon name="refresh" size={30}/><h2>Perdemos a conexão</h2><p>{error}</p><button className="primary-button small" onClick={() => void refresh()}>Tentar novamente</button></div> : state && <>
        {view === 'overview' && <>
          <section className="metrics"><article><span className="metric-icon purple"><Icon name="bus"/></span><div><small>VIAGENS MONITORADAS</small><strong>{state.trips.length}</strong><span>Hoje no terminal</span></div></article><article><span className="metric-icon orange"><Icon name="bell"/></span><div><small>ALERTAS ATIVOS</small><strong>{state.alerts.length}</strong><span><i className="up"/> 1 crítico agora</span></div></article><article><span className="metric-icon teal"><Icon name="map"/></span><div><small>PASSAGEIROS ASSISTIDOS</small><strong>{state.trips.reduce((sum, trip) => sum + trip.activeJourneys, 0)}</strong><span>Jornadas em andamento</span></div></article><article><span className="metric-icon yellow"><Icon name="spark"/></span><div><small>CSAT DA JORNADA</small><strong>{state.csat}<em>/5</em></strong><span>Hipótese em validação</span></div></article></section>
          <div className="content-grid"><section className="panel trips-panel"><div className="panel-head"><div><h2>Próximas partidas</h2><p>Viagens com embarque nas próximas horas</p></div><button onClick={() => setView('trips')}>Ver todas <Icon name="arrow" size={16}/></button></div><TripTable trips={state.trips} onSelect={setSelectedTrip}/></section>
          <section className="panel help-panel"><div className="panel-head"><div><h2>Ajuda solicitada</h2><p>Chamados vindos dos totens</p></div>{waiting > 0 && <span className="live"><i/>AO VIVO</span>}</div>{state.helpRequests.length ? state.helpRequests.slice(0, 2).map((request) => <HelpCard key={request.id} request={request} onStatus={(next) => void helpStatus(request.id, next)}/>) : <div className="quiet-state"><span><Icon name="check"/></span><h3>Nenhum pedido pendente</h3><p>A operação está tranquila por aqui.</p></div>}</section></div>
        </>}
        {view === 'trips' && <section className="panel page-panel"><div className="toolbar"><label><Icon name="search"/><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar rota, código ou viação"/></label><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="all">Todos os status</option><option value="on-time">No horário</option><option value="boarding">Embarcando</option><option value="attention">Atenção</option></select></div><TripTable trips={filtered} onSelect={setSelectedTrip}/></section>}
        {view === 'help' && <section className="help-list">{state.helpRequests.length ? state.helpRequests.map((request) => <HelpCard key={request.id} request={request} onStatus={(next) => void helpStatus(request.id, next)}/>) : <div className="panel quiet-state large"><span><Icon name="check"/></span><h2>Nenhum pedido pendente</h2><p>Quando alguém pedir apoio em um totem, o contexto aparecerá aqui.</p></div>}</section>}
        {view === 'alerts' && <section className="panel alerts-list"><div className="panel-head"><div><h2>Histórico de alertas</h2><p>Comunicações publicadas nesta operação</p></div></div>{state.alerts.map((alert) => { const trip = state.trips.find((item) => item.id === alert.tripId); return <article key={alert.id}><span className={`alert-symbol ${alert.severity}`}><Icon name="bell"/></span><div><strong>{alert.message}</strong><p>{trip?.departureTime} · {trip?.origin} → {trip?.destination}</p></div><time>{relativeTime(alert.createdAt)}</time></article> })}</section>}
        <button className="reset-demo" onClick={async () => { setState(await resetDemo()); setToast('Demonstração restaurada para a plataforma 18.') }}><Icon name="refresh" size={16}/>Restaurar demo</button>
      </>}
    </main>
    {selectedTrip && <PlatformModal trip={selectedTrip} onClose={() => setSelectedTrip(null)} onConfirm={confirmPlatform}/>} {toast && <div className="toast"><span><Icon name="check"/></span>{toast}</div>}
  </div>
}

export default App
