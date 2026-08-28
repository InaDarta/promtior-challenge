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

export function sendChatMessage(message) {
  return apiFetch('/chat', {
    method: 'POST',
    body: JSON.stringify({ message }),
  })
}
