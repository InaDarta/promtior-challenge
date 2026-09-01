import { createContext, useContext, useState } from 'react'
import { getStoredUsername, login as apiLogin, logout as apiLogout } from '../api/client'

const AuthContext = createContext(null)

/**
 * No hay endpoint `/api/me`: la sesión no se "resuelve" contra el backend, se conoce porque el
 * usuario acaba de loguearse (o porque ya había un usuario guardado en `sessionStorage` de una
 * navegación previa). Si un request autenticado responde 401, quien lo hace es responsable de
 * llamar a `logout` y redirigir a `/login`.
 */
export function AuthProvider({ children }) {
  const [username, setUsername] = useState(() => getStoredUsername())

  async function login(user, password) {
    await apiLogin(user, password)
    setUsername(user)
  }

  function logout() {
    apiLogout()
    setUsername(null)
  }

  const value = {
    user: username,
    isAuthenticated: username !== null,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider')
  }
  return context
}
