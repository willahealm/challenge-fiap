/** API v1 wire types. All timestamps are ISO-8601 UTC strings. */
export type Role = 'PASSENGER' | 'OPERATOR' | 'TOTEM' | 'KIOSK';
export interface User { id: string; name: string; role: Role }
export interface AuthResponse { accessToken: string; tokenType: 'Bearer'; expiresAt: string; user: User }
export interface Trip { id: string; terminalId: string; origin: string; destination: string; departureAt: string; carrier: string; platform: string; status: 'ON_TIME' | 'DELAYED' | 'BOARDING'; activeJourneys: number }
export interface Journey { id: string; tripId: string; userId: string; currentPointId: string; stage: 'PREPARING' | 'AT_TERMINAL' | 'COMPLETED'; checklist: Record<'document' | 'ticket' | 'luggage' | 'departureTime', boolean>; updatedAt: string }
export interface Step { pointId: string; instruction: string; distanceMeters: number; headingDegrees: number }
export interface Route { id: string; terminalId: string; fromPointId: string; toPointId: string; accessible: boolean; distanceMeters: number; headingDegrees: number; steps: Step[] }
export type AlertType = 'INFO' | 'PLATFORM_CHANGE' | 'DELAY' | 'BOARDING';
export type Severity = 'INFO' | 'WARNING' | 'CRITICAL';
export interface Alert { id: string; tripId: string; type: AlertType; severity: Severity; message: string; previousPlatform: string; platform: string; createdAt: string }
export interface JourneyView { journey: Journey; trip: Trip; route: Route; alerts: Alert[] }
export interface HandoffResponse { token: string; code: string; expiresAt: string }
export interface ConsumeResponse extends JourneyView { accessToken: string; expiresAt: string }
export type HelpCategory = 'MOBILITY' | 'VISION_HEARING' | 'TRIP_INFO' | 'SECURITY';
export interface HelpRequest { id: string; journeyId: string | null; tripId: string | null; totemId: string; pointId: string; category: HelpCategory; status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'; createdAt: string }
export interface Feedback { id: string; journeyId: string; rating: number; tags: string[]; comment: string; createdAt: string }
export interface Point { id: string; terminalId: string; code: string; name: string; kind: string }
export interface ApiError { code: string; message: string; timestamp: string }
export interface LoginRequest { email: string; password: string }
export interface AlertRequest { type: AlertType; severity: Severity; message: string; platform?: '18' | '21' }
export interface ChecklistRequest { checklist: Partial<Journey['checklist']> }
export interface CheckpointRequest { code: string }
export interface HelpRequestBody { journeyId?: string; category: HelpCategory }
export interface FeedbackRequest { journeyId: string; rating: number; tags?: string[]; comment?: string }
