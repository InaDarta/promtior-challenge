const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

const TOKEN_STORAGE_KEY = 'promtior.token'
const USERNAME_STORAGE_KEY = 'promtior.username'

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function getToken() {
  return sessionStorage.getItem(TOKEN_STORAGE_KEY)
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY)
}

export function getStoredUsername() {
  return sessionStorage.getItem(USERNAME_STORAGE_KEY)
}

export async function apiFetch(path, options = {}) {
  const token = getToken()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
    ...options,
  })

  if (!response.ok) {
    throw new ApiError(`Request to ${path} failed with status ${response.status}`, response.status)
  }

  if (response.status === 204) return null

  return response.json()
}

/**
 * El backend devuelve el JWT en el cuerpo de la respuesta (no en una cookie): se guarda en
 * `sessionStorage` junto con el usuario y viaja como `Authorization: Bearer` en cada request
 * siguiente. No hay endpoint de sesión (`/api/me`): la identidad se conoce porque acabamos de
 * loguearnos con ella.
 */
export async function login(username, password) {
  const { token } = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  sessionStorage.setItem(TOKEN_STORAGE_KEY, token)
  sessionStorage.setItem(USERNAME_STORAGE_KEY, username)
}

export function logout() {
  clearToken()
  sessionStorage.removeItem(USERNAME_STORAGE_KEY)
}

export function getRoomSchedule(room, start, end) {
  return apiFetch(`/rooms/${room}/schedule?start=${start}&end=${end}`)
}

/**
 * Parsea un stream SSE de a un evento por vez: junta bytes hasta encontrar el separador `\n\n` de
 * cada evento, y de sus líneas `event:`/`data:` arma `{ event, data }` (varias líneas `data:`
 * seguidas se juntan con `\n`, tal como indica la especificación).
 */
async function* parseSseEvents(reader) {
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) return
    buffer += decoder.decode(value, { stream: true })

    let boundary
    while ((boundary = buffer.indexOf('\n\n')) !== -1) {
      yield parseSseEvent(buffer.slice(0, boundary))
      buffer = buffer.slice(boundary + 2)
    }
  }
}

function parseSseEvent(rawEvent) {
  let event = 'message'
  const dataLines = []
  for (const line of rawEvent.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart())
    }
  }
  return { event, data: dataLines.join('\n') }
}

/**
 * Envía un mensaje a `/chat/stream` y va invocando `onToken` por cada fragmento parcial y
 * `onDone` con el texto final completo. Un evento `error` (ver ChatController#chatStream) tira un
 * `Error` común -- no un `ApiError`, porque no es un fallo HTTP -- con el mensaje en español que ya
 * viene listo para mostrar.
 *
 * Se usa `fetch` en vez de `EventSource` a propósito: `EventSource` no permite mandar el header
 * `Authorization`, y poner el token como query param lo expondría en logs y en el historial del
 * navegador (ver ADR 0010).
 */
export async function streamChatMessage(message, { onToken, onDone }) {
  const token = getToken()
  const response = await fetch(`${API_BASE_URL}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
    },
    body: JSON.stringify({ message }),
  })

  if (!response.ok) {
    throw new ApiError(`Request to /chat/stream failed with status ${response.status}`, response.status)
  }

  const reader = response.body.getReader()
  for await (const { event, data } of parseSseEvents(reader)) {
    if (event === 'token') {
      onToken(data)
    } else if (event === 'done') {
      onDone(data)
    } else if (event === 'error') {
      throw new Error(data)
    }
  }
}
