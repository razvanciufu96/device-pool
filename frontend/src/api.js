import { currentUser } from './session'

/** Error carrying the backend's problem details (status, detail, and conflicts on 409). */
export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || `Request failed (${status})`)
    this.status = status
    this.conflicts = problem?.conflicts || []
  }
}

async function request(method, path, body) {
  const headers = {}
  if (currentUser.value) headers['X-User-Id'] = currentUser.value.id
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  const data = res.status === 204 ? null : await res.json().catch(() => null)
  if (!res.ok) throw new ApiError(res.status, data)
  return data
}

export const api = {
  users: () => request('GET', '/users'),
  devices: (from, to) => {
    const params = from && to ? `?${new URLSearchParams({ from: from.toISOString(), to: to.toISOString() })}` : ''
    return request('GET', `/devices${params}`)
  },
  myReservations: () => request('GET', '/reservations/mine'),
  reserve: (deviceId, start, end) =>
    request('POST', '/reservations', { deviceId, start: start.toISOString(), end: end.toISOString() }),
  cancel: (id) => request('DELETE', `/reservations/${id}`),
  reportDamage: (deviceId, description) => request('POST', `/devices/${deviceId}/damage-reports`, { description }),
  damageReports: () => request('GET', '/damage-reports'),
  resolveDamage: (id) => request('POST', `/damage-reports/${id}/resolve`),
}
