import { describe, expect, it } from 'vitest'
import { addHelpRequest, seedState, setHelpStatus, updatePlatform } from './index'

describe('demo state', () => {
  it('publishes a platform change without mutating the original seed', () => {
    const original = seedState()
    const next = updatePlatform(original, 'trip-sp-rio-001', '21')
    expect(original.trips[0].platform).toBe('18')
    expect(next.trips[0]).toMatchObject({ platform: '21', previousPlatform: '18', status: 'attention' })
    expect(next.alerts[0].severity).toBe('critical')
  })

  it('creates only one active help request per totem and resolves it', () => {
    const requested = addHelpRequest(seedState(), 'mobilidade', 'tiete-entrada-a')
    const duplicate = addHelpRequest(requested, 'seguranca', 'tiete-entrada-a')
    expect(duplicate.helpRequests).toHaveLength(1)
    const resolved = setHelpStatus(duplicate, duplicate.helpRequests[0].id, 'resolved')
    expect(resolved.helpRequests[0].status).toBe('resolved')
  })
})
