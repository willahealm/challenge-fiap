import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { helpCategoryLabels, type DemoState, type HelpCategory } from '@embarque-facil/web-core'
import { clearKioskSession, consumeCode, requestHelp, subscribeState, totemId, touchKioskSession } from './api'

type Screen = 'welcome' | 'code' | 'confirm' | 'route' | 'help' | 'done'

function speakInBrazilianPortuguese(text: string) {
  if (!('speechSynthesis' in window)) return
  const synthesizer = window.speechSynthesis
  synthesizer.cancel()

  let spoken = false
  const start = () => {
    if (spoken) return
    spoken = true
    const utterance = new SpeechSynthesisUtterance(text)
    const voices = synthesizer.getVoices()
    const brazilianVoices = voices.filter((voice) => voice.lang.replace('_', '-').toLowerCase() === 'pt-br')
    const portugueseVoices = voices.filter((voice) => voice.lang.toLowerCase().startsWith('pt'))
    utterance.voice = brazilianVoices.find((voice) => /francisca|google.*portugu[eê]s|maria|luciana/i.test(voice.name))
      || brazilianVoices.find((voice) => voice.localService)
      || brazilianVoices[0]
      || portugueseVoices.find((voice) => voice.localService)
      || portugueseVoices[0]
      || null
    utterance.lang = utterance.voice?.lang || 'pt-BR'
    utterance.rate = 0.92
    utterance.pitch = 1
    synthesizer.speak(utterance)
  }

  if (synthesizer.getVoices().length > 0) start()
  else {
    synthesizer.addEventListener('voiceschanged', start, { once: true })
    window.setTimeout(start, 400)
  }
}

function Icon({ name, size = 28 }: { name: string; size?: number }) {
  const icons: Record<string, ReactNode> = {
    arrow: <><path d="M5 12h14M13 6l6 6-6 6"/></>, back: <><path d="m15 18-6-6 6-6"/></>,
    scan: <><path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M8 12h8"/></>,
    ticket: <><path d="M3 7a2 2 0 0 0 2-2h14a2 2 0 0 0 2 2v3a2 2 0 0 0 0 4v3a2 2 0 0 0-2 2H5a2 2 0 0 0-2-2v-3a2 2 0 0 0 0-4Z"/><path d="M13 5v2M13 11v2M13 17v2"/></>,
    map: <><path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3Z"/><path d="M9 3v15M15 6v15"/></>,
    help: <><circle cx="12" cy="12" r="9"/><path d="M9.7 9a2.4 2.4 0 1 1 3.4 2.2c-.8.4-1.1.9-1.1 1.8M12 17h.01"/></>,
    access: <><circle cx="12" cy="5" r="2"/><path d="M5 8h14M12 7v6M8 21l4-8 4 8"/></>,
    sound: <><path d="M11 5 6 9H3v6h3l5 4ZM15 9a4 4 0 0 1 0 6M18 6a8 8 0 0 1 0 12"/></>,
    phone: <><rect x="6" y="2" width="12" height="20" rx="2"/><path d="M10 18h4"/></>,
    check: <path d="m5 12 4 4L19 6"/>, clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2"/></>,
    security: <><path d="M12 3 4 6v5c0 5 3.4 8.4 8 10 4.6-1.6 8-5 8-10V6Z"/><path d="m9 12 2 2 4-4"/></>,
    person: <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{icons[name]}</svg>
}

function Logo() { return <div className="k-logo"><span className="k-mark"><i/></span><span>Embarque <b>Fácil</b><small>TERMINAL TIETÊ</small></span></div> }

function Topbar({ accessibility, onAccessibility }: { accessibility: boolean; onAccessibility: () => void }) {
  return <header className="topbar"><Logo/><div><button onClick={onAccessibility} className={accessibility ? 'selected' : ''}><Icon name="access" size={21}/>Acessibilidade</button><button><span className="flag">🇧🇷</span>Português⌄</button></div></header>
}

function Welcome({ onStart, onAccessibility }: { onStart: () => void; onAccessibility: () => void }) {
  return <main className="welcome"><section className="welcome-copy"><span className="welcome-tag">TOTEM DE ORIENTAÇÃO</span><h1>Encontre sua plataforma <em>sem complicação.</em></h1><p>Conecte sua viagem e receba o caminho mais simples até o embarque.</p><button className="k-primary xl" onClick={onStart}>Começar agora <Icon name="arrow"/></button><small><Icon name="clock" size={16}/>Sua sessão será apagada automaticamente ao finalizar.</small></section>
    <section className="welcome-art" aria-hidden="true"><div className="halo h1"/><div className="halo h2"/><div className="pin p1"><i>18</i></div><div className="pin p2"><Icon name="ticket" size={24}/></div><div className="path-line"/><div className="art-card"><span><Icon name="map" size={36}/></span><strong>Seu caminho,<br/>passo a passo.</strong><small>Orientação clara dentro do terminal</small></div></section>
    <div className="welcome-options"><button onClick={onStart}><span><Icon name="scan"/></span><div><strong>Escanear QR do app</strong><small>Aponte o código para a câmera</small></div><Icon name="arrow" size={20}/></button><button onClick={onStart}><span><Icon name="ticket"/></span><div><strong>Digitar passagem</strong><small>Use o código da sua viagem</small></div><Icon name="arrow" size={20}/></button><button onClick={onAccessibility}><span><Icon name="access"/></span><div><strong>Rota acessível</strong><small>Opções sem escadas</small></div><Icon name="arrow" size={20}/></button></div>
  </main>
}

function CodeScreen({ onBack, onContinue }: { onBack: () => void; onContinue: (code: string) => Promise<void> }) {
  const [code, setCode] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async () => { setBusy(true); setError(''); try { await onContinue(code) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Código inválido.') } finally { setBusy(false) } }
  return <main className="center-screen"><button className="back-button" onClick={onBack}><Icon name="back" size={22}/>Voltar</button><section className="code-card"><span className="big-icon"><Icon name="ticket" size={42}/></span><span className="k-eyebrow">CONECTAR VIAGEM</span><h1>Digite o código da sua passagem</h1><p>Encontre o localizador no e-mail ou no aplicativo da sua passagem.</p><form onSubmit={(event) => { event.preventDefault(); void submit() }}><label>Código ou localizador<input autoFocus value={code} onChange={(event) => setCode(event.target.value.toUpperCase().replace(/[^A-Z0-9-]/g, '').slice(0, 20))} placeholder="Ex.: EF4821" autoComplete="off"/></label>{error && <div className="k-error" role="alert">{error}</div>}<button className="k-primary" disabled={code.length < 5 || busy}>{busy ? 'Buscando…' : 'Encontrar minha viagem'}<Icon name="arrow"/></button></form><button className="scan-option"><Icon name="scan"/>Prefiro escanear o QR do aplicativo</button><div className="demo-hint">Para a demonstração, use <button onClick={() => setCode('EF4821')}>EF4821</button></div></section></main>
}

function ConfirmScreen({ state, offline, onBack, onContinue }: { state: DemoState; offline: boolean; onBack: () => void; onContinue: () => void }) {
  const trip = state.trips[0]
  return <main className="center-screen"><button className="back-button" onClick={onBack}><Icon name="back" size={22}/>Não é esta</button><section className="confirm-card"><span className="success-icon"><Icon name="check" size={33}/></span><span className="k-eyebrow">VIAGEM ENCONTRADA {offline && '· OFFLINE'}</span><h1>Esta é a sua viagem?</h1><div className="trip-card"><div className="trip-route"><span><small>ORIGEM</small><strong>{trip.origin}</strong></span><div><i/><span>ÔNIBUS</span><i/></div><span><small>DESTINO</small><strong>{trip.destination}</strong></span></div><div className="trip-data"><span><small>HORÁRIO</small><strong>{trip.departureTime}</strong></span><span><small>VIAÇÃO</small><strong>{trip.company}</strong></span><span className="platform-data"><small>PLATAFORMA</small><strong>{trip.platform}</strong></span></div></div><p className="privacy"><Icon name="security" size={20}/>Exibimos apenas o necessário. Seus dados serão apagados deste totem ao encerrar.</p><button className="k-primary" onClick={onContinue}>Sim, orientar meu caminho <Icon name="arrow"/></button></section></main>
}

function RouteMap({ platform, accessible }: { platform: string; accessible: boolean }) {
  return <div className="route-map"><div className="map-grid"/><div className="building b1">Entrada A</div><div className="building b2">Lojas</div><div className="building b3">Setor B</div><div className="building b4">Plataforma {platform}</div><svg viewBox="0 0 600 330" preserveAspectRatio="none"><path d="M70 270 C150 245, 150 170, 245 177 S345 235, 400 170 S450 85,530 75"/><path className="walk" d="M70 270 C150 245, 150 170, 245 177 S345 235, 400 170 S450 85,530 75"/></svg><span className="start-dot"><Icon name="person" size={19}/></span><span className="end-pin">{platform}</span>{accessible && <span className="accessible-map"><Icon name="access" size={18}/>Rota sem escadas</span>}</div>
}

function RouteScreen({ state, accessible, onHelp, onEnd }: { state: DemoState; accessible: boolean; onHelp: () => void; onEnd: () => void }) {
  const trip = state.trips[0]
  const alert = state.alerts.find((item) => item.tripId === trip.id && item.platform === trip.platform)
  const [showAlert, setShowAlert] = useState(Boolean(trip.previousPlatform && alert))
  const steps = accessible ? ['Siga em frente pelo corredor principal', 'Use o elevador ao lado da cafeteria', `Vire à direita para a plataforma ${trip.platform}`] : ['Siga em frente por cerca de 40 metros', 'Suba pela escada rolante do setor B', `Vire à direita para a plataforma ${trip.platform}`]
  useEffect(() => { if (trip.previousPlatform) setShowAlert(true) }, [trip.platform, trip.previousPlatform])
  const speak = () => speakInBrazilianPortuguese(`Siga em frente. ${steps.join('. ')}.`)
  return <main className="route-screen"><div className="route-head"><div><span className="k-eyebrow">ORIENTAÇÃO ATIVA</span><h1>Você está a caminho da plataforma <b>{trip.platform}</b></h1><p>Saída às {trip.departureTime} · Portão fecha às {trip.gateClosesAt}</p></div><div className="route-actions"><button onClick={speak}><Icon name="sound" size={22}/>Ouvir instruções</button><button className="help-action" onClick={onHelp}><Icon name="help" size={22}/>Pedir ajuda</button></div></div><div className="route-layout"><section className="map-panel"><RouteMap platform={trip.platform} accessible={accessible}/><div className="map-caption"><span className="pulse-dot"/><div><strong>Você está aqui</strong><small>Totem Entrada A · Piso térreo</small></div><div className="estimate"><small>DISTÂNCIA</small><strong>~ 4 min</strong></div></div></section><section className="steps-panel"><span className="k-eyebrow">PASSO A PASSO</span><h2>Um caminho simples até o embarque</h2><ol>{steps.map((step, index) => <li key={step} className={index === 0 ? 'current' : ''}><span>{index + 1}</span><div><strong>{step}</strong><small>{index === 0 ? 'aprox. 40 m' : index === 1 ? 'setor B' : 'destino final'}</small></div>{index === 0 && <em>AGORA</em>}</li>)}</ol><button className="phone-button"><Icon name="phone" size={22}/><div><strong>Continuar no celular</strong><small>Gere um QR para levar a rota</small></div><Icon name="arrow" size={18}/></button><button className="end-link" onClick={onEnd}>Encerrar e apagar meus dados</button></section></div>
    {showAlert && trip.previousPlatform && <div className="critical-backdrop"><section className="critical-modal"><span className="alert-badge">MUDANÇA IMPORTANTE</span><h2>Sua plataforma mudou</h2><p>{alert?.message || 'A operação atualizou o local do seu embarque.'}</p><div className="change"><div><small>ANTES</small><strong>{trip.previousPlatform}</strong></div><Icon name="arrow" size={34}/><div className="new"><small>AGORA</small><strong>{trip.platform}</strong></div></div><div className="new-step"><span>1</span><div><small>PRIMEIRO PASSO</small><strong>Siga em frente até o setor B. Atualizamos sua rota.</strong></div></div><button className="k-primary alert-confirm" onClick={() => setShowAlert(false)}>Entendi, mostrar nova rota <Icon name="arrow"/></button></section></div>}
  </main>
}

function HelpScreen({ onBack, onSelect, sent, status }: { onBack: () => void; onSelect: (category: HelpCategory) => void; sent: HelpCategory | null; status?: 'waiting' | 'assigned' | 'resolved' }) {
  const options: Array<{ id: HelpCategory; icon: string; detail: string }> = [{ id: 'mobilidade', icon: 'access', detail: 'Preciso de apoio para me deslocar' }, { id: 'visao-audicao', icon: 'eye', detail: 'Preciso de orientação adaptada' }, { id: 'informacao', icon: 'ticket', detail: 'Tenho dúvida sobre a viagem' }, { id: 'seguranca', icon: 'security', detail: 'Preciso falar com a equipe agora' }]
  if (sent) return <main className="center-screen"><section className="help-sent"><span className="success-icon"><Icon name="check" size={40}/></span><span className="k-eyebrow">PEDIDO ENVIADO</span><h1>{status === 'assigned' ? 'Um atendente assumiu seu chamado.' : status === 'resolved' ? 'Atendimento concluído.' : 'A operação já recebeu seu chamado.'}</h1><p>Enviamos a categoria <strong>{helpCategoryLabels[sent]}</strong>, este totem e sua viagem. Aguarde próximo a este ponto.</p><div className="waiting-box">{status === 'waiting' || !status ? <span className="loader"/> : <span className="support-ready"><Icon name="person" size={21}/></span>}<div><strong>{status === 'assigned' ? 'Atendente a caminho' : status === 'resolved' ? 'Chamado finalizado pela operação' : 'Buscando um atendente'}</strong><small>{status === 'assigned' ? 'Permaneça próximo ao totem para receber apoio.' : 'Você pode continuar vendo a rota enquanto aguarda.'}</small></div></div><button className="k-primary" onClick={onBack}>Voltar para a rota <Icon name="arrow"/></button></section></main>
  return <main className="center-screen"><button className="back-button" onClick={onBack}><Icon name="back" size={22}/>Voltar para a rota</button><section className="help-choice"><span className="k-eyebrow">APOIO NO TERMINAL</span><h1>Como podemos ajudar?</h1><p>Escolha uma opção. A equipe receberá este totem e sua viagem, sem dados pessoais desnecessários.</p><div className="help-options">{options.map((option) => <button key={option.id} onClick={() => onSelect(option.id)}><span><Icon name={option.icon} size={29}/></span><div><strong>{helpCategoryLabels[option.id]}</strong><small>{option.detail}</small></div><Icon name="arrow" size={22}/></button>)}</div></section></main>
}

function Done({ onRestart }: { onRestart: () => void }) { return <main className="center-screen"><section className="done-card"><span className="success-icon"><Icon name="check" size={40}/></span><h1>Sessão encerrada.</h1><p>Seus dados foram apagados deste totem. Boa viagem!</p><button className="k-primary" onClick={onRestart}>Voltar ao início</button></section></main> }

export default function App() {
  const [screen, setScreen] = useState<Screen>('welcome')
  const [state, setState] = useState<DemoState | null>(null)
  const [offline, setOffline] = useState(false)
  const [accessible, setAccessible] = useState(false)
  const [helpSent, setHelpSent] = useState<HelpCategory | null>(null)
  const [idle, setIdle] = useState(30)
  const active = !['welcome', 'done'].includes(screen)
  const end = () => { void clearKioskSession(); setState(null); setHelpSent(null); setScreen('done'); setIdle(30); setTimeout(() => setScreen('welcome'), 3500) }
  useEffect(() => state ? subscribeState(setState, end) : undefined, [Boolean(state)])
  useEffect(() => {
    if (!active) return
    const reset = () => { setIdle(30); void touchKioskSession() }
    const events = ['pointerdown', 'keydown'] as const
    events.forEach((name) => window.addEventListener(name, reset))
    const interval = setInterval(() => setIdle((value) => { if (value <= 1) { setTimeout(end, 0); return 30 } return value - 1 }), 1000)
    return () => { events.forEach((name) => window.removeEventListener(name, reset)); clearInterval(interval) }
  }, [active])
  const warning = active && idle <= 8
  const trip = useMemo(() => state?.trips[0], [state])
  const helpStatus = state?.helpRequests.find((request) => request.totemId === totemId)?.status
  return <div className={`kiosk ${accessible ? 'accessible' : ''}`}><Topbar accessibility={accessible} onAccessibility={() => setAccessible((value) => !value)}/>{screen === 'welcome' && <Welcome onStart={() => setScreen('code')} onAccessibility={() => setAccessible((value) => !value)}/>} {screen === 'code' && <CodeScreen onBack={() => setScreen('welcome')} onContinue={async (code) => { const result = await consumeCode(code); setState(result.state); setOffline(result.offline); setScreen('confirm') }}/>} {screen === 'confirm' && state && <ConfirmScreen state={state} offline={offline} onBack={() => { setState(null); setScreen('code') }} onContinue={() => setScreen('route')}/>} {screen === 'route' && state && trip && <RouteScreen state={state} accessible={accessible} onHelp={() => setScreen('help')} onEnd={end}/>} {screen === 'help' && state && <HelpScreen sent={helpSent} status={helpStatus} onBack={() => setScreen('route')} onSelect={async (category) => { setState(await requestHelp(state, category)); setHelpSent(category) }}/>} {screen === 'done' && <Done onRestart={() => setScreen('welcome')}/>}<footer><span><i/>Totem {totemId} · Entrada A</span><span>Privacidade protegida · sessão temporária</span></footer>{warning && <div className="idle-warning"><Icon name="clock" size={24}/><div><strong>Sessão quase encerrando</strong><span>Toque na tela para continuar.</span></div><b>{idle}</b></div>}</div>
}
